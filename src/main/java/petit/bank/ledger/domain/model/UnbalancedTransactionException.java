package petit.bank.ledger.domain.model;

import petit.bank.shared.Money;

/**
 * Lançada quando os lançamentos de uma {@link Transaction} não somam zero (débitos ≠
 * créditos) — a violação do invariante central do double-entry, que significaria dinheiro
 * criado ou destruído.
 */
public class UnbalancedTransactionException extends RuntimeException {

    public UnbalancedTransactionException(Money totalDebits, Money totalCredits) {
        super("Transação não balanceia: débitos=%s, créditos=%s"
                .formatted(totalDebits, totalCredits));
    }
}
