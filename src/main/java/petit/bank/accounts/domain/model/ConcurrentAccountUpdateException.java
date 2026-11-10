package petit.bank.accounts.domain.model;

/**
 * Lançada quando uma conta é salva a partir de uma versão desatualizada: outra operação a
 * alterou depois que ela foi lida. É o conflito do lock otimista; a operação pode ser repetida
 * a partir de uma nova leitura.
 */
public class ConcurrentAccountUpdateException extends RuntimeException {
    public ConcurrentAccountUpdateException(AccountId id) {
        super("Conta alterada por outra operação: " + id);
    }
}
