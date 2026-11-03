package petit.bank.shared;

import java.util.Currency;

/**
 * Lançada quando se tenta operar (somar, subtrair, comparar) {@link Money} de moedas
 * diferentes — operação sem sentido, pois não existe câmbio implícito. É exceção de
 * domínio (unchecked): sinaliza violação de regra de negócio, não erro técnico.
 */
public class CurrencyMismatchException extends RuntimeException{

    public CurrencyMismatchException(Currency expected, Currency actual) {
        super("Operação entre moedas diferentes: %s vc %s"
                .formatted(expected.getCurrencyCode(), actual.getCurrencyCode()));
    }
}
