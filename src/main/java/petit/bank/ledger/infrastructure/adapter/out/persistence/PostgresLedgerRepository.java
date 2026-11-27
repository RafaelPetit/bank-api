package petit.bank.ledger.infrastructure.adapter.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import petit.bank.ledger.domain.model.LedgerEntry;
import petit.bank.ledger.domain.model.Transaction;
import petit.bank.ledger.domain.port.out.LedgerRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Adapter de persistência para {@link LedgerRepository} no PostgreSQL, com SQL explícito via
 * JDBC. Uma transação vira uma linha em {@code ledger_transactions} e uma linha por lançamento em
 * {@code ledger_entries}, gravadas juntas numa transação de banco. Registrar a mesma transação
 * duas vezes viola a chave primária e é recusado. O banco também rejeita qualquer UPDATE ou
 * DELETE nessas tabelas, então o append-only não depende só deste código.
 */
@ApplicationScoped
public class PostgresLedgerRepository implements LedgerRepository {

    private static final String INSERT_TRANSACTION =
            "INSERT INTO ledger_transactions (id) VALUES (?)";
    private static final String INSERT_ENTRY =
            "INSERT INTO ledger_entries (transaction_id, position, account_id, direction, amount, currency) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

    private final DataSource dataSource;

    public PostgresLedgerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void append(Transaction transaction) {
        try (Connection connection = dataSource.getConnection()) {
            insertTransaction(connection, transaction);
            insertEntries(connection, transaction);
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao registrar a transação " + transaction.id(), e);
        }
    }


    private void insertTransaction(Connection connection, Transaction transaction) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_TRANSACTION)) {
            statement.setObject(1, transaction.id().value());
            statement.executeUpdate();
        }
    }

    private void insertEntries(Connection connection, Transaction transaction) throws SQLException {
        List<LedgerEntry> entries = transaction.entries();

        try (PreparedStatement statement = connection.prepareStatement(INSERT_ENTRY)) {
            for (int position = 0; position < entries.size(); position++) {
                LedgerEntry entry = entries.get(position);

                statement.setObject(1, transaction.id().value());
                statement.setInt(2, position);
                statement.setObject(3, entry.account().value());
                statement.setString(4, entry.direction().name());
                statement.setBigDecimal(5, entry.amount().amount());
                statement.setString(6, entry.amount().currency().getCurrencyCode());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
