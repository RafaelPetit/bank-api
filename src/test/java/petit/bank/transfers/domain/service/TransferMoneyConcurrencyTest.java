package petit.bank.transfers.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.port.out.AccountRepository;
import petit.bank.accounts.infrastructure.adapter.out.persistence.InMemoryAccountRepository;
import petit.bank.ledger.infrastructure.adapter.out.persistence.InMemoryLedgerRepository;
import petit.bank.shared.Money;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;

import java.util.Currency;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduz de forma determinística a corrida entre duas transferências da mesma conta: as duas
 * só tentam salvar após ambas terem lido o saldo, forçando o pior caso de concorrência.
 */
public class TransferMoneyConcurrencyTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final TransferMoneyService service = new TransferMoneyService(new SaveAfterBothRead(accounts), ledger);


    @Test
    @DisplayName("duas transferências simultâneas não podem gastar o mesmo saldo")
    void naoGastaOMesmoSaldoDuasVezes() {
        AccountId origem = contaComSaldo("100.00");
        AccountId destinoA = contaComSaldo("0.00");
        AccountId destinoB = contaComSaldo("0.00");

        try (ExecutorService threads = Executors.newFixedThreadPool(2)) {
            threads.submit(() -> tentarTransferir(origem, destinoA));
            threads.submit(() -> tentarTransferir(origem, destinoB));
        }

        Money totalRecebido = saldoDe(destinoA).add(saldoDe(destinoB));

        assertEquals(Money.of("60.00", BRL), totalRecebido);
        assertEquals(Money.of("40.00", BRL), saldoDe(origem));
        assertEquals(1, ledger.transactions().size());
    }

    private void tentarTransferir(AccountId origem, AccountId destino) {
        try {
            service.transfer(new TransferMoneyCommand(origem, destino, Money.of("60.00", BRL)));
        } catch (RuntimeException e) {
            // A transferência perdedora falha; quem confere o resultado são as asserções
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
     * Segura o primeiro save de cada thread até as duas terem chegado a esse ponto.
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
        public void save(Account account) {
            bothRead.countDown();

            try {
                bothRead.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }

            delegate.save(account);
        }
    }
}
