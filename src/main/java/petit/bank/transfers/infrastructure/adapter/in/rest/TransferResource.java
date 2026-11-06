package petit.bank.transfers.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import petit.bank.accounts.domain.model.AccountNotFoundException;
import petit.bank.accounts.domain.model.InsufficientFundsException;
import petit.bank.ledger.domain.model.TransactionId;
import petit.bank.shared.CurrencyMismatchException;
import petit.bank.transfers.domain.port.in.TransferMoneyUseCase;

/**
 * Adapter de entrada HTTP da transferência. Só traduz: requisição em command, resultado em
 * resposta e exceção de domínio em status HTTP. Não contém regra de negócio.
 */
@Path("/transfers")
public class TransferResource {

    private final TransferMoneyUseCase transferMoney;

    public TransferResource(TransferMoneyUseCase transferMoney) {
        this.transferMoney = transferMoney;
    }

    @POST
    public RestResponse<TransferResponse> transfer(@Valid @NotNull TransferRequest request) {
        TransactionId id = transferMoney.transfer(request.toCommand());

        return RestResponse.status(Response.Status.CREATED, new TransferResponse(id.toString()));
    }

    @ServerExceptionMapper
    RestResponse<ErrorResponse> map(AccountNotFoundException e) {
        return RestResponse.status(Response.Status.NOT_FOUND,
                new ErrorResponse("ACCOUNT_NOT_FOUND", "Conta não encontrada"));
    }

    @ServerExceptionMapper
    RestResponse<ErrorResponse> map(InsufficientFundsException e) {
        return unprocessable("INSUFFICIENT_FUNDS", "Saldo insuficiente");
    }

    @ServerExceptionMapper
    RestResponse<ErrorResponse> map(CurrencyMismatchException e) {
        return unprocessable("CURRENCY_MISMATCH", "Moeda diferente da moeda da conta");
    }

    @ServerExceptionMapper
    RestResponse<ErrorResponse> map(IllegalArgumentException e) {
        return RestResponse.status(Response.Status.BAD_REQUEST,
                new ErrorResponse("INVALID_REQUEST", "Requisição inválida"));
    }

    private RestResponse<ErrorResponse> unprocessable(String code, String message) {
        return RestResponse.ResponseBuilder.<ErrorResponse>create(422)
                .entity(new ErrorResponse(code, message))
                .build();
    }
}
