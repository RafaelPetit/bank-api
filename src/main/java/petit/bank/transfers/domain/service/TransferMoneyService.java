package petit.bank.transfers.domain.service;

import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.model.AccountNotFoundException;
import petit.bank.accounts.domain.port.out.AccountRepository;
import petit.bank.ledger.domain.model.Transaction;
import petit.bank.ledger.domain.model.TransactionId;
import petit.bank.ledger.domain.port.out.LedgerRepository;
import petit.bank.transfers.domain.port.in.TransferMoneyCommand;
import petit.bank.transfers.domain.port.in.TransferMoneyUseCase;

import java.util.Objects;

/**
 * Implementação do caso de uso de transferência (Application Service). Orquestra os aggregates
 * sem conter regra de negócio própria: as regras ficam em {@link Account} e {@link Transaction}.
 * Valida e altera tudo em memória antes de persistir, de modo que qualquer falha interrompe a
 * operação antes de gravar algo.
 */
public class TransferMoneyService implements TransferMoneyUseCase {

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;

    public TransferMoneyService(AccountRepository accountRepository, LedgerRepository ledgerRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository não pode ser nulo");
        this.ledgerRepository = Objects.requireNonNull(ledgerRepository, "ledgerRepository não pode ser nulo");
    }

    @Override
    public TransactionId transfer(TransferMoneyCommand command) {
        Account from = findAccount(command.from());
        Account to = findAccount(command.to());

        Transaction transaction = Transaction.transfer(from.id(), to.id(), command.amount());

        from.debit(command.amount());
        to.credit(command.amount());

        accountRepository.save(from);
        accountRepository.save(to);
        ledgerRepository.append(transaction);

        return transaction.id();
    }

    private Account findAccount(AccountId id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
