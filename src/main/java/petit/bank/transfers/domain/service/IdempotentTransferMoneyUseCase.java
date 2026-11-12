package petit.bank.transfers.domain.service;

import petit.bank.ledger.domain.model.TransactionId;
import petit.bank.transfers.domain.model.IdempotencyKeyReusedException;
import petit.bank.transfers.domain.model.IdempotencyRecord;
import petit.bank.transfers.domain.model.TransferInProgressException;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;
import petit.bank.transfers.domain.port.in.TransferMoneyUseCase;
import petit.bank.transfers.domain.port.out.IdempotencyStore;

import java.util.Objects;
import java.util.Optional;

/**
 * Decorator que torna a transferência idempotente pela idempotency key.
 * Um pedido novo é executado e tem o resultado registrado; repetir a mesma chave devolve a mesma transação sem
 * mover dinheiro de novo. A mesma chave com outro pedido é recusada, e uma chave ainda em
 * processamento também. Se a execução falhar, a chave é liberada para permitir nova tentativa.
 */
public class IdempotentTransferMoneyUseCase implements TransferMoneyUseCase {

    private final TransferMoneyUseCase delegate;
    private final IdempotencyStore store;

    public IdempotentTransferMoneyUseCase(TransferMoneyUseCase delegate, IdempotencyStore store) {
        this.delegate = Objects.requireNonNull(delegate, "delegate não pode ser nulo");
        this.store = Objects.requireNonNull(store, "store não pode ser nulo");
    }

    @Override
    public TransactionId transfer(TransferMoneyCommand command) {
        Optional<IdempotencyRecord> existing = store.reserve(command);

        if(existing.isPresent()) {
            return replay(command, existing.get());
        }

        try {
            TransactionId transactionId = delegate.transfer(command);
            store.complete(command.idempotencyKey(), transactionId);

            return transactionId;
        } catch (RuntimeException e) {
            store.release(command.idempotencyKey());
            throw e;
        }
    }

    private TransactionId replay(TransferMoneyCommand command, IdempotencyRecord existing) {
        if (!existing.command().equals(command)) {
            throw new IdempotencyKeyReusedException(command.idempotencyKey());
        }
        return switch (existing) {
            case IdempotencyRecord.Completed completed -> completed.transactionId();
            case IdempotencyRecord.InProgress inProgress ->
                    throw new TransferInProgressException(command.idempotencyKey());
        };
    }
}
