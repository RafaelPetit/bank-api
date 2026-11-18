package petit.bank.accounts.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.model.ConcurrentAccountUpdateException;
import petit.bank.shared.Money;

import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes do lock otimista no repositório em memória: cada gravação avança a versão,
 * gravar a partir de uma leitura desatualizada é recusado, e uma gravação em grupo é tudo ou nada.
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

        assertEquals(Money.of("10.00", BRL),
                accounts.findById(conta.id()).orElseThrow().balance());
    }

    @Test
    @DisplayName("gravação em grupo com uma conta desatualizada não grava nenhuma")
    void gravacaoEmGrupoTudoOuNada() {
        Account a = Account.open(AccountId.generate(), BRL);
        Account b = Account.open(AccountId.generate(), BRL);
        accounts.saveAll(List.of(a, b));

        Account aLida = accounts.findById(a.id()).orElseThrow();
        Account bLida = accounts.findById(b.id()).orElseThrow();

        Account bPorOutraOperacao = accounts.findById(b.id()).orElseThrow();
        bPorOutraOperacao.credit(Money.of("5.00", BRL));
        accounts.save(bPorOutraOperacao);

        aLida.credit(Money.of("10.00", BRL));
        bLida.credit(Money.of("10.00", BRL));
        assertThrows(ConcurrentAccountUpdateException.class,
                () -> accounts.saveAll(List.of(aLida, bLida)));

        assertEquals(Money.of("0.00", BRL),
                accounts.findById(aLida.id()).orElseThrow().balance());
        assertEquals(Money.of("5.00", BRL),
                accounts.findById(bLida.id()).orElseThrow().balance());
    }

}
