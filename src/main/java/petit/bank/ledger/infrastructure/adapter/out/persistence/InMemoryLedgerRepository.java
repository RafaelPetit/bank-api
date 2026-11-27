package petit.bank.ledger.infrastructure.adapter.out.persistence;

import petit.bank.ledger.domain.model.Transaction;
import petit.bank.ledger.domain.port.out.LedgerRepository;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adapter de persistência em memória para {@link LedgerRepository}, usado nos testes unitários,
 * que não sobem o banco. Não é um bean: a aplicação usa {@link PostgresLedgerRepository}. Guarda
 * as transações numa lista apenas acrescentada (append-only).
 * {@code transactions()} existe só para inspeção em testes e não faz parte da porta.
 */
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
