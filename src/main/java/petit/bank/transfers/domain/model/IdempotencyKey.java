package petit.bank.transfers.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Identidade de um pedido de transferência, criada por quem origina a intenção.
 * Repetir o pedido com a mesma chave significa "é o mesmo pedido", não uma nova transferência.
 * O tamanho é limitado porque as chaves ficam guardadas.
 */
public record IdempotencyKey(String value) {
    private static final int MAX_LENGTH = 100;

    public IdempotencyKey {
        Objects.requireNonNull(value, "idempotency key não pode ser nula");

        if (value.isBlank() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Idempotency key deve ter entre 1 e 100 caracteres");
        }
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }

    public static IdempotencyKey generate() {
        return new IdempotencyKey(UUID.randomUUID().toString());
    }

}
