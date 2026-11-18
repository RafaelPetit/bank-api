package petit.bank.transfers.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import petit.bank.accounts.domain.port.out.AccountRepository;
import petit.bank.ledger.domain.port.out.LedgerRepository;
import petit.bank.transfers.domain.port.in.TransferMoneyUseCase;
import petit.bank.transfers.domain.port.out.IdempotencyStore;
import petit.bank.transfers.domain.service.IdempotentTransferMoneyUseCase;
import petit.bank.transfers.domain.service.RetryingTransferMoneyUseCase;
import petit.bank.transfers.domain.service.TransferMoneyService;

/**
 * Ensina o contêiner do Quarkus a montar o caso de uso de transferência. Existe para que
 * {@link TransferMoneyService} continue em Java puro, sem nenhuma anotação de framework.
 */
@ApplicationScoped
public class TransferBeans {

    @Produces
    @ApplicationScoped
    TransferMoneyUseCase transferMoneyUseCase(AccountRepository accounts, LedgerRepository ledger, IdempotencyStore idempotency) {
        TransferMoneyUseCase transfer = new TransferMoneyService(accounts, ledger);
        TransferMoneyUseCase withRetry = new RetryingTransferMoneyUseCase(transfer, 3);

        return new IdempotentTransferMoneyUseCase(withRetry, idempotency);
    }
}
