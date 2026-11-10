package petit.bank.transfers.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.model.ConcurrentAccountUpdateException;
import petit.bank.accounts.domain.model.InsufficientFundsException;
import petit.bank.ledger.domain.model.TransactionId;
import petit.bank.shared.Money;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;
import petit.bank.transfers.domain.port.in.TransferMoneyUseCase;

import java.util.Currency;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes da política de retry: conflitos são repetidos até o limite, e erros de negócio nunca são repetidos.
 */
public class RetryingTransferMoneyUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private final AtomicInteger attempts = new AtomicInteger();
    private final TransactionId transactionId = TransactionId.generate();
    private final TransferMoneyCommand command = new TransferMoneyCommand(AccountId.generate(),
            AccountId.generate(), Money.of("10.00", BRL));

    @Test
    @DisplayName("repete o conflito e devolve o resultado quando uma tentativa dá certo")
    void repeteAteDarCerto() {
        TransferMoneyUseCase conflitoDuasVezes = cmd -> {
            if (attempts.incrementAndGet() <= 2) {
                throw new ConcurrentAccountUpdateException(cmd.from());
            }

            return transactionId;
        };

        TransactionId result = new RetryingTransferMoneyUseCase(conflitoDuasVezes, 3).transfer(command);

        assertEquals(transactionId, result);
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("desiste depois do limite de tentativas e devolve conflito")
    void desisteNoLimite() {
        TransferMoneyUseCase sempreConflita = cmd -> {
            attempts.incrementAndGet();
            throw new ConcurrentAccountUpdateException(cmd.from());
        };

        assertThrows(ConcurrentAccountUpdateException.class,
                () -> new RetryingTransferMoneyUseCase(sempreConflita, 3).transfer(command));

        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("erro de negócio não é repetido")
    void naoRepeteErroDeNegocio() {
        TransferMoneyUseCase semSaldo = cmd -> {
            attempts.incrementAndGet();
            throw new InsufficientFundsException();
        };

        assertThrows(InsufficientFundsException.class,
                () -> new RetryingTransferMoneyUseCase(semSaldo, 3).transfer(command));

        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("exige pelo menos uma tentativa")
    void exigePeloMenosUmaTentativa() {
        TransferMoneyUseCase qualquer = cmd -> transactionId;

        assertThrows(IllegalArgumentException.class,
                () -> new RetryingTransferMoneyUseCase(qualquer, 0));
    }
}
