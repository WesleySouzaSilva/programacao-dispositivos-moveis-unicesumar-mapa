# MAPA — Programação para Dispositivos Móveis

> Atividade MAPA (Material de Avaliação Prática da Aprendizagem) da disciplina
> **Programação para Dispositivos Móveis**. O repositório organiza a resposta
> dissertativa e os **snippets de código Kotlin** utilizados como evidência
> técnica, estruturados como um pequeno projeto Android de produção.

---

## Sumário

- [Visão geral](#visão-geral)
- [Arquitetura do projeto Kotlin](#arquitetura-do-projeto-kotlin)
- [Etapa 2 — protótipo desktop](#etapa-2--protótipo-desktop-emulador-mobile)
- [Questões respondidas](#questões-respondidas)
- [Snippets de código](#snippets-de-código)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Convenção de pastas](#convenção-de-pastas)
- [Como o conteúdo foi organizado](#como-o-conteúdo-foi-organizado)
- [Como executar o protótipo desktop](#como-executar-o-protótipo-desktop)
- [Como rodar os testes](#como-rodar-os-testes)
- [Referências bibliográficas](#referências-bibliográficas)

---

## Visão geral

O trabalho consiste em responder, de forma técnica e fundamentada, três
questões propostas na atividade MAPA sobre a linguagem Kotlin, aplicadas ao
contexto de um aplicativo Android corporativo que manipula grandes volumes de
dados de usuários e serviços. Os temas centrais são:

- **A)** Diferença entre arrays e collections, com análise de trade-offs.
- **B)** Manipulação de lista de usuários usando `List` e `MutableList`.
- **C)** Operações funcionais (`filter`, `map`, `sortedBy`, `groupBy`)
  aplicadas a pipelines Android.

A entrega oficial ao AVA é um documento Word formatado conforme normas da
disciplina. Este repositório armazena a **versão-fonte em Markdown** da
resposta e os **snippets Kotlin** compiláveis que sustentam as afirmações
técnicas — organizados segundo a mesma arquitetura de camadas usada em
projetos Android profissionais.

---

## Arquitetura do projeto Kotlin

Os snippets seguem uma separação de camadas inspirada em MVVM/Clean
Architecture, ainda que compactada para o escopo da atividade:

```
┌─────────────────────────────────────────────────────────────────┐
│                     CAMADA DE APRESENTAÇÃO                      │
│  ContatosAdapter / AutoCompleteTextView / RecyclerView          │
│  (recebe apenas DTOs imutáveis — projeções via map)              │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ List<String>, List<Usuario> (imutável)
                              │
┌─────────────────────────────────────────────────────────────────┐
│                     CAMADA DE VIEWMODEL                        │
│  UsuariosViewModel                                               │
│  - expõe List<String> / List<Usuario> (somente leitura)          │
│  - aplica pipelines: filter → filter → sortedBy → map           │
│  - usa asSequence() para lazy evaluation em listas grandes       │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ MutableList<Usuario> (CRUD local)
                              │
┌─────────────────────────────────────────────────────────────────┐
│                     CAMADA DE DOMÍNIO                          │
│  data class Usuario(nome, email, ativo)                          │
│  data class Waste / Sector (placeholders para extensões)         │
└─────────────────────────────────────────────────────────────────┘
```

### Princípios aplicados

| Princípio                              | Onde aparece                                          |
|----------------------------------------|-------------------------------------------------------|
| Default imutável                       | `List<Usuario>` retornado pelo ViewModel              |
| Menor privilégio                       | `MutableList` só dentro do ViewModel                  |
| Pipeline declarativo                   | `filter → sortedBy → map` encadeado                   |
| Lazy evaluation                        | `asSequence()` antes do pipeline                      |
| Projeção para DTO de UI                | `map { "${nome} <${email}>" }`                        |
| Indexação por chave                    | `groupBy { nome.first() }` para a agenda             |

### Mapeamento camada → questão

| Camada          | Questão | Mecanismo Kotlin                           |
|-----------------|---------|--------------------------------------------|
| Apresentação    | **C)**  | `filter` no adapter, `map` para DTO       |
| ViewModel       | **B)**  | `List`/`MutableList`, `sortedBy`, `filter`|
| ViewModel       | **C)**  | Pipeline `asSequence → filter → map`      |
| Domínio         | **A)**  | `data class` (imutável) vs `Array` (fixo) |

---

## Etapa 2 — protótipo desktop (emulador mobile)

Além da resposta dissertativa, a Etapa 2 da atividade pede um **protótipo
executável** que materialize os conceitos das três questões em um app real.
Este repositório entrega esse protótipo como uma aplicação Swing
auto-contida, emulando visualmente um app Android rodando em um smartphone:

```
┌────────────────────────────────────┐
│ ░░░ AppBar azul com gradiente ░░░░ │  ← topo estilo Material
│ ░ Usuários                         │
│ ░ MAPA Mobile — Kotlin             │
├────────────────────────────────────┤
│ [Listar] [Ativos] [Buscar] [...]   │  ← chips coloridos com hover
├────────────────────────────────────┤
│ (●A)  Ana Lima                      │  ← lista com avatar circular
│       ana@empresa.com.br            │
│       Engenharia                    │
├────────────────────────────────────┤
│ (●B)  Bruno Sá   • inativo          │
│       bruno.sa@empresa.com.br       │
│       Engenharia                    │
├────────────────────────────────────┤
│              [ + ]                   │  ← FAB rosa circular flutuante
│  6 usuário(s)                        │
│  Lista inicial (seed)               │
└────────────────────────────────────┘
```

### Arquitetura em camadas do protótipo

| Camada        | Pacote                                          | Responsabilidade                                 |
|---------------|-------------------------------------------------|--------------------------------------------------|
| Domínio       | `br.com.unicesumar.mapa.desktop.domain`         | `data class Usuario` (modelo imutável)          |
| Dados         | `br.com.unicesumar.mapa.desktop.data`           | `RepositorioEmMemoria` (seed + CRUD + pipelines)|
| Apresentação  | `br.com.unicesumar.mapa.desktop.ui`             | `UsuariosApp` (Swing, paleta Material, FAB)     |

### Pipeline de coleções exercitado em runtime

| Operação Kotlin         | Onde aparece no protótipo                              | Questão |
|-------------------------|--------------------------------------------------------|---------|
| `MutableList` (CRUD)    | `RepositorioEmMemoria.usuarios` (interno, privado)    | **B**   |
| `List` imutável exposto | `listarTodos() / listarAtivos() / buscarPorPrefixo()` | **B**   |
| `filter`                | `listarAtivos` — apenas usuários com `ativo == true`   | **C**   |
| `asSequence → sortedBy` | `buscarPorPrefixo` — pipeline preguiçoso ordenado      | **C**   |
| `groupBy`               | `agruparPorInicial` — índice por letra (A, B, C, ...) | **C**   |
| `map` (DTO de UI)       | Renderização da `JList` (string `"${nome} <${email}"`) | **C**   |

### Decisões de UI para emular o visual mobile

A janela é fixada em **360x720 px** (proporção típica de smartphone) e
aplica convenções de Material Design para dar a sensação de uso em celular,
apesar de rodar como `.jar` desktop:

- **AppBar** com gradiente azul (Blue 600 → Blue 800) e título branco.
- **Lista de cards** com **avatar circular colorido** (iniciais + paleta).
- **Botões em chip** arredondados (Listar / Ativos / Buscar / Agrupar / Resetar).
- **Campo de busca** arredondado com borda sutil.
- **FAB rosa** circular no canto inferior direito (Incluir usuário).
- **Status bar inferior** com contador + última ação (padrão snackbar).
- **Excluir** por clique-duplo na linha (UX mobile sem menus de contexto).

A camada de apresentação é totalmente separada do domínio — o que chega na
UI é sempre uma `List<Usuario>` imutável; mutações só acontecem via métodos
do repositório.

### Comportamento esperado

- O app abre com **6 usuários de seed** pré-carregados (lista fixa).
- Botão **Incluir** abre diálogo com nome/e-mail/setor e gera novo ID.
- Botão **Excluir** (clique-duplo na linha) remove o usuário selecionado.
- Botão **Resetar** restaura o estado original da seed (com confirmação).
- Mutações valem **só em runtime** — fechar o `.jar` descarta alterações,
  conforme exigido pela Etapa 2.

---

## Questões respondidas

A resposta completa está em [`mapa-resposta-revisada.md`](./mapa-resposta-revisada.md).
Estrutura do documento:

| Seção        | Conteúdo                                                              |
|--------------|-----------------------------------------------------------------------|
| Introdução   | Contextualização do cenário, tese do trabalho                         |
| **A)**       | Arrays vs Collections: tabela comparativa + 2 vantagens               |
| **B)**       | Lista de nomes de usuários: `List`/`MutableList` + `sortedBy`/`filter` |
| **C)**       | Operações funcionais: `filter`, `map`, `groupBy` com exemplos Android |
| Conclusão    | Síntese integradora das três questões                                 |
| Referências  | ABNT: Senne (2025) + Kotlin Documentation                             |

