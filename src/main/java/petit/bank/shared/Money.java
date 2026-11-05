package petit.bank.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Valor monetário imutável (Value Object): quantia + moeda.
 *
 * <p>A quantia é normalizada para a escala da moeda (ex.: 2 casas em BRL), de modo
 * que {@code 1.0} e {@code 1.00} sejam iguais — do contrário o {@code equals} do
 * {@link java.math.BigDecimal}, sensível à escala, quebraria a igualdade por valor.</p>
 *
 * <p>Operações aritméticas exigem a mesma moeda; caso contrário lançam
 * {@link CurrencyMismatchException}.</p>
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount não pode ser nulo");
        Objects.requireNonNull(currency, "currency não pode ser nula");

        int fractionDigits = Math.max(currency.getDefaultFractionDigits(), 0);
        amount = amount.setScale(fractionDigits, RoundingMode.HALF_EVEN);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money ofExact(String amount, Currency currency) {
        Objects.requireNonNull(currency, "currency não pode ser nula");

        int fractionDigits = Math.max(currency.getDefaultFractionDigits(), 0);

        try {
            return new Money(new BigDecimal(amount).setScale(fractionDigits, RoundingMode.UNNECESSARY), currency);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Valor com mais casas decimais do que a moeda permite", e);
        }

    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);

        return amount.compareTo(other.amount) > 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other não pode ser nulo");

        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }

}
