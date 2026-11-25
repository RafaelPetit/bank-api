package petit.bank.accounts.infrastructure.config;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.port.out.AccountRepository;
import petit.bank.shared.Money;

import java.util.Currency;

/**
 * Cria duas contas de exemplo ao iniciar em dev mode, enquanto não há endpoint de abertura de
 * conta. {@code @IfBuildProfile("dev")} garante que esta classe nunca exista em produção.
 * O banco sobrevive ao live reload, então as contas só são criadas se ainda não existirem.
 */
@ApplicationScoped
@IfBuildProfile("dev")
public class DevAccountsSeeder {
    private static final Currency BRL = Currency.getInstance("BRL");
    private static final AccountId ORIGEM = AccountId.of("11111111-1111-1111-1111-111111111111");

    void seed(@Observes StartupEvent event, AccountRepository accounts) {
        if (accounts.findById(ORIGEM).isPresent()) {
            return;
        }

        Account origem = Account.open(ORIGEM, BRL);

        origem.credit(Money.of("100.00", BRL));
        accounts.save(origem);

        accounts.save(Account.open(AccountId.of("22222222-2222-2222-2222-222222222222"), BRL));
    }
}
