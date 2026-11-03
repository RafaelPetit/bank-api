package petit.bank.ledger.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Identidade de uma transação no ledger (Value Object imutável). Envelopa um
 * {@link java.util.UUID} para dar tipagem forte, impedindo misturá-lo com outros
 * identificadores e tornando as assinaturas autoexplicativas.
 */
public record TransactionId(UUID value) {

    public TransactionId {
        Objects.requireNonNull(value, "TransactionId não pode ser nulo");
    }

    public static TransactionId generate () {
        return new TransactionId(UUID.randomUUID());
    }

    public static TransactionId of (UUID value) {
        return new TransactionId(value);
    }

    public static TransactionId of (String value) {
        return new TransactionId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }

}
