# 0006. Política de interfaces nas portas

- Status: Aceita
- Data: 2026-11-03
- Registrada retroativamente em 2026-11-25

## Contexto

Na arquitetura hexagonal, a dúvida recorrente é quando criar uma interface. Sem regra, o projeto
vira uma mistura: algumas portas com interface, outras sem, e interfaces criadas "para o caso de
um dia existir outra implementação".

## Decisão

- **Portas de saída:** sempre interface. É a inversão de dependência: o domínio declara o que
  precisa, e a infraestrutura implementa.
- **Portas de entrada:** sempre interface, para todo caso de uso ter a mesma forma e para o adapter
  de entrada depender do contrato, não da classe concreta.
- **Fora das portas** (value objects, entidades, serviços de domínio): classe concreta. Só ganha
  interface com um motivo concreto no momento.
- A implementação é nomeada pelo que é (`TransferMoneyService`), não com o sufixo `Impl`.

"Pode surgir outra implementação" não é motivo aceito para criar uma interface.

## Alternativas consideradas

- **Interface para tudo:** dobra os arquivos sem desacoplar nada. `Money` não tem uma segunda
  implementação possível.
- **Interface só quando houver duas implementações:** a porta de saída ficaria sem interface até o
  segundo adapter, e o domínio dependeria da infraestrutura nesse meio-tempo.
- **Portas de entrada sem interface, com o adapter chamando o serviço direto:** mais simples e
  defensável. Foi descartada pela consistência entre as portas e porque a interface permite
  decorar o caso de uso.

## Consequências

- Toda porta tem a mesma forma, e quem lê o código sabe onde procurar.
- Na Fase 1, o retry e a idempotência entraram como decorators de `TransferMoneyUseCase`, montados
  em `TransferBeans`. O `TransferResource` continua chamando a mesma interface, sem saber que eles
  existem.
- Custo: um arquivo a mais por caso de uso, mesmo quando só existe uma implementação.
