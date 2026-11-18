package petit.bank.transfers.domain.port.in;

import petit.bank.accounts.domain.model.AccountId;
import petit.bank.shared.Money;
import petit.bank.transfers.domain.model.IdempotencyKey;

import java.util.Objects;

/**
 * Pedido de transferência de dinheiro entre duas contas (padrão Command): um objeto imutável
 * que carrega a intenção do chamador até o caso de uso.
 * A idempotency key identifica o pedido, para que uma repetição não vire uma segunda transferência.
 * Valida apenas a forma do pedido (nada nulo); as regras de negócio, como valor positivo, saldo suficiente e contas
 * diferentes, ficam no domínio.
 */
public record TransferMoneyCommand(AccountId from, AccountId to, Money amount, IdempotencyKey idempotencyKey) {

    public TransferMoneyCommand {
        Objects.requireNonNull(from, "from não pode ser nulo");
        Objects.requireNonNull(to, "to não pode ser nulo");
        Objects.requireNonNull(amount, "amount não pode ser nulo");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey não pode ser nula");
    }
}
