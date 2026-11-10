package petit.bank.accounts.domain.model;

import petit.bank.shared.Money;

import java.util.Currency;
import java.util.Objects;

/**
 * Conta bancária: Entity e aggregate root do contexto de contas. Guarda um saldo (projeção
 * dos movimentos) e protege os invariantes do dinheiro — saldo nunca negativo e operações
 * sempre na moeda da conta. É mutável: a mesma conta evolui no tempo mantendo a identidade.
 * A versão indica a partir de qual gravação a conta foi lida e serve ao controle de
 * concorrência otimista; o domínio só a carrega, quem a confere é a persistência.
 */
public final class Account {
    private final AccountId id;
    private final long version;
    private Money balance;

    private Account(AccountId id, Money balance, long version) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.balance = Objects.requireNonNull(balance, "balance não pode ser nulo");

        if (balance.isNegative()) {
            throw new IllegalArgumentException("Saldo de uma conta não pode ser negativo");
        }

        if (version < 0) {
            throw new IllegalArgumentException("Versão de uma conta não pode ser negativa");
        }
        this.version = version;
    }

    public static Account open(AccountId id, Currency currency) {
        Objects.requireNonNull(currency, "currency não pode ser nula");
        return new Account(id, Money.zero(currency), 0);
    }

    public static Account restore(AccountId id, Money balance, long version) {
        return new Account(id, balance, version);
    }

    public void credit(Money amount) {
        requirePositive(amount);
        this.balance = this.balance.add(amount);
    }

    public void debit(Money amount) {
        requirePositive(amount);
        if (amount.isGreaterThan(balance)) {
            throw new InsufficientFundsException();
        }
        this.balance = this.balance.subtract(amount);
    }

    private static void requirePositive(Money amount) {
        Objects.requireNonNull(amount, "amount não pode ser nulo");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("valor da operação deve ser positivo: " + amount);
        }
    }

    public AccountId id() {
        return id;
    }

    public Money balance() {
        return balance;
    }

    public long version() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
