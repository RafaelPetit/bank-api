package petit.bank.accounts.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Identidade de uma conta (Value Object imutável). Envelopa um {@link java.util.UUID}
 * para dar tipagem forte: impede trocar um {@code AccountId} por qualquer outro
 * identificador (evita <em>primitive obsession</em>) e deixa as assinaturas
 * autoexplicativas.
 */
public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value, "AccountId não pode ser nulo");
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId of (UUID value) {
        return new AccountId(value);
    }

    public static AccountId of(String value) {
        return new AccountId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }

}
