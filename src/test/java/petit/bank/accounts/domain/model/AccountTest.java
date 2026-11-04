package petit.bank.accounts.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import petit.bank.shared.CurrencyMismatchException;
import petit.bank.shared.Money;

import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do aggregate root {@link Account}: abertura com saldo zero, crédito/débito,
 * proteção do saldo (sem cheque especial) e da moeda única. Domínio puro — não sobe o Quarkus.
 */
public class AccountTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    @DisplayName("abre a conta com saldo zero na moeda informada")
    void abreComSaldoZero() {
        Account conta = Account.open(AccountId.generate(), BRL);

        assertEquals(Money.zero(BRL), conta.balance());
    }

    @Test
    @DisplayName("credita aumenta o saldo")
    void credita() {
        Account conta = Account.open(AccountId.generate(), BRL);
        conta.credit(Money.of("100.00", BRL));

        assertEquals(Money.of("100.00", BRL), conta.balance());
    }

    @Test
    @DisplayName("devita reduz o saldo")
    void debita() {
        Account conta = Account.open(AccountId.generate(), BRL);
        conta.credit(Money.of("100.00", BRL));
        conta.debit(Money.of("30.00", BRL));

        assertEquals(Money.of("70.00", BRL), conta.balance());
    }

    @Test
    @DisplayName("debitar o saldo inteiro é permitido (chega a zero)")
    void debitaTudo() {
        Account conta = Account.open(AccountId.generate(), BRL);
        conta.credit(Money.of("50.00", BRL));
        conta.debit(Money.of("50.00", BRL));

        assertTrue(conta.balance().isZero());
    }

    @Test
    @DisplayName("debitar além do saldo é rejeitado e não altera o saldo")
    void saldoInsuficiente() {
        Account conta = Account.open(AccountId.generate(), BRL);
        conta.credit(Money.of("20.00", BRL));

        assertThrows(InsufficientFundsException.class,
                () -> conta.debit(Money.of("20.01", BRL)));

        assertEquals(Money.of("20.00", BRL), conta.balance());
    }

    @Test
    @DisplayName("creditar em moeda diferente da conta é rejeitado")
    void moedaDiferente() {
        Account conta = Account.open(AccountId.generate(), BRL);

        assertThrows(CurrencyMismatchException.class,
                () -> conta.credit(Money.of("10.00", USD)));
    }

    @Test
    @DisplayName("valor não possitivo é rejeitado")
    void valorNaoPositivo() {
        Account conta = Account.open(AccountId.generate(), BRL);

        assertThrows(IllegalArgumentException.class,
                () -> conta.credit(Money.of("0.00", BRL)));
    }

    @Test
    @DisplayName("contas têm identidade própria: comparadas por id")
    void identidadePorId() {
        AccountId id = AccountId.generate();
        Account a = Account.open(id, BRL);
        Account b = Account.open(id, BRL);
        Account outra = Account.open(AccountId.generate(), BRL);

        assertEquals(a, b);
        assertNotEquals(a, outra);
    }

    @Test
    @DisplayName("restore reconstrói a conta com o saldo informado")
    void restoreReconstriComSaldo() {
        AccountId id = AccountId.generate();
        Account conta = Account.restore(id, Money.of("150.00", BRL));

        assertEquals(id, conta.id());
        assertEquals(Money.of("150.00", BRL), conta.balance());
    }

    @Test
    @DisplayName("restore rejeita saldo negativo")
    void restoreRejeitaSaldoNegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> Account.restore(AccountId.generate(), Money.of("-0.01", BRL)));
    }
}
