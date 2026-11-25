# 0001. Registrar decisões de arquitetura

- Status: Aceita
- Data: 2026-11-25

## Contexto

O objetivo deste projeto é mostrar raciocínio de engenharia, e esse raciocínio estava espalhado
em descrições de PR e mensagens de commit. O código mostra o que foi feito, mas não o que foi
descartado nem por quê. Quem chega ao repositório não tem onde encontrar isso.

## Decisão

Cada decisão de arquitetura vira um ADR (Architecture Decision Record) em `docs/adr`, no formato
de Michael Nygard, com uma seção de alternativas consideradas. Os arquivos são numerados em
sequência (`NNNN-titulo.md`).

Um ADR aceito não é editado. Se a decisão mudar, um novo ADR a substitui, e o antigo passa a ter o
status "Substituída por ADR-NNNN".

O ADR entra no mesmo PR que implementa a decisão. As decisões das Fases 0 e 1 foram registradas
retroativamente, com a data em que foram tomadas.

## Alternativas consideradas

- **Só descrições de PR:** ficam no GitHub, fora do repositório, e não formam um histórico que dê
  para ler em sequência.
- **Uma seção de decisões no README:** cresce sem estrutura e não guarda as decisões que foram
  substituídas.

## Consequências

- O histórico de decisões é versionado com o código e revisado no mesmo PR.
- Uma decisão revertida continua visível, junto com o motivo da troca.
- Escrever o ADR passa a fazer parte de cada decisão. Detalhes de implementação não viram ADR,
  para o conjunto não virar ruído.