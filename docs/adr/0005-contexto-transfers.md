# 0005. Contexto transfers orquestra contas e ledger

- Status: Aceita
- Data: 2026-11-03
- Registrada retroativamente em 2026-11-25

## Contexto

Uma transferência envolve dois contextos: debita e credita contas (`accounts`) e registra a
transação (`ledger`). Se a orquestração ficasse em um deles, ele passaria a depender do outro, e
qualquer chamada no sentido contrário criaria uma dependência circular.

## Decisão

Um terceiro contexto, `transfers`, orquestra a operação. Ele usa `accounts` e `ledger` pelas
portas deles (`AccountRepository`, `LedgerRepository`), e nenhum dos dois conhece `transfers`.

O caso de uso (`TransferMoneyUseCase`) vive em `transfers`, junto com o que é próprio da operação
de transferir: retry em conflito e idempotência (Fase 1).

## Alternativas consideradas

- **Orquestrar em `accounts`:** o contexto de contas passa a depender do ledger, e a regra "todo
  movimento gera lançamento" fica escondida num contexto que é sobre contas.
- **Orquestrar em `ledger`:** o ledger passa a alterar saldo, misturando o registro contábil com o
  estado da conta.
- **Eventos entre os contextos (`accounts` publica, `ledger` consome):** é o caminho da Fase 3, mas
  hoje traria consistência eventual sem necessidade, dentro de um único processo.

## Consequências

- As dependências entre contextos não têm ciclo: `transfers` → `accounts` e `transfers` → `ledger`.
- `accounts` e `ledger` podem mudar, ou ser extraídos, sem conhecer o fluxo de transferência.
- O `ledger` ainda usa o `AccountId` do contexto `accounts` para identificar a conta de cada
  lançamento. É uma dependência só de identificador, aceita por ora; se o ledger for extraído,
  esse id passa a ser um tipo dele.
- `transfers` é o candidato natural a virar uma Saga quando entrar a mensageria (Fase 3).
