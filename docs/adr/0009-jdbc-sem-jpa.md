# 0009. Persistência com JDBC puro, sem JPA

- Status: Aceita
- Data: 2026-11-25

## Contexto

A Fase 2 troca o repositório de contas em memória pelo PostgreSQL. O padrão de mercado no Quarkus
é o Hibernate ORM, com JPA ou Panache. Mas a `Account` é um objeto de domínio sem anotações
(ADR 0002), com a própria versão (ADR 0007), e o lock otimista é a peça mais importante desta
persistência.

## Decisão

O `PostgresAccountRepository` usa JDBC direto, com SQL explícito em `PreparedStatement`:

- leitura com `SELECT`, reconstruindo a conta por `Account.restore`;
- conta nova (versão 0) com `INSERT ... ON CONFLICT (id) DO NOTHING`, onde um id que já existe
  conta como conflito;
- conta existente com `UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?`. Se o
  número de linhas afetadas não for 1, `ConcurrentAccountUpdateException`.

`saveAll` é `@Transactional` e grava as contas em ordem de id, para que duas transferências em
sentidos opostos não entrem em deadlock.

O esquema é versionado com Flyway. O dinheiro fica em `NUMERIC(19,4)`, com `CHECK` para saldo e
versão não negativos.

Um teste de contrato (`AccountRepositoryContractTest`) roda a mesma bateria contra o adapter em
memória e contra o PostgreSQL.

## Alternativas consideradas

- **Hibernate ORM, com uma `@Entity` separada do domínio:** o lock otimista vira `@Version`,
  gerado sem aparecer no código. A entidade gerenciada tem ciclo de vida próprio (dirty checking,
  flush no fim da transação), e o adapter teria que copiar o estado entre ela e a `Account`,
  garantindo que o Hibernate confira a versão que o domínio leu, e não a que ele carregou.
- **Panache no modo Active Record:** coloca a persistência no próprio objeto, o oposto de um
  domínio puro.
- **jOOQ ou JDBI:** SQL explícito com menos código repetitivo, mas é uma dependência a mais para
  uma tabela de quatro colunas. Reavaliar quando o número de tabelas crescer.

## Consequências

- O lock otimista está escrito no SQL: quem lê o adapter vê exatamente o que o banco faz.
- O lock vale entre instâncias da aplicação, porque quem arbitra é o banco.
- Mapeamento, tratamento de `SQLException` e fechamento de recursos são manuais, e cada tabela
  nova custa mais código do que com um ORM.
- Uma falha de banco vira `IllegalStateException` e responde `500`, não um erro do cliente.
- A ordenação contra deadlock ainda não tem teste de concorrência contra o banco.
