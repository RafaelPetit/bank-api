package petit.bank.transfers.infrastructure.adapter.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import petit.bank.ledger.domain.model.TransactionId;
import petit.bank.transfers.domain.model.IdempotencyKey;
import petit.bank.transfers.domain.model.IdempotencyRecord;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;
import petit.bank.transfers.domain.port.out.IdempotencyStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter em memória para {@link IdempotencyStore}, usado em testes e desenvolvimento.
 * A reserva usa {@code putIfAbsent}, que verifica e grava numa única operação atômica.
 * As chaves nunca expiram aqui; um armazenamento real precisaria de um prazo de retenção.
 */
@ApplicationScoped
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<IdempotencyKey, IdempotencyRecord> records = new ConcurrentHashMap<>();

    @Override
    public Optional<IdempotencyRecord> reserve(TransferMoneyCommand command) {
        IdempotencyRecord inProgress = new IdempotencyRecord.InProgress(command);

        return Optional.ofNullable(records.putIfAbsent(command.idempotencyKey(), inProgress));
    }

    @Override
    public void complete(IdempotencyKey key, TransactionId transactionId) {
        records.computeIfPresent(key,
                (k, record) -> new IdempotencyRecord.Completed(record.command(), transactionId));
    }

    @Override
    public void release(IdempotencyKey key) {
        records.remove(key);
    }
}
