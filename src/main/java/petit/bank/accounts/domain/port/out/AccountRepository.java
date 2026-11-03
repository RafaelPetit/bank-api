package petit.bank.accounts.domain.port.out;

import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;

import java.util.Optional;

/**
 * Porta de saída para persistir e recuperar o aggregate {@link Account}. O domínio declara
 * o contrato; a infraestrutura implementa (memória, Postgres...). {@code findById} devolve
 * vazio quando a conta não existe, e quem chama decide o que isso significa no negócio.
 * {@code save} serve tanto para conta nova quanto para conta alterada.
 */
public interface AccountRepository {
    Optional<Account> findById(AccountId id);

    void save(Account account);
}
