package petit.bank.transfers.infrastructure.adapter.in.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import petit.bank.accounts.domain.model.Account;
import petit.bank.accounts.domain.model.AccountId;
import petit.bank.accounts.domain.port.out.AccountRepository;
import petit.bank.shared.Money;

import java.util.Currency;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes do adapter REST de transferência com a aplicação Quarkus de pé: conversão do JSON,
 * status HTTP de cada erro de negócio e validação de formato da requisição.
 */
@QuarkusTest
public class TransferResourceTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    @Inject
    AccountRepository accounts;

    @Test
    @DisplayName("transferência válida responde 201 com o id da transação e move os saldos")
    void transferenciaValida() {
        AccountId origem = contaComSaldo("100.00", BRL);
        AccountId destino = contaComSaldo("0.00", BRL);

        post(corpo(origem.toString(), destino.toString(), "30.00", "BRL"))
                .then()
                .statusCode(201)
                .body("transactionId", notNullValue());

        assertEquals(Money.of("70.00", BRL), saldoDe(origem));
        assertEquals(Money.of("30.00", BRL), saldoDe(destino));
    }

    @Test
    @DisplayName("saldo insuficiente responde 422 e não altera o saldo")
    void saldoInsuficiente() {
        AccountId origem = contaComSaldo("10.00", BRL);
        AccountId destino = contaComSaldo("0.00", BRL);

        post(corpo(origem.toString(), destino.toString(), "50.00", "BRL"))
                .then()
                .statusCode(422)
                .body("code", equalTo("INSUFFICIENT_FUNDS"));

        assertEquals(Money.of("10.00", BRL), saldoDe(origem));
    }

    @Test
    @DisplayName("moeda diferente da conta responde 422")
    void moedaDiferente() {
        AccountId origem = contaComSaldo("100.00", BRL);
        AccountId destino = contaComSaldo("0.00", USD);

        post(corpo(origem.toString(), destino.toString(), "10.00", "BRL"))
                .then()
                .statusCode(422)
                .body("code", equalTo("CURRENCY_MISMATCH"));
    }

    @Test
    @DisplayName("conta inexistente responde 404")
    void contaInexistente() {
        AccountId origem = contaComSaldo("100.00", BRL);

        post(corpo(origem.toString(), AccountId.generate().toString(), "10.00", "BRL"))
                .then()
                .statusCode(404)
                .body("code", equalTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("id malformado responde 400")
    void idMalformado() {
        AccountId destino = contaComSaldo("0.00", BRL);

        post(corpo("nao-e-um-uuid", destino.toString(), "10.00", "BRL"))
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("valor com mais casas do que a moeda permite responde 400, sem arredondar")
    void valorComCasasDemais() {
        AccountId origem = contaComSaldo("100.00", BRL);
        AccountId destino = contaComSaldo("0.00", BRL);

        post(corpo(origem.toString(), destino.toString(), "10.555", "BRL"))
                .then()
                .statusCode(400);

        assertEquals(Money.of("100.00", BRL), saldoDe(origem));
    }

    @Test
    @DisplayName("valor em notação científica é barrado pela validação de formato")
    void valorEmNotacaoCientifica() {
        AccountId origem = contaComSaldo("100.00", BRL);
        AccountId destino = contaComSaldo("0.00", BRL);

        post(corpo(origem.toString(), destino.toString(), "1E+1000000", "BRL"))
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("campo obrigatório ausente responde 400")
    void campoAusente() {
        post("{\"from\": \"abc\"}")
                .then()
                .statusCode(400);
    }

    private Response post(String corpo) {
        return given().contentType("application/json").body(corpo).when().post("/transfers");
    }

    private String corpo(String from, String to, String amount, String currency) {
        return """
                {"from": "%s", "to": "%s", "amount": "%s", "currency": "%s"}
                """.formatted(from, to, amount, currency);
    }

    private AccountId contaComSaldo(String valor, Currency moeda) {
        Account conta = Account.restore(AccountId.generate(), Money.of(valor, moeda));

        accounts.save(conta);

        return conta.id();
    }

    private Money saldoDe(AccountId id) {
        return accounts.findById(id).orElseThrow().balance();
    }
}
