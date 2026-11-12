package petit.bank.transfers.domain.model;

/**
 * Lançada quando chega um pedido cuja idempotency key continua a ser processada.
 * O pedido original segue; quem repetiu pode consultar o resultado depois, reenviando com a mesma chave.
 */
public class TransferInProgressException extends RuntimeException {
    public TransferInProgressException(IdempotencyKey key) {
        super("Pedido ainda em processamento: " + key.value());
    }
}