---

## Snippets de código

O arquivo [`snippets-kotlin.kt`](./snippets-kotlin.kt) contém quatro blocos
compiláveis (Kotlin 1.9+ / AndroidX Lifecycle 2.7+), cada um associado a
uma camada da arquitetura e a uma seção da resposta:

1. **Camada de domínio — `data class Usuario`** — modelo imutável de
   domínio, alicerce de toda a arquitetura.
2. **Camada de ViewModel — `UsuariosViewModel`** (Seção B) — pipeline
   declarativo `asSequence → filter → filter → sortedBy → map → toList`
   sobre `MutableList<Usuario>`, expondo `List<String>` imutável.
3. **Camada de apresentação — `ContatosFiltro`** (Seção C, exemplo 1) —
   `filter` para renderizar apenas usuários ativos em `RecyclerView`.
4. **Projeção para UI — `sugestoesParaAutoComplete`** (Seção C, exemplo 2) —
   `map` projetando `List<Usuario>` em `List<String>` para um
   `AutoCompleteTextView`.
5. **Indexação — `usuariosAgrupadosPorInicial`** (Seção C, bônus) —
   `groupBy` indexando usuários por letra inicial, padrão usado em agendas
   nativas (iOS/Android).

Os snippets são intencionalmente isolados da resposta textual para que
possam ser revisados, executados em um `Scratch.kt` do Android Studio ou
copiados para um projeto Android real sem dependências extras.

