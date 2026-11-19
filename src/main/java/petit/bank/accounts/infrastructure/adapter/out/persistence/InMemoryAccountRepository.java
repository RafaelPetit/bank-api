package petit.bank.accounts.infrastructure.adapter.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.port.out.AccountRepository;
import petit.bank.shared.Money;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter de persistência em memória para {@link AccountRepository}, usado em testes e
 * desenvolvimento enquanto não há banco. Imita a semântica de um banco: guarda apenas o
 * estado (o saldo) e reconstrói uma conta nova a cada leitura, de modo que alterar uma conta
 * lida não afeta o que está guardado até que ela seja salva.
 */
@ApplicationScoped
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<AccountId, Money> balances = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(balances.get(id))
                .map(balance -> Account.restore(id, balance));
    }

    @Override
    public void save(Account account) {
        balances.put(account.id(), account.balance());
    }
}
