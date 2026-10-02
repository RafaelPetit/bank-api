<div align="center">

# 🏦 bank-api

**Core de uma fintech construído do zero, com foco obsessivo em correção financeira, concorrência e system design de alta escala.**

![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Quarkus](https://img.shields.io/badge/Quarkus-3.40.1-4695EB?logo=quarkus&logoColor=white)
![Arquitetura](https://img.shields.io/badge/Arquitetura-Hexagonal-6DB33F)
![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)

</div>

---

## 📖 Sobre o projeto

`bank-api` é o backend de uma fintech (estilo Nubank/PicPay) desenvolvido como **projeto de
estudo e portfólio**. O objetivo **não** é acumular funcionalidades — é demonstrar, de forma
progressiva e justificada por trade-offs reais, a jornada de evolução arquitetural de um sistema
que lida com **dinheiro**: do domínio puro bem modelado até uma arquitetura distribuída,
resiliente e observável, pronta para alta carga.

Cada etapa do [roadmap](#-roadmap) representa uma competência de engenharia sendo exercitada
e comprovada na prática.

> ⚠️ **Por que dinheiro é diferente?** Um sistema financeiro não tolera "quase certo". Um
> arredondamento mal feito, uma corrida entre duas transferências ou uma mensagem processada
> duas vezes significam dinheiro criado ou destruído. Este projeto trata correção, concorrência
> e idempotência como requisitos de primeira classe desde a primeira linha de código.

---

## 🎯 Competências demonstradas

- **Domain-Driven Design** tático — modelo de domínio rico, _value objects_ imutáveis e
  agregados com invariantes protegidas por construção.
- **Arquitetura Hexagonal (Ports & Adapters)** — domínio 100% isolado de frameworks.
- **Correção financeira** — modelagem de dinheiro sem ponto flutuante e **ledger de partidas
  dobradas** (_double-entry_).
- **Concorrência e consistência** — controle de concorrência explícito e operações idempotentes.
- **Sistemas distribuídos** — mensageria com Kafka, _Outbox Pattern_ e orquestração de _Sagas_.
- **Cloud-native & operação** — containerização, Kubernetes, compilação nativa (GraalVM),
  resiliência e observabilidade.

---

## 🧭 Decisões de arquitetura (o "porquê")

### Arquitetura Hexagonal
A dependência sempre aponta **para dentro**: a infraestrutura conhece o domínio, nunca o
contrário. O pacote `domain` é Java puro — **sem nenhuma anotação de framework** (`@Entity`,
`@Inject`, Jackson, Panache...). Isso mantém as regras de negócio testáveis em milissegundos,
sem subir o Quarkus, e permite trocar tecnologia de borda (REST, persistência, mensageria)
sem tocar no núcleo.

### Ledger de partidas dobradas (_double-entry_)
Saldo **não** é um campo que se sobrescreve (`saldo = saldo - valor`) — isso destrói a história
e é uma bomba sob concorrência. Todo movimento de dinheiro é um **lançamento imutável e
_append-only_**, e cada transação gera pernas que **somam zero** (debita A, credita B). O saldo
é uma **projeção** desses lançamentos. Resultado: auditoria total, reconciliação possível e uma
invariante de integridade matematicamente verificável.

### Dinheiro como _value object_
`Money` é imutável e proíbe `double`/`float` (usa `BigDecimal`/menor unidade com
arredondamento controlado). Operações entre moedas diferentes falham explicitamente.

### Idempotência e concorrência
Operações financeiras são idempotentes (uma dupla submissão **não** move dinheiro duas vezes),
e o acesso concorrente às contas é protegido por controle de concorrência explícito
(_optimistic locking_). A integração entre banco e mensageria usa o **Outbox Pattern** para
resolver o _dual-write problem_.

---

## 🗂️ Estrutura do projeto

Organizado como **monólito modular** (_context-first_): módulos com fronteiras nítidas que
poderão ser extraídos como serviços quando — e **se** — houver justificativa mensurável.

```
petit.bank
├── <contexto>                      # ex.: accounts, ledger, payments, identity
│   ├── domain                      # núcleo puro (Java, sem frameworks)
│   │   ├── model                   # entidades, value objects, agregados, eventos
│   │   ├── port
│   │   │   ├── in                  # driving ports (casos de uso)
│   │   │   └── out                 # driven ports (repositórios, publishers)
│   │   └── service                 # implementação dos casos de uso
│   └── infrastructure              # adapters (o mundo externo)
│       ├── adapter.in.rest         # REST (Quarkus)
│       ├── adapter.out.persistence # @Entity + mappers
│       └── adapter.out.messaging   # Kafka
└── shared                          # kernel compartilhado mínimo (ex.: Money)
```

---

## 🛠️ Stack tecnológica

| Camada            | Tecnologia                                      |
|-------------------|-------------------------------------------------|
| Linguagem         | Java 25 (records, sealed types, pattern matching) |
| Framework         | Quarkus 3.40.1                                   |
| Arquitetura       | Hexagonal (Ports & Adapters) + DDD               |
| Mensageria        | Apache Kafka _(planejado)_                        |
| Build             | Maven (Wrapper)                                  |
| Empacotamento     | JVM e imagem nativa (GraalVM)                     |
| Orquestração      | Docker + Kubernetes _(planejado)_                 |
| Frontend          | React _(planejado)_                               |

---

## 🚀 Roadmap

Cada fase comprova uma competência. A divisão em microserviços aparece **tardiamente** e de
forma deliberada — saber _quando não distribuir_ faz parte da proposta.

| Fase | Foco                                                      | Status |
|:----:|-----------------------------------------------------------|:------:|
| 0    | Domínio puro + Hexagonal (monólito modular)               | 🔨 Em andamento |
| 1    | Concorrência + consistência (locking, idempotência)       | ⏳ |
| 2    | Persistência + Outbox + testes de integração              | ⏳ |
| 3    | Kafka + eventos + Saga                                    | ⏳ |
| 4    | Extração do primeiro serviço + containers + Kubernetes    | ⏳ |
| 5    | Resiliência (retry, circuit breaker, backpressure) + observabilidade | ⏳ |
| 6    | _Load testing_ + tuning (imagem nativa / GraalVM)         | ⏳ |

---

## ⚙️ Como executar

Pré-requisitos: **JDK 25** e Docker (para as fases com infraestrutura).

```shell
# Modo de desenvolvimento (live reload + Dev UI em http://localhost:8080/q/dev/)
./mvnw quarkus:dev

# Testes unitários
./mvnw test

# Build e verificação (inclui testes de integração)
./mvnw verify

# Empacotamento (gera target/quarkus-app/quarkus-run.jar)
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar

# Imagem nativa (requer GraalVM; ou use -Dquarkus.native.container-build=true)
./mvnw package -Dnative
./target/bank-api-1.0.0-runner
```

---

## 📌 Status atual

Projeto em **Fase 0**: modelagem do domínio puro da transferência de dinheiro
(`Money`, `Account`, `Transaction`/`LedgerEntry` e as portas). O _scaffold_ inicial do Quarkus
está sendo substituído pelo primeiro _bounded context_.

---

## 👤 Autor

Desenvolvido por **Rafael Petit** como projeto principal de portfólio.

- GitHub: [@rafaelpetit](https://github.com/rafaelpetit)
- LinkedIn: [rafaelpetit](https://linkedin.com/in/rafaelpetit)

---

<div align="center">
<sub>Projeto de estudo focado em system design, engenharia de software e Java moderno.</sub>
</div>
