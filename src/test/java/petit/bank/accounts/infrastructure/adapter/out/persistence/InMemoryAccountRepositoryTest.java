package petit.bank.accounts.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.model.ConcurrentAccountUpdateException;
import petit.bank.shared.Money;

import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes do lock otimista no repositório em memória: cada gravação avança a versão, e gravar
 * a partir de uma leitura desatualizada é recusado sem alterar o que está guardado.
 */
public class InMemoryAccountRepositoryTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();

    @Test
    @DisplayName("cada gravação avança a versão da conta")
    void gravacaoAvancaVersao() {
        Account conta = Account.open(AccountId.generate(), BRL);
        accounts.save(conta);

        Account lida = accounts.findById(conta.id()).orElseThrow();
        lida.credit(Money.of("10.00", BRL));
        accounts.save(lida);

        assertEquals(2, accounts.findById(conta.id()).orElseThrow().version());
    }


    @Test
    @DisplayName("gravar a partir de uma leitura desatualizada é recusado")
    void leituraDesatualizadaRecusada() {
        Account conta = Account.open(AccountId.generate(), BRL);
        accounts.save(conta);

        Account primeira = accounts.findById(conta.id()).orElseThrow();
        Account segunda = accounts.findById(conta.id()).orElseThrow();

        primeira.credit(Money.of("10.00", BRL));
        accounts.save(primeira);

        segunda.credit(Money.of("99.00", BRL));
        assertThrows(ConcurrentAccountUpdateException.class, () -> accounts.save(segunda));

        assertEquals(Money.of("10.00", BRL), accounts.findById(conta.id()).orElseThrow().balance());
    }

}
