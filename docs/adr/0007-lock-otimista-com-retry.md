# 0007. Lock otimista por versão, com retry

- Status: Aceita
- Data: 2026-11-10
- Registrada retroativamente em 2026-11-25

## Contexto

Duas transferências na mesma conta ao mesmo tempo é o caso normal, não a exceção. Sem controle,
as duas leem o mesmo saldo, e a segunda gravação sobrescreve a primeira (lost update): o dinheiro
de uma delas some. O `TransferMoneyConcurrencyTest` reproduziu isso de forma determinística antes
da correção.

## Decisão

- A `Account` carrega a versão em que foi lida. O domínio só carrega a versão; quem confere é a
  persistência.
- Uma conta só é gravada se a versão guardada ainda for a que foi lida. Se não for,
  `ConcurrentAccountUpdateException`. No PostgreSQL, essa conferência é o próprio
  `UPDATE ... WHERE version = ?` (ADR 0009).
- `saveAll` grava origem e destino como uma unidade: tudo ou nada.
- O decorator `RetryingTransferMoneyUseCase` repete a transferência inteira, relendo as contas, em
  até 3 tentativas. Só conflitos são repetidos; erros de negócio, como saldo insuficiente, sobem
  na primeira vez.
- Esgotadas as tentativas, a API responde `409 Conflict`.

## Alternativas consideradas

- **Lock pessimista (`SELECT ... FOR UPDATE`):** não há conflito a resolver, mas a linha fica
  travada durante toda a operação. Com contas muito disputadas, as transferências fazem fila e o
  risco de deadlock cresce. Volta a ser candidato se o conflito virar a regra, o que será medido
  na Fase 6.
- **`synchronized` ou lock em memória:** só vale dentro de uma JVM. Com duas instâncias da
  aplicação, não protege nada.
- **Fila serializando as operações por conta:** elimina o conflito, mas torna a transferência
  assíncrona cedo demais.
- **Deixar o retry só para o cliente:** devolve o problema para quem chama, e cada cliente
  reimplementa a mesma lógica.

## Consequências

- Sem disputa, não há espera nem lock longo: o caso comum é rápido.
- Uma conta muito disputada gera retries e, no limite, `409`. O cliente pode repetir o pedido com
  segurança graças à idempotency key (ADR 0008).
- O retry é imediato, sem backoff nem jitter: sob disputa alta, as tentativas podem colidir de
  novo. Backoff entra na Fase 5.
