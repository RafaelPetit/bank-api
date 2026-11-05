package petit.bank.transfers.infrastructure.adapter.in.rest;

/**
 * Resposta de uma transferência concluída: a identidade da transação registrada.
 */
public record TransferResponse(String transactionId) {
}
