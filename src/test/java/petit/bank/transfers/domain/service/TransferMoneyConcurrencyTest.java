package petit.bank.transfers.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.model.ConcurrentAccountUpdateException;
import petit.bank.accounts.domain.port.out.AccountRepository;
import petit.bank.accounts.infrastructure.adapter.out.persistence.InMemoryAccountRepository;
import petit.bank.ledger.infrastructure.adapter.out.persistence.InMemoryLedgerRepository;
import petit.bank.shared.Money;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduz de forma determinística os conflitos de concorrência da transferência: duas
 * transferências que leem o mesmo saldo e uma operação que altera o destino entre a leitura e a gravação.
 */
public class TransferMoneyConcurrencyTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();

    @Test
    @DisplayName("as duas transferências simultâneas não podem gastar o mesmo saldo")
    void naoGastaOMesmoSaldoDuasVezes() {
        TransferMoneyService service = new TransferMoneyService(new SaveAfterBothRead(accounts), ledger);
        AccountId origem = contaComSaldo("100.00");
        AccountId destinoA = contaComSaldo("0.00");
        AccountId destinoB = contaComSaldo("0.00");

        try (ExecutorService threads = Executors.newFixedThreadPool(2)) {
            threads.submit(() -> tentarTransferir(service, origem, destinoA));
            threads.submit(() -> tentarTransferir(service, origem, destinoB));
        }

        Money totalRecebido = saldoDe(destinoA).add(saldoDe(destinoB));
        assertEquals(Money.of("60.00", BRL), totalRecebido);
        assertEquals(Money.of("40.00", BRL), saldoDe(origem));
        assertEquals(1, ledger.transactions().size());
    }

    @Test
    @DisplayName("conflito no destino não deixa a origem debitada")
    void conflitoNoDestinoNaoDebitaOrigem() {
        AccountId origem = contaComSaldo("100.00");
        AccountId destino = contaComSaldo("0.00");
        TransferMoneyService service = new TransferMoneyService(new ChangeBeforeFirstSave(accounts, destino), ledger);

        assertThrows(ConcurrentAccountUpdateException.class,
                () -> service.transfer(new TransferMoneyCommand(origem, destino,
                        Money.of("60.00", BRL))));

        assertEquals(Money.of("100.00", BRL), saldoDe(origem));
        assertEquals(Money.of("10.00", BRL), saldoDe(destino));
        assertTrue(ledger.transactions().isEmpty());
    }

    private void tentarTransferir(TransferMoneyService service, AccountId origem, AccountId destino) {
        try {
            service.transfer(new TransferMoneyCommand(origem, destino,
                    Money.of("60.00", BRL)));
        } catch (RuntimeException e) {
            // A transferência perdedora falha; quem confere o resultado são as asserções.
        }
    }

    private AccountId contaComSaldo(String valor) {
        Account conta = Account.open(AccountId.generate(), BRL);
        Money saldo = Money.of(valor, BRL);

        if (saldo.isPositive()) {
            conta.credit(saldo);
        }

        accounts.save(conta);

        return conta.id();
    }

    private Money saldoDe(AccountId id) {
        return accounts.findById(id).orElseThrow().balance();
    }

    /**
     * Segura a primeira gravação de cada thread até as duas terem chegado a esse ponto.
     */
    private static final class SaveAfterBothRead implements AccountRepository {

        private final AccountRepository delegate;
        private final CountDownLatch bothRead = new CountDownLatch(2);

        private SaveAfterBothRead(AccountRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<Account> findById(AccountId id) {
            return delegate.findById(id);
        }

        @Override
        public void saveAll(List<Account> accounts) {
            bothRead.countDown();

            try {
                bothRead.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }

            delegate.saveAll(accounts);
        }
    }

    /**
     * Simula outra operação que credita a conta indicada logo antes da primeira gravação.
     */
    private static final class ChangeBeforeFirstSave implements AccountRepository {

        private final AccountRepository delegate;
        private final AccountId changedAccount;
        private boolean changed;

        private ChangeBeforeFirstSave(AccountRepository delegate, AccountId changedAccount) {
            this.delegate = delegate;
            this.changedAccount = changedAccount;
        }

        @Override
        public Optional<Account> findById(AccountId id) {
            return delegate.findById(id);
        }

        @Override
        public void saveAll(List<Account> accountsToSave) {
            if (!changed) {
                changed = true;
                Account other = delegate.findById(changedAccount).orElseThrow();
                other.credit(Money.of("10.00", BRL));
                delegate.save(other);
            }

            delegate.saveAll(accountsToSave);
        }
    }
}
