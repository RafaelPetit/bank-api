package petit.bank.ledger.domain.port.out;

import petit.bank.ledger.domain.model.Transaction;

/**
 * Porta de saída do ledger. O ledger é append-only: transações são apenas acrescentadas,
 * nunca alteradas ou removidas (correções são feitas com uma nova transação de estorno).
 * Por isso o contrato expõe só {@code append}, sem update nem delete.
 */
public interface LedgerRepository {

    void append(Transaction transaction);
}
