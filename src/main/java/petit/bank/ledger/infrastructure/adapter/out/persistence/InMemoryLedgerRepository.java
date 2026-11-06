package petit.bank.ledger.infrastructure.adapter.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import petit.bank.ledger.domain.model.Transaction;
import petit.bank.ledger.domain.port.out.LedgerRepository;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adapter de persistência em memória para {@link LedgerRepository}, usado em testes e
 * desenvolvimento. Guarda as transações numa lista apenas acrescentada (append-only).
 * {@code transactions()} existe só para inspeção em testes e não faz parte da porta.
 */
@ApplicationScoped
public class InMemoryLedgerRepository implements LedgerRepository {

    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();

    @Override
    public void append(Transaction transaction) {
        transactions.add(Objects.requireNonNull(transaction, "transaction não pode ser nula"));
    }

    public List<Transaction> transactions() {
        return List.copyOf(transactions);
    }
}
