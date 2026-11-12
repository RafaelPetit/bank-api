package petit.bank.transfers.domain.port.out;

import petit.bank.ledger.domain.model.TransactionId;
import petit.bank.transfers.domain.model.IdempotencyKey;
import petit.bank.transfers.domain.model.IdempotencyRecord;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;

import java.util.Optional;

/**
 * Porta de saída que registra os pedidos de transferência já recebidos, pela idempotency key.
 * {@code reserve} é atômico: se a chave é nova, reserva-a como "em processamento" e
 * devolve vazio; se já existe, não altera nada e devolve o registro existente.
 * Duas chamadas simultâneas com a mesma chave nunca reservam as duas. {@code complete} marca o pedido como
 * concluído com a transação gerada; {@code release} libera a chave de um pedido que falhou.
 */
public interface IdempotencyStore {

    Optional<IdempotencyRecord> reserve(TransferMoneyCommand command);

    void complete(IdempotencyKey key, TransactionId transactionId);

    void release(IdempotencyKey key);
}
