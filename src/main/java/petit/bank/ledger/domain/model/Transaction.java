package petit.bank.ledger.domain.model;


import petit.bank.accounts.domain.model.AccountId;
import petit.bank.shared.Money;

import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Transação financeira do ledger e <b>aggregate root</b> do contexto: um evento atômico
 * composto por {@link LedgerEntry}s imutáveis que obrigatoriamente somam zero (double-entry).
 * É a fronteira de consistência da movimentação de dinheiro — só se cria por factory, já
 * balanceada e numa única moeda, de modo que uma transação inválida é impossível de existir.
 */
public final class Transaction {

    private final TransactionId id;
    private final List<LedgerEntry> entries;

    private Transaction(TransactionId id, List<LedgerEntry> entries) {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(entries, "entries não pode ser nulo");

        List<LedgerEntry> copy = List.copyOf(entries);

        if (copy.size() < 2) {
            throw new IllegalArgumentException("Uma transação precisa de ao menos 2 lançamentos");
        }

        requireSingleCurrency(copy);
        requireBalanced(copy);

        this.id = id;
        this.entries = copy;
    }

    public static Transaction of(List<LedgerEntry> entries) {
        return new Transaction(TransactionId.generate(), entries);
    }

    public static Transaction transfer(AccountId from, AccountId to, Money amount) {
        Objects.requireNonNull(from, "from não pode ser nulo");
        Objects.requireNonNull(to, "to não pode ser nulo");

        if (from.equals(to)) {
            throw new IllegalArgumentException("Origem e destino não podem ser a mesma conta");
        }

        return of(List.of(
                LedgerEntry.debit(from, amount),
                LedgerEntry.credit(to, amount)
        ));
    }

    private static void requireSingleCurrency(List<LedgerEntry> entries) {
        Currency currency = entries.getFirst().amount().currency();

        boolean allSame = entries.stream()
                .allMatch(e -> e.amount().currency().equals(currency));

        if (!allSame) {
            throw new IllegalArgumentException("Todos os lançamentos devem ter a mesma moeda");
        }
    }

    private static void requireBalanced(List<LedgerEntry> entries) {
        Currency currency = entries.getFirst().amount().currency();
        Money totalDebits = sumByDirection(entries, EntryDirection.DEBIT, currency);
        Money totalCredits = sumByDirection(entries, EntryDirection.CREDIT, currency);

        if (!totalDebits.equals(totalCredits)) {
            throw new UnbalancedTransactionException(totalDebits, totalCredits);
        }
    }


    private static Money sumByDirection(List<LedgerEntry> entries, EntryDirection direction, Currency currency) {
        return entries.stream()
                .filter(e -> e.direction() == direction)
                .map(LedgerEntry::amount)
                .reduce(Money.zero(currency), Money::add);
    }

    public TransactionId id() {
        return id;
    }

    public List<LedgerEntry> entries() {
        return entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
