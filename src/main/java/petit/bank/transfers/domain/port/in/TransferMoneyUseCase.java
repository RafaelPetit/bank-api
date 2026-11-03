package petit.bank.transfers.domain.port.in;

import petit.bank.ledger.domain.model.TransactionId;

/**
 * Porta de entrada para transferir dinheiro entre contas. É o contrato usado pelos adapters
 * de entrada (REST, mensageria...), que não conhecem a implementação. Debita a origem,
 * credita o destino e registra a transação balanceada no ledger, devolvendo o id dessa
 * transação. Falha se alguma conta não existir, se o saldo da origem for insuficiente ou se
 * as moedas forem diferentes, sem alterar nenhum saldo.
 */
public interface TransferMoneyUseCase {

    TransactionId transfer(TransferMoneyCommand command);
}
