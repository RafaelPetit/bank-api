# 0004. Ledger de partidas dobradas

- Status: Aceita
- Data: 2026-11-03
- Registrada retroativamente em 2026-11-25

## Contexto

Um sistema que guarda só o saldo e o sobrescreve a cada operação perde o histórico. Não dá para
explicar como a conta chegou ao valor atual, nem reconstruí-lo se um bug gravar um número errado.
Auditoria e reconciliação exigem o registro de cada movimento.

## Decisão

Todo movimento de dinheiro é uma `Transaction` do contexto `ledger`, composta por `LedgerEntry`s:
lançamentos de débito ou crédito, sempre com valor positivo.

- Uma transação só existe balanceada: os débitos somam o mesmo que os créditos, numa única moeda,
  com pelo menos dois lançamentos. Se não somar, `UnbalancedTransactionException`.
- Os lançamentos são imutáveis, e o ledger só aceita novos (`append`). Nada é alterado ou
  removido; um erro se corrige com uma nova transação de estorno.
- O ledger é a fonte da verdade. O saldo guardado na `Account` é um snapshot dele, mantido para a
  leitura ser direta e para a regra de saldo insuficiente e o lock otimista terem onde atuar.

## Alternativas consideradas

- **Só o saldo na conta, sobrescrito:** simples, mas sem histórico nem forma de provar que o valor
  está certo.
- **Uma tabela de movimentos simples, com sinal:** guarda o histórico, mas não garante que o
  dinheiro que sai de um lugar entra em outro. Um bug que credite sem debitar passa despercebido.
- **Calcular o saldo somando o ledger a cada leitura:** não há snapshot para divergir, mas a
  leitura fica proporcional ao histórico da conta, e a regra de saldo insuficiente precisaria
  travar todos os lançamentos da conta.

## Consequências

- Toda transação soma zero por construção: um bug não cria nem destrói dinheiro.
- Qualquer saldo pode ser reconstruído e conferido somando os lançamentos da conta.
- Saldo e ledger precisam ser gravados na mesma transação de banco, ou divergem. Hoje o saldo
  está no PostgreSQL e o ledger ainda em memória; juntar os dois no mesmo commit é uma etapa da
  Fase 2.
- Corrigir um erro deixa um estorno visível em vez de uma edição silenciosa, que é o que a
  auditoria precisa.
