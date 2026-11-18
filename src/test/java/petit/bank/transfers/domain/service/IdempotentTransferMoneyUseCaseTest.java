package petit.bank.transfers.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.model.InsufficientFundsException;
import petit.bank.ledger.domain.model.TransactionId;
import petit.bank.shared.Money;
import petit.bank.transfers.domain.model.IdempotencyKey;
import petit.bank.transfers.domain.model.IdempotencyKeyReusedException;
import petit.bank.transfers.domain.model.TransferInProgressException;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;
import petit.bank.transfers.domain.port.in.TransferMoneyUseCase;
import petit.bank.transfers.infrastructure.adapter.out.persistence.InMemoryIdempotencyStore;

import java.util.Currency;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes da idempotência: repetir a mesma chave não executa de novo, a mesma chave com outro
 * pedido é recusada, uma chave em processamento é recusada e uma falha libera a chave.
 */
public class IdempotentTransferMoneyUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private final IdempotencyKey key = IdempotencyKey.generate();
    private final TransferMoneyCommand command = new TransferMoneyCommand(AccountId.generate(), AccountId.generate(),
            Money.of("10.00", BRL), key);
    private final InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    private final AtomicInteger executions = new AtomicInteger();

    private final TransferMoneyUseCase transfersOk = cmd -> {
        executions.incrementAndGet();
        return TransactionId.generate();
    };

    @Test
    @DisplayName("repetir a mesma chave devolve a mesma transação sem executar de novo")
    void repeticaoNaoExecutaDeNovo() {
        IdempotentTransferMoneyUseCase useCase = new IdempotentTransferMoneyUseCase(transfersOk, store);

        TransactionId primeira = useCase.transfer(command);
        TransactionId segunda = useCase.transfer(command);

        assertEquals(primeira, segunda);
        assertEquals(1, executions.get());
    }

    @Test
    @DisplayName("a mesma chave com outro pedido é recusada")
    void mesmaChaveOutroPedido() {
        IdempotentTransferMoneyUseCase useCase = new IdempotentTransferMoneyUseCase(transfersOk, store);
        useCase.transfer(command);

        TransferMoneyCommand outroValor = new TransferMoneyCommand(command.from(), command.to(),
                Money.of("99.00", BRL), key);

        assertThrows(IdempotencyKeyReusedException.class,
                () -> useCase.transfer(outroValor));
        assertEquals(1, executions.get());
    }

    @Test
    @DisplayName("uma chave ainda em processamento é recusada")
    void chaveEmProcessamento() {
        IdempotentTransferMoneyUseCase useCase = new IdempotentTransferMoneyUseCase(transfersOk, store);
        store.reserve(command);

        assertThrows(TransferInProgressException.class,
                () -> useCase.transfer(command));
        assertEquals(0, executions.get());
    }

    @Test
    @DisplayName("uma falha libera a chave para uma nova tentativa")
    void falhaLiberaAChave() {
        TransferMoneyUseCase falhaNaPrimeira = cmd -> {
            if (executions.incrementAndGet() == 1) {
                throw new InsufficientFundsException();
            }
            return TransactionId.generate();
        };
        IdempotentTransferMoneyUseCase useCase = new IdempotentTransferMoneyUseCase(falhaNaPrimeira, store);

        assertThrows(InsufficientFundsException.class, () -> useCase.transfer(command));
        useCase.transfer(command);

        assertEquals(2, executions.get());
    }

}
