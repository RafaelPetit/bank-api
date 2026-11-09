package petit.bank.accounts.infrastructure.adapter.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.model.ConcurrentAccountUpdateException;
import petit.bank.accounts.domain.port.out.AccountRepository;
import petit.bank.shared.Money;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter de persistência em memória para {@link AccountRepository}, usado em testes e
 * desenvolvimento enquanto não há banco. Imita a semântica de um banco: guarda apenas o
 * estado (saldo e versão) e reconstrói uma conta nova a cada leitura. O {@code save} confere
 * a versão e grava numa única operação atômica por conta, implementando o lock otimista.
 */
@ApplicationScoped
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<AccountId, StoredAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(accounts.get(id))
                .map(stored -> Account.restore(id, stored.balance(), stored.version()));
    }

    @Override
    public void save(Account account) {
        accounts.compute(account.id(), (id, stored) -> {
            long currentVersion = stored == null ? 0 : stored.version();
            if (currentVersion != account.version()) {
                throw new ConcurrentAccountUpdateException(id);
            }

            return new StoredAccount(account.balance(), currentVersion + 1);
        });
    }

    private record StoredAccount(Money balance, long version) {
    }
}
