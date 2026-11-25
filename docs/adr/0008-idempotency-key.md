# 0008. Idempotency key como decorator do caso de uso

- Status: Aceita
- Data: 2026-11-18
- Registrada retroativamente em 2026-11-25

## Contexto

Rede falha. O cliente envia a transferência, a resposta se perde num timeout, e ele não sabe se o
dinheiro saiu. Se repetir o pedido sem proteção, transfere duas vezes. Um clique duplo ou o retry
automático de um gateway causam o mesmo problema.

## Decisão

Todo `POST /transfers` exige o cabeçalho `Idempotency-Key`, gerado pelo cliente (um UUID, por
exemplo), como no draft da IETF "The Idempotency-Key HTTP Header Field".

O decorator `IdempotentTransferMoneyUseCase` envolve o caso de uso, por fora do retry. Antes de
executar, ele reserva a chave de forma atômica na porta `IdempotencyStore`:

- chave nova: fica reservada como "em processamento", a transferência roda e o resultado é
  registrado;
- mesma chave e mesmo pedido, já concluído: devolve a mesma transação, sem mover dinheiro;
- mesma chave com outro pedido: recusado;
- mesma chave ainda em processamento: recusado, para o cliente tentar depois.

Os dois estados possíveis são um `sealed IdempotencyRecord` (`InProgress`, `Completed`), tratado
com `switch` exaustivo. Se a transferência falhar, a chave é liberada para uma nova tentativa.

Respostas HTTP: `400` sem o cabeçalho, `422` para a chave reutilizada com outro pedido e `409`
para o pedido ainda em processamento.

## Alternativas consideradas

- **Deduplicar pelo conteúdo (mesma origem, destino e valor em poucos minutos):** bloqueia duas
  transferências legítimas iguais, porque o servidor não sabe a intenção do cliente.
- **Idempotência dentro do `TransferMoneyService`:** mistura a regra da transferência com o
  controle de repetição, e cada tentativa do retry reservaria e liberaria a chave de novo.
- **Consultar a chave e depois gravá-la (check-then-act):** duas requisições simultâneas passam
  pela consulta e as duas transferem. Por isso a reserva é uma operação atômica.
- **Cabeçalho opcional:** o cliente que esquece fica desprotegido sem perceber. Obrigatório, ele
  falha logo, com `400`.

## Consequências

- Repetir um pedido é seguro, inclusive depois do `409` de conflito de concorrência (ADR 0007).
- Por ficar por fora do retry, uma transferência que sofre vários conflitos usa uma única
  reserva.
- Erros de negócio não são memorizados: uma transferência recusada por saldo insuficiente libera
  a chave, e a mesma chave pode ser usada de novo depois de um depósito.
- O `IdempotencyStore` ainda é em memória: as chaves se perdem ao reiniciar, crescem sem limite e
  não são compartilhadas entre instâncias. A versão persistente, com prazo de retenção, é uma
  etapa da Fase 2.
- Se a aplicação cair entre a reserva e a conclusão, a chave fica "em processamento". O store
  persistente vai precisar de um prazo de expiração para a reserva.
