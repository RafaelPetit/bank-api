package petit.bank.accounts.domain.port.out;

import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;

import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para persistir e recuperar o aggregate {@link Account}. O domínio declara
 * o contrato; a infraestrutura implementa (memória, Postgres...). {@code findById} devolve
 * vazio quando a conta não existe, e quem chama decide o que isso significa no negócio.
 * {@code saveAll} grava várias contas como uma unidade: confere se todas continuam na versão
 * em que foram lidas e, se alguma não estiver, lança
 * {@link petit.bank.accounts.domain.model.ConcurrentAccountUpdateException} sem gravar nenhuma.
 * {@code save} é o mesmo contrato para uma conta só.
 */
public interface AccountRepository {
    Optional<Account> findById(AccountId id);

    void saveAll(List<Account> accounts);

    default void save(Account account) {
        saveAll(List.of(account));
    }
}
