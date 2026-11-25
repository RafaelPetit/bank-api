package petit.bank.accounts.infrastructure.adapter.out.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import petit.bank.accounts.domain.port.out.AccountRepository;

@QuarkusTest
class PostgresAccountRepositoryTest extends AccountRepositoryContractTest {

    @Inject
    PostgresAccountRepository accounts;

    @Override
    protected AccountRepository accounts() {
        return accounts;
    }
}
