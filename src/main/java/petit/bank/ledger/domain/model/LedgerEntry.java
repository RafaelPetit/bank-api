package petit.bank.ledger.domain.model;

import petit.bank.accounts.domain.model.AccountId;
import petit.bank.shared.Money;

import java.util.List;
import java.util.Objects;

/**
 * Lançamento imutável do ledger — uma "perna" de uma {@link Transaction}: a aplicação de
 * um valor positivo a uma conta, numa direção (débito ou crédito). Nunca existe solto;
 * só faz sentido numa transação que balanceia em zero.
 */
public record LedgerEntry(AccountId account, EntryDirection direction, Money amount) {

    public LedgerEntry {
        Objects.requireNonNull(account, "account não pode ser nulo");
        Objects.requireNonNull(direction, "direction não pode ser nula");
        Objects.requireNonNull(amount, "amount não pode ser nulo");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Valor de lançamento deve ser positivo: " + amount);
        }
    }

    public static LedgerEntry debit (AccountId account, Money amount) {
        return new LedgerEntry(account, EntryDirection.DEBIT, amount);
    }

    public static LedgerEntry credit (AccountId account, Money amount) {
        return new LedgerEntry(account, EntryDirection.CREDIT, amount);
    }
}