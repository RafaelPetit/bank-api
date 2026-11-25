# 0003. Dinheiro como value object com BigDecimal

- Status: Aceita
- Data: 2026-11-03
- Registrada retroativamente em 2026-11-25

## Contexto

`double` e `float` são ponto flutuante binário: `0.1 + 0.2` não dá `0.3`, e o erro se acumula em
cada operação. Um valor sem moeda permite somar real com dólar sem ninguém perceber. E o `equals`
do `BigDecimal` considera a escala, então `1.0` e `1.00` são diferentes.

## Decisão

`Money` é um record imutável com quantia (`BigDecimal`) e moeda (`Currency`):

- o construtor normaliza a quantia para a escala da moeda (2 casas em BRL), com `HALF_EVEN`;
- `Money.ofExact` recusa valores com mais casas do que a moeda permite, e é usado na borda HTTP
  para não arredondar em silêncio o que o cliente enviou;
- operações entre moedas diferentes lançam `CurrencyMismatchException`;
- no banco, a coluna é `NUMERIC(19,4)`, que guarda o `BigDecimal` sem conversão.

## Alternativas consideradas

- **`double`/`float`:** imprecisão binária, inaceitável em dinheiro.
- **`long` em centavos:** exato e rápido, mas a menor unidade muda por moeda (0 casas no iene, 3 no
  dinar do Bahrein), e a conversão para exibir e receber valores se espalha pelo código. Também
  não acomoda frações abaixo do centavo, que juros e câmbio vão precisar.
- **JSR 354 (Moneta):** traz uma dependência externa para o domínio, que deve ser Java puro, e uma
  API muito maior do que o projeto precisa.

## Consequências

- Não existe `Money` sem moeda ou com escala errada: a invariante vale por construção.
- Dois valores iguais são iguais no `equals`, independente de como foram escritos.
- O arredondamento acontece em um único lugar, com regra explícita (`HALF_EVEN`, sem viés
  acumulado).
- `BigDecimal` é mais lento que `long` e aloca objetos. Perto do custo de I/O do banco, isso não
  aparece; se aparecer, será medido na Fase 6.