---

## Estrutura do repositório

```
.
├── README.md                          # Este arquivo
├── mapa-resposta-revisada.md          # Resposta dissertativa (Markdown, fonte da verdade)
├── snippets-kotlin.kt                 # Snippets Kotlin compiláveis usados na resposta
├── pom.xml                            # Build Maven do protótipo desktop (Etapa 2)
├── src/
│   ├── main/kotlin/br/com/unicesumar/mapa/desktop/
│   │   ├── domain/Usuario.kt          # data class (camada de domínio)
│   │   ├── data/RepositorioEmMemoria.kt # CRUD + pipelines (camada de dados)
│   │   └── ui/UsuariosApp.kt          # Swing UI mobile-style (camada de apresentação)
│   └── test/kotlin/br/com/unicesumar/mapa/desktop/
│       ├── domain/UsuarioTest.kt       # JUnit 5: igualdade, copy, componentN
│       └── data/RepositorioEmMemoriaTest.kt # JUnit 5: CRUD + filter/sortedBy/groupBy
└── .gitignore                         # Exclusões de versionamento
```

---

## Convenção de pastas

| Caminho                       | Propósito                                                       |
|-------------------------------|-----------------------------------------------------------------|
| `README.md`                   | Apresentação do projeto e arquitetura                           |
| `mapa-resposta-revisada.md`   | Resposta final, fonte de verdade editável (diff textual limpo)  |
| `snippets-kotlin.kt`          | Camadas de domínio / ViewModel / apresentação como evidência    |
| `pom.xml`                     | Build Maven do protótipo desktop (Etapa 2)                      |
| `src/main/kotlin/...`         | Fontes Kotlin do protótipo (domain / data / ui)                 |
| `src/test/kotlin/...`         | Testes JUnit 5 cobrindo CRUD + pipelines                        |
| `.gitignore`                  | Exclui artefatos binários e estado interno do assistente        |

A camada de **entrega oficial** (documento Word formatado conforme template
da disciplina) é gerada a partir de `mapa-resposta-revisada.md` e ajustada
manualmente conforme exigido pelo AVA — não é versionada por ser binária
e sujeita a ajustes de formatação específicos do template.

---

## Como o conteúdo foi organizado

O trabalho adota a mesma separação de responsabilidades usada em projetos
Android de produção, espelhando a organização do projeto de referência
`logistica-reversa-ia-unicesumar-mapa`:

- **Camada conceitual (dissertação)** — `mapa-resposta-revisada.md`
  descreve decisões de engenharia (trade-off imutável vs mutável, default
  seguro, pipeline declarativo, complexidade algorítmica).
- **Camada de evidência técnica (código)** — `snippets-kotlin.kt`
  materializa cada decisão em código compilável, organizado por camada
  arquitetural (domínio → ViewModel → apresentação).
- **Camada de entrega oficial (binário)** — documento Word final,
  ajustado manualmente conforme template da disciplina (Arial 12,
  espaçamento 1,5, justificado, ABNT).

Essa organização permite:

- **Diff limpo** entre revisões (Markdown textual em vez de binário).
- **Reaproveitamento** dos snippets em outros trabalhos ou projetos
  Android reais.
- **Separação clara** entre o que é entrega oficial (binária) e o que é
  fonte de verdade editável (texto puro + código).
- **Rastreabilidade** entre cada afirmação da dissertação e o snippet
  que a sustenta (mapeamento na tabela *Mapeamento camada → questão*).

---

## Referências bibliográficas

- KOTLIN DOCUMENTATION. *Collections overview*. JetBrains, 2024.
  Disponível em: <https://kotlinlang.org/docs/collections-overview.html>.
- SENNE, E. A. **Programação para dispositivos móveis**. Florianópolis:
  Arqué, 2025.

---

## Como executar o protótipo desktop

```bash
# Build (gera fat jar com Kotlin stdlib embutido)
mvn -DskipTests package

# Executar
java -jar target/usuarios-desktop-1.0.0-all.jar
```

A janela abre com 360x720 px fixos (visual mobile), seed de 6 usuários
pré-carregada, e botões Listar / Ativos / Buscar / Agrupar / Resetar + FAB
rosa de Incluir. As mutações valem apenas em runtime.

## Como rodar os testes

```bash
mvn test
```

Suíte JUnit 5 (15+ casos) cobrindo:

- **Igualdade estrutural** da `data class Usuario`.
- **Pipeline funcional**: `filter`, `asSequence → sortedBy → toList`, `groupBy`.
- **CRUD**: `incluir` (com geração automática de ID), `excluir` (por id),
  `resetar` (volta ao estado do seed).
- **Imutabilidade da fronteira**: `listarTodos()` devolve cópia desacoplada.