package petit.bank.transfers.infrastructure.adapter.in.rest;

/**
 * Corpo das respostas de erro. {@code code} é estável, para o cliente tratar por programa;
 * {@code message} é fixa e nunca expõe saldo ou detalhes internos.
 */
public record ErrorResponse(String code, String message) {
}
