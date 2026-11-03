package petit.bank.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do Value Object {@link Money}: normalização de escala (igualdade por
 * valor), imutabilidade das operações, invariante de moeda única e os factories.
 * Domínio puro — não sobe o Quarkus.
 */
class MoneyTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    @DisplayName("normaliza a escala: 1.0 e 1.00 da mesma moeda são iguais")
    void normalizeEscalaNoEquals() {
        Money a = Money.of("1.0", BRL);
        Money b = Money.of("1.00", BRL);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("of(String) preserva o valor exato, sem erro de ponto flutuante")
    void ofStringEhExato() {
        Money valor = Money.of("10.50", BRL);
        assertEquals(new BigDecimal("10.50"), valor.amount());
    }

    @Test
    @DisplayName("Soma dois valores da mesma moeda")
    void soma() {
        Money resultado = Money.of("10.00", BRL).add(Money.of("5.50", BRL));
        assertEquals(Money.of("15.50", BRL), resultado);
    }

    @Test
    @DisplayName("subtrai dois valores da mesma moeda")
    void subtrai() {
        Money resultado = Money.of("10.00", BRL).subtract(Money.of("3.00", BRL));
        assertEquals(Money.of("7.00", BRL), resultado);
    }

    @Test
    @DisplayName("add é imutavel: não altera a instancia original")
    void addEhImutavel() {
        Money dez = Money.of("10.00", BRL);
        dez.add(Money.of("5.00", BRL));
        assertEquals(Money.of("10.00", BRL), dez);
    }

    @Test
    @DisplayName("operar entre moedas diferentes estoura CurrencyMismatchException")
    void moedasDiferentesFalham() {
        Money real = Money.of("10.00", BRL);
        Money dolar = Money.of("10.00", USD);
        assertThrows(CurrencyMismatchException.class, () -> real.add(dolar));
    }

    @Test
    @DisplayName("amount nula não cria Money")
    void amountNuloFalha() {
        assertThrows(NullPointerException.class, () -> Money.of((BigDecimal) null, BRL));
    }

    @Test
    @DisplayName("zero cria um Money de valor nulo na moeda")
    void zero() {
        assertTrue(Money.zero(BRL).isZero());
    }

    @Test
    @DisplayName("sinais: positivo, negativo e zero")
    void sinais() {
        assertTrue(Money.of("0.01", BRL).isPositive());
        assertTrue(Money.of("-0.01", BRL).isNegative());
        assertTrue(Money.of("0.00", BRL).isZero());
    }

    @Test
    @DisplayName("isGreaterThab compara grandeza corretamente")
    void comparaGrandeza() {
        assertTrue(Money.of("10.00", BRL).isGreaterThan(Money.of("9.99", BRL)));
        assertFalse(Money.of("9.99", BRL).isGreaterThan(Money.of("10.00", BRL)));
    }
}
