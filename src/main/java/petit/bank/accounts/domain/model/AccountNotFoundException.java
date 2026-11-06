package petit.bank.accounts.domain.model;

/**
 * Lançada quando uma operação referencia uma conta que não existe. Exceção de domínio
 * (unchecked). A mensagem inclui só o id, que não é dado sensível; nunca saldo ou titular.
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(AccountId id) {
        super("Conta não encontrada: " + id);
    }
}
