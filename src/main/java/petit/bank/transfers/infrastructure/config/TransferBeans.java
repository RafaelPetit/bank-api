package petit.bank.transfers.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import petit.bank.accounts.domain.port.out.AccountRepository;
import petit.bank.ledger.domain.port.out.LedgerRepository;
import petit.bank.transfers.domain.port.in.TransferMoneyUseCase;
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
    TransferMoneyUseCase transferMoneyUseCase(AccountRepository accounts, LedgerRepository ledger) {
        return new RetryingTransferMoneyUseCase(new TransferMoneyService(accounts, ledger), 3);
    }
}
