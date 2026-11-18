package petit.bank.transfers.domain.model;

/**
 * Lançada quando uma idempotency key já usada chega com um pedido diferente do original.
 * É um erro de quem chama: a chave identifica um único pedido.
 */
public class IdempotencyKeyReusedException extends RuntimeException {
    public IdempotencyKeyReusedException(IdempotencyKey key) {
        super("Idempotency key já usada em outro pedido: " + key.value());
    }
}
