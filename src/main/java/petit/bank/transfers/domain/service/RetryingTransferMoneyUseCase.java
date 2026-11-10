package petit.bank.transfers.domain.service;

import petit.bank.accounts.domain.model.ConcurrentAccountUpdateException;
import petit.bank.ledger.domain.model.TransactionId;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;
import petit.bank.transfers.domain.port.in.TransferMoneyUseCase;

import java.util.Objects;

/**
 * Decorator do caso de uso de transferência que repete a operação quando ela perde um conflito
 * de concorrência. Cada nova tentativa relê as contas, então trabalha sobre o estado atual.
 * Só conflitos são repetidos: erros de negócio, como saldo insuficiente, sobem na primeira vez.
 * Esgotadas as tentativas, o conflito sobe para quem chamou.
 */
public class RetryingTransferMoneyUseCase implements TransferMoneyUseCase {
    private final TransferMoneyUseCase delegate;

    private final int maxAttempts;

    public RetryingTransferMoneyUseCase(TransferMoneyUseCase delegate, int maxAttempts) {
        this.delegate = Objects.requireNonNull(delegate, "delegate não pode ser nulo");

        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts deve ser pelo menos 1");
        }
        this.maxAttempts = maxAttempts;
    }

    @Override
    public TransactionId transfer(TransferMoneyCommand command) {
        for (int attempt = 1; ; attempt++) {
            try {
                return delegate.transfer(command);
            } catch (ConcurrentAccountUpdateException e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
            }
        }
    }
}
