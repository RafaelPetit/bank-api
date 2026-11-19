package petit.bank.transfers.infrastructure.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.shared.Money;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;

import java.util.Currency;

/**
 * Corpo JSON do pedido de transferência: o formato da API, separado do formato da aplicação.
 * Valida a forma do texto recebido e o converte em {@link TransferMoneyCommand}.
 */
public record TransferRequest(
        @NotBlank String from,
        @NotBlank String to,
        @NotBlank @Pattern(regexp = "\\d{1,15}(\\.\\d{1,4})?") String amount,
        @NotBlank String currency) {

    TransferMoneyCommand toCommand() {
        return new TransferMoneyCommand(
                AccountId.of(from),
                AccountId.of(to),
                Money.ofExact(amount, Currency.getInstance(currency))
        );
    }
}
