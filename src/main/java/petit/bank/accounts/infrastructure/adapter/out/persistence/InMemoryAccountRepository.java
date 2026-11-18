package petit.bank.accounts.infrastructure.adapter.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.model.ConcurrentAccountUpdateException;
import petit.bank.accounts.domain.port.out.AccountRepository;
import petit.bank.shared.Money;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter de persistência em memória para {@link AccountRepository}, usado em testes e
 * desenvolvimento enquanto não há banco. Imita a semântica de um banco: guarda apenas o
 * estado (saldo e versão) e reconstrói uma conta nova a cada leitura. O {@code saveAll}
 * confere as versões de todas as contas antes de gravar qualquer uma, sob um único lock,
 * fazendo o papel de uma transação.
 */
@ApplicationScoped
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<AccountId, StoredAccount> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(storage.get(id))
                .map(stored -> Account.restore(id, stored.balance(), stored.version()));
    }

    @Override
    public synchronized void saveAll(List<Account> accounts) {
        for (Account account : accounts) {
            if (currentVersion(account.id()) != account.version()) {
                throw new ConcurrentAccountUpdateException(account.id());
            }
        }

        for (Account account : accounts) {
            storage.put(account.id(), new StoredAccount(account.balance(), account.version() +1));
        }

    }

    private long currentVersion(AccountId accountId) {
        StoredAccount storedAccount = storage.get(accountId);
        return storedAccount == null ? 0 : storedAccount.version();
    }

    private record StoredAccount(Money balance, long version) {
    }
}
