package petit.bank.transfers.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.model.AccountNotFoundException;
import petit.bank.accounts.domain.model.InsufficientFundsException;
import petit.bank.accounts.infrastructure.adapter.out.persistence.InMemoryAccountRepository;
import petit.bank.ledger.domain.model.TransactionId;
import petit.bank.ledger.infrastructure.adapter.out.persistence.InMemoryLedgerRepository;
import petit.bank.shared.CurrencyMismatchException;
import petit.bank.shared.Money;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;

import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do caso de uso de transferência com os adapters em memória: movimentação dos saldos,
 * registro no ledger e a garantia de que nenhuma falha altera saldos ou registra transação.
 */
public class TransferMoneyServiceTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final TransferMoneyService service = new TransferMoneyService(accounts, ledger);

    @Test
    @DisplayName("transferir debita a origem e credita o destino")
    void transfereMoveSaldos() {
        AccountId origem = contaComSaldo("100.00", BRL);
        AccountId destino = contaComSaldo("20.00", BRL);

        service.transfer(new TransferMoneyCommand(origem, destino,
                Money.of("30.00", BRL)));

        assertEquals(Money.of("70.00", BRL), saldoDe(origem));
        assertEquals(Money.of("50.00", BRL), saldoDe(destino));
    }

    @Test
    @DisplayName("transferir registra a transação no ledger e devolve o id dela")
    void registraNoLedger() {
        AccountId origem = contaComSaldo("100.00", BRL);
        AccountId destino = contaComSaldo("0.00", BRL);

        TransactionId id = service.transfer(new TransferMoneyCommand(origem, destino,
                Money.of("30.00", BRL)));

        assertEquals(1, ledger.transactions().size());
        assertEquals(id, ledger.transactions().getFirst().id());
    }

    @Test
    @DisplayName("saldo insuficiente não altera nenhuma conta nem registra transação")
    void saldoInsuficienteNaoAlteraNada() {
        AccountId origem = contaComSaldo("10.00", BRL);
        AccountId destino = contaComSaldo("0.00", BRL);

        assertThrows(InsufficientFundsException.class,
                () -> service.transfer(new TransferMoneyCommand(origem, destino,
                        Money.of("50.00", BRL))));

        assertEquals(Money.of("10.00", BRL), saldoDe(origem));
        assertEquals(Money.of("0.00", BRL), saldoDe(destino));
        assertTrue(ledger.transactions().isEmpty());
    }

    @Test
    @DisplayName("falha no crédito não deixa a origem debitada")
    void falhaNoCreditoNaoDebitaOrigem() {
        AccountId origem = contaComSaldo("100.00", BRL);
        AccountId destino = contaComSaldo("0.00", USD);

        assertThrows(CurrencyMismatchException.class,
                () -> service.transfer(new TransferMoneyCommand(origem, destino,
                        Money.of("10.00", BRL))));

        assertEquals(Money.of("100.00", BRL), saldoDe(origem));
        assertTrue(ledger.transactions().isEmpty());
    }

    @Test
    @DisplayName("conta inexistente é rejeitada sem alterar a origem")
    void contaInexistente() {
        AccountId origem = contaComSaldo("100.00", BRL);
        AccountId inexistente = AccountId.generate();

        assertThrows(AccountNotFoundException.class,
                () -> service.transfer(new TransferMoneyCommand(origem, inexistente,
                        Money.of("10.00", BRL))));

        assertEquals(Money.of("100.00", BRL), saldoDe(origem));
        assertTrue(ledger.transactions().isEmpty());
    }

    @Test
    @DisplayName("transferir para a própria conta é rejeitado sem alterar o saldo")
    void mesmaConta() {
        AccountId conta = contaComSaldo("100.00", BRL);

        assertThrows(IllegalArgumentException.class,
                () -> service.transfer(new TransferMoneyCommand(conta, conta,
                        Money.of("10.00", BRL))));

        assertEquals(Money.of("100.00", BRL), saldoDe(conta));
    }

    private AccountId contaComSaldo(String valor, Currency moeda) {
        Account conta = Account.open(AccountId.generate(), moeda);
        Money saldo = Money.of(valor, moeda);
        if (saldo.isPositive()) {
            conta.credit(saldo);
        }

        accounts.save(conta);

        return conta.id();
    }

    private Money saldoDe(AccountId id) {
        return accounts.findById(id).orElseThrow().balance();
    }
}