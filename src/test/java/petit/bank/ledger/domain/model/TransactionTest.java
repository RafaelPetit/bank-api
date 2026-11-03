package petit.bank.ledger.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.shared.Money;

import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do aggregate root {@link Transaction}: criação balanceada via {@code transfer},
 * proteção das invariantes (soma-zero, moeda única, mínimo de 2 lançamentos), imutabilidade dos
 * lançamentos e identidade por id. Domínio puro — não sobe o Quarkus.
 */
public class TransactionTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private final AccountId origem = AccountId.generate();
    private final AccountId destino = AccountId.generate();

    @Test
    @DisplayName("transfer nasce balanceada: debita a origem e credita o destino pelo mesmo valor")
    void transferNasceBalanceada() {

        Transaction tx = Transaction.transfer(origem, destino, Money.of("10.00", BRL));

        assertEquals(2, tx.entries().size());

        LedgerEntry debito = tx.entries().getFirst();
        LedgerEntry credito = tx.entries().get(1);

        assertEquals(origem, debito.account());
        assertEquals(EntryDirection.DEBIT, debito.direction());
        assertEquals(destino, credito.account());
        assertEquals(EntryDirection.CREDIT, credito.direction());
        assertEquals(Money.of("10.00", BRL), debito.amount());
        assertEquals(Money.of("10.00", BRL), credito.amount());
    }

    @Test
    @DisplayName("transferir para a mesma conta é proibido")
    void mesmaContaFalha() {
        assertThrows(IllegalArgumentException.class,
                () -> Transaction.transfer(origem, origem, Money.of("10.00", BRL)));
    }

    @Test
    @DisplayName("os lançamentos de uma transação são imutáveis")
    void entriesSaoImutaveis() {

        Transaction tx = Transaction.transfer(origem, destino, Money.of("10.00", BRL));
        LedgerEntry novo = LedgerEntry.debit(origem, Money.of("10.00", BRL));

        assertThrows(UnsupportedOperationException.class, () -> tx.entries().add(novo));
    }

    @Test
    @DisplayName("transação desbalanceada (débitos ≠ créditos) é rejeitada")
    void desbalanceadaFalha() {
        List<LedgerEntry> pernas = List.of(
                LedgerEntry.debit(origem, Money.of("10.00", BRL)),
                LedgerEntry.credit(destino, Money.of("5.00", BRL))
        );

        assertThrows(UnbalancedTransactionException.class, () -> Transaction.of(pernas));

    }

    @Test
    @DisplayName("transação com menos de 2 lançamentos é rejeitada")
    void minimoDoisLançamentos() {
        List<LedgerEntry> umaPerna = List.of(
                LedgerEntry.debit(origem, Money.of("10.00", BRL))
        );
        assertThrows(IllegalArgumentException.class, () -> Transaction.of(umaPerna));
    }

    @Test
    @DisplayName("lançamentos de moedas diferentes na mesma transaçãi são rejeitados")
    void moedaUnica() {
        Currency USD = Currency.getInstance("USD");

        List<LedgerEntry> pernas = List.of(
                LedgerEntry.debit(origem, Money.of("10.00", BRL)),
                LedgerEntry.credit(destino, Money.of("10.00", USD))
        );

        assertThrows(IllegalArgumentException.class, () -> Transaction.of(pernas));
    }

    @Test
    @DisplayName("transações tem identidade própria: comparadas por id, não por conteudo")
    void identidadePorId() {
        Transaction t1 = Transaction.transfer(origem, destino, Money.of("10.00", BRL));
        Transaction t2 = Transaction.transfer(origem, destino, Money.of("10.00", BRL));

        assertEquals(t1, t1);
        assertNotEquals(t1, t2);
    }
}
