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
- [Download do protótipo compilado](#download-do-protótipo-compilado)
- [Como executar o protótipo desktop](#como-executar-o-protótipo-desktop)
- [Como rodar os testes](#como-rodar-os-testes)
- [Como gerar uma nova versão (release)](#como-gerar-uma-nova-versão-release)
- [Inicializador automático](#inicializador-automático)
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

## Download do protótipo compilado

O executável pronto (fat jar com a Kotlin stdlib embutida, sem
dependências externas) é publicado em **GitHub Releases** a cada tag
`v*`. Para baixar a versão mais recente:

```
https://github.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/releases/latest
```

O asset anexado é `usuarios-desktop-1.0.0-all.jar` (~1,8 MB). Após o
download, basta rodar `java -jar` apontando para o arquivo (próxima
seção).

### Pré-requisito: Java (JRE/JDK)

| Item                | Versão                    |
|---------------------|---------------------------|
| Mínima              | **Java 11** (LTS)         |
| Recomendada         | **Java 11** (LTS)         |
| Testada em build    | Temurin JDK 11            |
| Fornecedor sugerido | Eclipse Temurin / Adoptium|

A porta de entrada do jar é `Main-Class: br.com.unicesumar.mapa.desktop.ui.UsuariosApp`,
declarada no `MANIFEST.MF`. Em sistemas sem Java, o comando abaixo não
será reconhecido — instale o JRE antes de prosseguir.

#### Como verificar se o Java já está instalado

```bash
# Linux / macOS (bash / zsh)
java -version
```

```powershell
# Windows (PowerShell)
java -version
```

Saída esperada (exemplo):

```
openjdk version "11.0.25" 2024-04-16
OpenJDK Runtime Environment Temurin-11.0.25+9 (build 11.0.25+9)
OpenJDK 64-Bit Server VM Temurin-11.0.25+9 (build 11.0.25+9, mixed mode)
```

#### Onde baixar o Java

| Sistema     | Distribuição recomendada          | Link de download                                                                                              |
|-------------|-----------------------------------|---------------------------------------------------------------------------------------------------------------|
| Windows     | Eclipse Temurin 11 (LTS) `.msi`   | <https://adoptium.net/temurin/releases/?version=11&os=windows&arch=x64&package=jdk>                          |
| Windows     | Eclipse Temurin 11 (LTS) `.zip`   | <https://adoptium.net/temurin/releases/?version=11&os=windows&arch=x64&package=jdk&jvmVariant=hotspot#x64>    |
| Linux       | Eclipse Temurin 11 (LTS) `.tar.gz`| <https://adoptium.net/temurin/releases/?version=11&os=linux&arch=x64&package=jdk>                             |
| macOS       | Eclipse Temurin 11 (LTS) `.pkg`   | <https://adoptium.net/temurin/releases/?version=11&os=mac&arch=x64&package=jdk>                              |
| Qualquer SO | Página geral do Adoptium          | <https://adoptium.net/>                                                                                       |

> Quem já tem **Oracle JDK**, **Azul Zulu**, **Amazon Corretto** ou
> **Microsoft Build of OpenJDK** em versão 11+ também funciona — qualquer
> build *Java SE 11 LTS* ou superior serve. O jar não depende de uma
> distribuição específica, apenas do contrato da plataforma Java SE.

---

## Como executar o protótipo desktop

### Opção A — a partir do `.jar` baixado (recomendado)

Independente do sistema operacional, o comando é o mesmo: `java -jar`.

**Windows (PowerShell):**

```powershell
# 1. Baixar o jar da release mais recente
Invoke-WebRequest -Uri "https://github.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/releases/latest/download/usuarios-desktop-1.0.0-all.jar" -OutFile "usuarios-desktop-1.0.0-all.jar"

# 2. Executar
java -jar .\usuarios-desktop-1.0.0-all.jar
```

> Equivalente em cmd.exe: `curl -L -o usuarios-desktop-1.0.0-all.jar https://github.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/releases/latest/download/usuarios-desktop-1.0.0-all.jar`

**Linux / macOS (bash / zsh):**

```bash
# 1. Baixar o jar da release mais recente
curl -L -o usuarios-desktop-1.0.0-all.jar \
  https://github.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/releases/latest/download/usuarios-desktop-1.0.0-all.jar

# 2. Executar
java -jar ./usuarios-desktop-1.0.0-all.jar
```

> Se preferir `wget`: `wget -O usuarios-desktop-1.0.0-all.jar https://github.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/releases/latest/download/usuarios-desktop-1.0.0-all.jar`

### Opção B — gerando o `.jar` localmente a partir do código-fonte

Requer **JDK 11** (LTS) e **Maven 3.8+** instalados e no
`PATH`.

```bash
# Clonar e entrar no repositório
git clone https://github.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa.git
cd programacao-dispositivos-moveis-unicesumar-mapa

# Build (gera fat jar com Kotlin stdlib embutido)
mvn -DskipTests package

# Executar
java -jar target/usuarios-desktop-1.0.0-all.jar
```

No Windows (PowerShell), basta trocar `java -jar target/...` por
`java -jar .\target\usuarios-desktop-1.0.0-all.jar` (o resto é
idêntico).

### O que esperar ao abrir

A janela Swing abre com **360x720 px fixos** (visual mobile), seed de
**6 usuários** pré-carregada, e botões **Listar / Ativos / Buscar /
Agrupar / Resetar** + **FAB rosa de Incluir** no canto inferior direito.
As mutações valem apenas em runtime — fechar o `.jar` descarta as
alterações e a próxima abertura volta à seed.

---

## Como rodar os testes

```bash
mvn test
```

Suíte JUnit 5 (18 casos) cobrindo:

- **Igualdade estrutural** da `data class Usuario`.
- **Pipeline funcional**: `filter`, `asSequence → sortedBy → toList`, `groupBy`.
- **CRUD**: `incluir` (com geração automática de ID), `excluir` (por id),
  `resetar` (volta ao estado do seed).
- **Imutabilidade da fronteira**: `listarTodos()` devolve cópia desacoplada.

Saída esperada (resumo): `Tests run: 18, Failures: 0, Errors: 0,
Skipped: 0` e `BUILD SUCCESS`.

---

## Como gerar uma nova versão (release)

O workflow `.github/workflows/release.yml` publica automaticamente uma
GitHub Release com o fat jar sempre que uma tag `v*` é enviada ao
repositório.

```bash
# 1. Garantir que main está atualizado e limpo
git checkout main
git pull

# 2. Criar tag semântica (MAJOR.MINOR.PATCH)
git tag v1.0.0

# 3. Enviar a tag para o GitHub (dispara o workflow)
git push origin v1.0.0
```

Acompanhe a execução em `Actions → release`. Ao final, a release
estará visível em `https://github.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/releases/tag/v1.0.0`
com o jar anexado para download. Também é possível disparar o
workflow manualmente em `Actions → release → Run workflow` (útil para
republicar a mesma versão sem bump de tag).

---

## Inicializador automático

Se você **não quer instalar o Java manualmente nem digitar comandos
longos**, basta baixar um pequeno script na raiz do repositório e
clicar duas vezes (ou rodar no terminal). O script cuida de tudo:
localiza um JDK existente ou baixa o Eclipse Temurin 11 LTS
silenciosamente, baixa o `.jar` da release mais recente do GitHub
e executa a interface Swing.

### One-liner: baixar e rodar direto do terminal

Cole **uma** das linhas abaixo em um terminal e pressione Enter. O
script é baixado, tornado executável e iniciado em sequência — sem
clonar o repositório inteiro.

**Windows (PowerShell):**

```powershell
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/main/EXECUTAR.bat" -OutFile "EXECUTAR.bat"; .\EXECUTAR.bat
```

**Linux / macOS (bash / zsh):**

```bash
curl -fsSL https://raw.githubusercontent.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/main/EXECUTAR.sh -o EXECUTAR.sh && chmod +x EXECUTAR.sh && ./EXECUTAR.sh
```

> No macOS, se aparecer um aviso sobre não poder executar um
> arquivo baixado da Internet, use o Finder ou rode
> `xattr -d com.apple.quarantine EXECUTAR.sh` e tente de novo.

### Passo a passo alternativo (clicar duas vezes)

Se preferir baixar manualmente:

| SO      | Arquivo          | O que o script faz                                                                                              |
|---------|------------------|----------------------------------------------------------------------------------------------------------------|
| Windows | `EXECUTAR.bat`   | 1. Procura JDK 11+ em `C:\Program Files\Java\` ou `%JAVA_HOME%`. 2. Se não houver, baixa Temurin 11 (~170 MB) e extrai em `C:\Program Files\Java\jdk-11` (UAC se necessário). 3. Cria `C:\MAPA\` e baixa o `.jar` do GitHub Releases. 4. Executa `java -jar C:\MAPA\usuarios-desktop-1.0.0-all.jar`. |
| Linux   | `EXECUTAR.sh`    | 1. Procura JDK 11+ no PATH, `JAVA_HOME`, `/usr/lib/jvm/`. 2. Se não houver, baixa Temurin 11 (~170 MB) e extrai em `$HOME/.local/share/mapa-java/jdk-11` (sem sudo). 3. Cria `~/MAPA/` e baixa o `.jar` do GitHub Releases. 4. Executa `java -jar ~/MAPA/usuarios-desktop-1.0.0-all.jar`. |
| macOS   | `EXECUTAR.sh`    | 1. Procura JDK 11+ no PATH, `JAVA_HOME`, Homebrew, `/Library/Java/JavaVirtualMachines/`. 2. Se não houver, baixa Temurin 11 (~170 MB) e extrai em `$HOME/Library/Application Support/mapa-java/jdk-11` (sem sudo). 3. Cria `~/MAPA/` e baixa o `.jar` do GitHub Releases. 4. Executa `java -jar ~/MAPA/usuarios-desktop-1.0.0-all.jar`. |

**Baixar manualmente:**

```bash
# Windows (PowerShell)
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/main/EXECUTAR.bat" -OutFile "EXECUTAR.bat"
```

```bash
# Linux / macOS (bash / zsh)
curl -fsSL https://raw.githubusercontent.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/main/EXECUTAR.sh -o EXECUTAR.sh
chmod +x EXECUTAR.sh
```

Depois é só **clicar duas vezes** (Windows) ou rodar `./EXECUTAR.sh`
no terminal (Linux/macOS).

> Na primeira execução o script baixará o Java e o `.jar`
> (~170 MB + ~2 MB). Execuções seguintes reutilizam o que já está
> em disco.