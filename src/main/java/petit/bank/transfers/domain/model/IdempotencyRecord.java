package petit.bank.transfers.domain.model;

import petit.bank.ledger.domain.model.TransactionId;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;

/**
 * O que se sabe sobre um pedido já recebido: ou ele continua a ser processado, ou já foi
 * concluído e gerou uma transação. São os únicos dois estados possíveis, e o {@code sealed} garante isso.
 */
public sealed interface IdempotencyRecord {

    TransferMoneyCommand command();

    record InProgress(TransferMoneyCommand command) implements IdempotencyRecord {
    }

    record Completed(TransferMoneyCommand command, TransactionId transactionId) implements IdempotencyRecord {
    }
}
