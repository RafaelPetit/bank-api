package petit.bank.accounts.infrastructure.adapter.out.persistence;

import petit.bank.accounts.domain.port.out.AccountRepository;

class InMemoryAccountRepositoryTest extends AccountRepositoryContractTest {

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();

    @Override
    protected AccountRepository accounts() {
        return accounts;
    }
}
