# 0002. Monólito modular com arquitetura hexagonal

- Status: Aceita
- Data: 2026-10-02
- Registrada retroativamente em 2026-11-25

## Contexto

O bank-api é um core financeiro que vai crescer por fases: persistência, mensageria, extração de
serviço e Kubernetes. A infraestrutura muda ao longo do caminho (memória, depois PostgreSQL, depois
Kafka), mas as regras de dinheiro não podem mudar junto, e precisam ser testadas sem banco nem
framework.

Começar com microserviços traria rede, consistência distribuída e operação de vários deploys antes
de existir carga que justifique esse custo.

## Decisão

Um único deployável, dividido em contextos (`accounts`, `ledger`, `transfers`) como pacotes de
primeiro nível, cada um com `domain` e `infrastructure`.

Dentro de cada contexto, Ports & Adapters:

- o domínio é Java puro, sem anotações nem imports de framework (`io.quarkus`, `jakarta`);
- as portas são interfaces declaradas no domínio;
- os adapters (REST, banco, mensageria) ficam na infraestrutura e implementam ou chamam as portas;
- a dependência sempre aponta para dentro: infraestrutura → domínio.

Um serviço só é extraído quando houver motivo mensurável (Fase 4), e começando por um.

## Alternativas consideradas

- **Camadas tradicionais (controller, service, repository):** o domínio acaba dependendo do
  repositório e do framework, e trocar a persistência mexe na regra de negócio.
- **Microserviços desde o início:** transações distribuídas e vários deploys antes de haver carga.
  Uma fronteira errada entre serviços custa caro para corrigir; dentro de um monólito, é um
  refactor.
- **Pacotes por camada técnica no topo (`domain/`, `infrastructure/`):** espalha cada contexto pelo
  projeto e dificulta extrair um deles depois.

## Consequências

- O domínio é testado sem subir o Quarkus, em milissegundos.
- Trocar um adapter não toca o domínio. Na Fase 2, o repositório em memória deu lugar ao
  PostgreSQL sem mudar nenhuma linha do domínio nem do caso de uso.
- Há mais código: interfaces para as portas e mapeamento manual entre HTTP, domínio e banco.
- As fronteiras entre contextos são só convenção de pacote. Nada no build impede um import
  indevido; um teste de arquitetura (ArchUnit) ou módulos Maven podem garantir isso no futuro.