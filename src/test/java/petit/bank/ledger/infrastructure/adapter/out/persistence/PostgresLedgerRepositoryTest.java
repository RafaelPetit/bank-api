package petit.bank.ledger.infrastructure.adapter.out.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.ledger.domain.model.EntryDirection;
import petit.bank.ledger.domain.model.LedgerEntry;
import petit.bank.ledger.domain.model.Transaction;
import petit.bank.ledger.domain.model.TransactionId;
import petit.bank.shared.Money;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class PostgresLedgerRepositoryTest {
    private static final Currency BRL = Currency.getInstance("BRL");

    @Inject
    PostgresLedgerRepository ledger;

    @Inject
    DataSource dataSource;

    @Test
    @DisplayName("registra os lançamentos da transação, na ordem")
    void registraLancamentos() throws SQLException {
        Transaction transacao = transferencia();

        ledger.append(transacao);

        assertEquals(transacao.entries(), lancamentosDe(transacao.id()));
    }

    @Test
    @DisplayName("a mesma transação não é registrada duas vezes")
    void transacaoDuplicadaRecusada() throws SQLException {
        Transaction transacao = transferencia();
        ledger.append(transacao);

        assertThrows(IllegalStateException.class, () -> ledger.append(transacao));
        assertEquals(2, lancamentosDe(transacao.id()).size());
    }

    @Test
    @DisplayName("o banco recusa alterar ou apagar um lançamento registrado")
    void lancamentoNaoPodeSerAlteradoNemApagado() throws SQLException {
        Transaction transacao = transferencia();
        ledger.append(transacao);

        assertThrows(SQLException.class, () -> executar(
                "UPDATE ledger_entries SET amount = 1 WHERE transaction_id = ?", transacao.id()));
        assertThrows(SQLException.class, () -> executar(
                "DELETE FROM ledger_entries WHERE transaction_id = ?", transacao.id()));
        assertEquals(transacao.entries(), lancamentosDe(transacao.id()));
    }

    private Transaction transferencia() {
        return Transaction.transfer(AccountId.generate(), AccountId.generate(), Money.of("10.00", BRL));
    }

    private List<LedgerEntry> lancamentosDe(TransactionId id) throws SQLException {
        String sql = "SELECT account_id, direction, amount, currency FROM ledger_entries "
                + "WHERE transaction_id = ? ORDER BY position";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id.value());

            try (ResultSet row = statement.executeQuery()) {
                List<LedgerEntry> lancamentos = new ArrayList<>();
                while (row.next()) {
                    lancamentos.add(new LedgerEntry(
                            AccountId.of(row.getObject("account_id", UUID.class)),
                            EntryDirection.valueOf(row.getString("direction")),
                            Money.of(row.getBigDecimal("amount"), Currency.getInstance(row.getString("currency")))));
                }
                return lancamentos;
            }
        }
    }

    private void executar(String sql, TransactionId id) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id.value());
            statement.executeUpdate();
        }
    }
}
