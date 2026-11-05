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
 */
@ApplicationScoped
@IfBuildProfile("dev")
public class DevAccountsSeeder {
    private static final Currency BRL = Currency.getInstance("BRL");

    void seed(@Observes StartupEvent event, AccountRepository accounts) {
        accounts.save(Account.restore(AccountId.of("11111111-1111-1111-1111-111111111111"),
                Money.of("100.00", BRL)));

        accounts.save(Account.restore(AccountId.of("22222222-2222-2222-2222-222222222222"),
                Money.of("0.00", BRL)));
    }
}
