package petit.bank.accounts.infrastructure.adapter.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.model.ConcurrentAccountUpdateException;
import petit.bank.accounts.domain.port.out.AccountRepository;
import petit.bank.shared.Money;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * Adapter de persistência para {@link AccountRepository} no PostgreSQL, com SQL explícito via
 * JDBC. O lock otimista é o próprio {@code UPDATE ... WHERE version = ?}: se outra transação
 * gravou antes, nenhuma linha muda e a gravação é recusada. Uma conta nova (versão 0) é inserida,
 * e um id que já existe também conta como conflito. {@code saveAll} roda numa transação, então
 * um conflito em qualquer conta desfaz todas. As contas são gravadas em ordem de id, para que
 * duas transferências em sentidos opostos travem as linhas na mesma ordem e não causem deadlock.
 */
@ApplicationScoped
public class PostgresAccountRepository implements AccountRepository {

    private static final String SELECT = "SELECT currency, balance, version FROM accounts WHERE id = ?";

    private static final String INSERT = "INSERT INTO accounts (id, currency, balance, version) VALUES (?, ?, ?, 1) " +
            "ON CONFLICT (id) DO NOTHING";

    private static final String UPDATE = "UPDATE accounts SET balance = ?, version = version + 1 WHERE id = ? AND version = ?";

    private final DataSource dataSource;

    public PostgresAccountRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT)) {
            statement.setObject(1, id.value());

            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return Optional.empty();
                }

                Currency currency = Currency.getInstance(row.getString("currency"));
                Money balance = Money.of(row.getBigDecimal("balance"), currency);

                return Optional.of(Account.restore(id, balance, row.getLong("version")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao ler a conta " + id, e);
        }
    }

    @Override
    @Transactional
    public void saveAll(List<Account> accounts) {
        List<Account> inIdOrder = accounts.stream()
                .sorted(Comparator.comparing(account -> account.id().value()))
                .toList();

        try (Connection connection = dataSource.getConnection()) {
            for (Account account : inIdOrder) {
                if (write(connection, account) != 1) {
                    throw new ConcurrentAccountUpdateException(account.id());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao gravar as contas", e);
        }
    }

    private int write(Connection connection, Account account) throws SQLException {
        return account.version() == 0 ? insert(connection, account) : update(connection, account);
    }

    private int insert(Connection connection, Account account) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setObject(1, account.id().value());
            statement.setString(2, account.balance().currency().getCurrencyCode());
            statement.setBigDecimal(3, account.balance().amount());

            return statement.executeUpdate();
        }
    }

    private int update(Connection connection, Account account) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setBigDecimal(1, account.balance().amount());
            statement.setObject(2, account.id().value());
            statement.setLong(3, account.version());

            return statement.executeUpdate();
        }
    }
}
