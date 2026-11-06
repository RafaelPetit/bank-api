package petit.bank.accounts.domain.model;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() {
        super("Saldo insuficiente para o débito solicitado");
    }
}
