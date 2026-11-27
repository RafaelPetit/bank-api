# 0010. Ledger no banco, append-only e sem chave estrangeira entre contextos

- Status: Aceita
- Data: 2026-11-27

## Contexto

O ledger passa da memória para o PostgreSQL. A ADR 0004 define que ele é append-only, mas até
aqui quem garantia isso era só o código: a porta `LedgerRepository` expõe apenas `append`.
Qualquer acesso direto ao banco, como um script de correção, poderia alterar ou apagar um lançamento.

Cada lançamento também guarda o id da conta, que pertence a outro contexto (ADR 0005).

## Decisão

- Duas tabelas: `ledger_transactions`, uma linha por transação, e `ledger_entries`, uma linha por
  lançamento. A chave primária dos lançamentos é `(transaction_id, position)`, que preserva a
  ordem e impede registrar a mesma transação duas vezes.
- `recorded_at` é preenchido pelo banco (`DEFAULT now()`), com um relógio só para todas as
  instâncias.
- Triggers recusam qualquer `UPDATE` ou `DELETE` nas duas tabelas.
- `ledger_entries.account_id` não tem chave estrangeira para `accounts`. A chave estrangeira de
  `ledger_entries` para `ledger_transactions` existe, porque as duas tabelas são do mesmo
  contexto.
- `CHECK` repete no banco as invariantes do `LedgerEntry`: valor positivo e direção `DEBIT` ou
  `CREDIT`.

## Alternativas consideradas

- **Confiar só na porta sem update nem delete:** protege o código da aplicação, mas não o banco.
- **Revogar `UPDATE` e `DELETE` do usuário da aplicação:** também funciona, mas depende de como
  cada ambiente cria os usuários e as permissões, fora das migrações. O trigger vai junto com o
  esquema e vale em qualquer ambiente.
- **Chave estrangeira para `accounts`:** garante no banco que a conta existe, mas amarra os dois
  contextos no esquema. A ordem das migrações passaria a depender do outro contexto, e no dia em
  que o ledger tiver banco próprio (Fase 4) a chave teria que ser removida, tirando uma garantia
  que todos assumiam. Quem confere que as contas existem é o `transfers`, que as carrega antes de
  registrar a transação.

## Consequências

- Um `UPDATE` ou `DELETE` no ledger é recusado, venha da aplicação ou de um acesso manual. Uma
  correção só entra como estorno.
- Um lançamento pode apontar para uma conta que não existe se algum código pular o `transfers`.
  É o custo de manter a fronteira entre os contextos.
- O trigger não dispara em `TRUNCATE`, e o dono da tabela pode desligá-lo com `ALTER TABLE`. Hoje
  o dono é o mesmo usuário da aplicação, que roda as migrações. O trigger protege contra erros,
  não contra alguém mal-intencionado; para isso, a resposta é separar o usuário das migrações do
  usuário da aplicação e auditar o acesso ao banco.
- A soma zero de cada transação continua garantida só pelo domínio. Conferi-la no banco exigiria
  um trigger adiado até o commit, o que não compensa enquanto toda gravação passa pela
  `Transaction`.
