package petit.bank.ledger.domain.model;

/**
 * Direção de um {@link LedgerEntry}. Convenção deste sistema (perspectiva do titular):
 * CREDIT aumenta o saldo da conta; DEBIT reduz. A igualdade entre total de débitos e
 * créditos numa {@link Transaction} é o que garante que dinheiro não é criado nem destruído.
 */
public enum EntryDirection {
    DEBIT,
    CREDIT
}
