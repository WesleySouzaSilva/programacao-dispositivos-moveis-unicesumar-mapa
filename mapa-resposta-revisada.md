# MAPA – Programação para Dispositivos Móveis

**Acadêmico:** Wesley Souza da Silva — R.A. 23031815-5
**Curso:** Engenharia de Software
**Disciplina:** Programação para Dispositivos Móveis
**Professor Mediador:** —
**Prazo:** 05/07/2026

---

## Introdução

O presente MAPA tem como objetivo responder, de forma técnica e fundamentada, às questões A, B e C propostas na atividade, situando-as no contexto de um projeto real: o desenvolvimento de um aplicativo Android em Kotlin para uma empresa que precisa manipular grandes volumes de dados de usuários e serviços. A escolha adequada das estruturas de dados — arrays ou collections — e o uso consciente de operações funcionais impactam diretamente a legibilidade, a manutenibilidade e a performance do software entregue (SENNE, 2025).

Em engenharia de software, a decisão sobre **como** armazenar e percorrer dados não é estética: trata-se de um trade-off entre segurança de tipos, custo de memória, custo de processamento e clareza para o time que dará manutenção ao código. A linguagem Kotlin, por design, incentiva o uso de collections em sua Standard Library, oferecendo abstrações idiomáticas — `List`, `MutableList`, `Set`, `Map` e suas variantes mutáveis — acompanhadas de um rico conjunto de operações funcionais (`filter`, `map`, `forEach`, `reduce`, `groupBy`, entre outras) que substituem loops manuais por expressões declarativas (KOTLIN DOCUMENTATION, 2024).

A tese deste trabalho é que, no cenário descrito pela atividade, **collections imutáveis com operações funcionais são o padrão recomendado**, reservando-se arrays e mutabilidade explícita para situações bem delimitadas em que oferecem ganho mensurável.

---

## A) Diferença entre arrays e collections em Kotlin

Em Kotlin, **arrays** (`Array<T>`, `IntArray`, `ByteArray`, etc.) são estruturas de tamanho fixo, alocadas em sequência contínua de memória e indexadas numericamente a partir de zero. Uma vez instanciado, o número de elementos não pode ser alterado — qualquer operação de "adição" cria, na verdade, um novo array. Arrays expõem, ainda, interoperabilidade direta com a JVM e com APIs de plataforma Android que exigem buffers de tamanho conhecido, como `ByteArray` para streams ou `IntArray` para coordenadas de tela.

**Collections**, por outro lado, são abstrações da Kotlin Standard Library construídas sobre interfaces (`Collection`, `List`, `Set`, `Map`) com implementações concretas (`ArrayList`, `LinkedList`, `HashSet`, `HashMap`, `TreeMap`). Collections podem ser **imutáveis** (`List`, `Set`, `Map`) ou **mutáveis** (`MutableList`, `MutableSet`, `MutableMap`), e oferecem operações funcionais de ordem superior que arrays não disponibilizam nativamente.

A Tabela 1 resume o comparativo técnico.

**Tabela 1 — Arrays vs Collections em Kotlin**

| Critério              | Array                          | Collection (List/Set/Map)                |
|-----------------------|--------------------------------|------------------------------------------|
| Tamanho               | Fixo após instanciação         | Dinâmico (cresce/encolhe)                |
| Mutabilidade          | Elementos mutáveis, tamanho fixo | Imutável por padrão; mutável se explícito |
| Sintaxe de criação    | `arrayOf(1, 2, 3)` / `IntArray(3)` | `listOf(1, 2, 3)` / `mutableListOf(1)` |
| Operações funcionais  | Não nativas (requer loop)      | Nativas: `filter`, `map`, `forEach`, `reduce` |
| Interoperabilidade    | Direta com JVM e Android SDK   | Requer conversão (`toList`, `toTypedArray`) |
| Caso de uso típico    | Buffers, coordenadas, tamanhos fixos | Listas de domínio, catálogos, agendas |

**Duas vantagens do uso de collections em um projeto real:**

1. **Flexibilidade estrutural sem realocação manual.** Em um aplicativo de catálogo de produtos, a coleção de itens chega dinamicamente via API paginada. Com `MutableList<Produto>`, o time adiciona, remove ou reordena itens sem se preocupar com realocação de memória, exatamente o que o cenário do enunciado exige para "listas de usuários e serviços".

2. **Operações funcionais de ordem superior integradas.** Métodos como `filter`, `map`, `groupBy` e `associate` permitem transformar coleções em pipelines declarativos. Isso reduz o tamanho do código, elimina variáveis temporárias e diminui a superfície de bugs — um ganho direto em **manutenibilidade** do software.

Vale registrar que arrays ainda são preferíveis em cenários específicos: buffers de tamanho fixo, interoperabilidade com C/JNI, hot paths onde o overhead de `List` é mensurável, ou quando o domínio impõe cardinalidade invariante. A regra de engenharia é: **default em collection, array apenas quando há motivo claro**.

---

## B) Manipulação de uma lista de nomes de usuários

Para armazenar nomes de usuários em um aplicativo Android, a Kotlin Standard Library oferece duas estruturas adequadas, com perfis de uso distintos.

A estrutura padrão — e recomendada como ponto de partida — é **`List<String>`** (imutável). Ela é a escolha segura porque, no contexto de UI Android, a lista de usuários normalmente é carregada de uma fonte (banco de dados Room, API REST, DataStore) e renderizada em um `RecyclerView`. Imutabilidade aqui protege contra mutações concorrentes vindas de threads de background e expressa a intenção de que aquela coleção não deve ser alterada in-place.

Quando há **CRUD local** real (cadastrar novo usuário, remover um contato, editar nome), promove-se a coleção para **`MutableList<String>`**, que expõe `add`, `removeAt`, `clear`, `addAll` e operadores de escrita. Mesmo nesse caso, é boa prática manter a fronteira bem definida: coleções mutáveis ficam confinadas a uma camada de repositório ou ViewModel, e o que sai para a UI é sempre uma `List` imutável.

**Duas operações comuns aplicáveis para tornar a lista mais útil no contexto do aplicativo:**

1. **Ordenação com `sortedBy`.** Em uma agenda de contatos, exibir os nomes em ordem alfabética é requisito clássico de UX. A operação `usuarios.sortedBy { it.nome }` retorna uma nova lista (sem mutar a original) já ordenada pelo critério informado. A complexidade é O(n log n), equivalente a `Collections.sort`, com a vantagem de ser uma operação de expressão — não um comando procedural.

2. **Filtragem com `filter`.** Em qualquer tela com busca, é necessário restringir a lista a um subconjunto. `usuarios.filter { it.ativo }` retorna apenas usuários ativos, e `usuarios.filter { it.nome.startsWith(prefixo, ignoreCase = true) }` implementa um autocompletar de busca. A operação é *lazy* quando combinada com `asSequence()`, evitando iterações intermediárias sobre listas muito grandes.

O snippet a seguir ilustra o uso conjunto das duas operações em um ViewModel Android:

```kotlin
data class Usuario(val nome: String, val ativo: Boolean)

class UsuariosViewModel : ViewModel() {

    private val _usuarios = mutableListOf(
        Usuario("Ana Lima", true),
        Usuario("Bruno Sá", false),
        Usuario("Carla Mota", true)
    )

    fun nomesOrdenadosAtivos(prefixo: String = ""): List<String> =
        _usuarios
            .asSequence()
            .filter { it.ativo }
            .filter { it.nome.startsWith(prefixo, ignoreCase = true) }
            .sortedBy { it.nome }
            .map { it.nome }
            .toList()
}
```

Esse pipeline é **declarativo**: lê-se de cima para baixo como uma sequência de transformações, sem loops visíveis, sem variáveis intermediárias. É a mesma forma de pensar que engenheiros aplicam em `Stream` no Java ou em LINQ no C# — só que idiomática em Kotlin.

---

## C) Operações funcionais em collections

Operações funcionais são funções de ordem superior — funções que recebem outras funções como parâmetro ou as retornam — aplicadas sobre collections para transformá-las de forma **declarativa**. Em vez de descrever *como* iterar (imperativo: `for`, `while`, índice), o código descreve *o que* se quer obter (declarativo: `filter`, `map`, `reduce`). O controle da iteração fica a cargo da Standard Library, que pode aplicar otimizações como *lazy evaluation* via `Sequence`.

Do ponto de vista de engenharia, três benefícios são mensuráveis:

- **Redução de linhas de código:** um pipeline de 5 operações cabe em 5 linhas; o equivalente imperativo exige variáveis temporárias, controle de índice e bloco de loop — tipicamente 15-25 linhas.
- **Imutabilidade por padrão:** cada operação retorna uma nova coleção, eliminando efeitos colaterais e condições de corrida em ambientes concorrentes (essencial em Android com coroutines).
- **Facilidade de teste unitário:** como cada etapa é uma função pura, é trivial isolar e testar `filter` e `map` separadamente, sem mockar dependências externas.

**Dois exemplos práticos de uso em um aplicativo Android:**

**Exemplo 1 — `filter` para exibir apenas usuários ativos em uma lista de contatos.**
Em um `RecyclerView.Adapter`, antes de renderizar, filtra-se a lista original:

```kotlin
class ContatosAdapter(
    private val todos: List<Usuario>
) : RecyclerView.Adapter<ContatosAdapter.VH>() {

    private val ativos: List<Usuario> = todos.filter { it.ativo }

    override fun getItemCount(): Int = ativos.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(ativos[position])
    }
    // ...
}
```

O ganho de performance aparece quando a tela de contatos tem busca: combinar `filter` com `asSequence()` posterga a iteração até o momento da coleta, evitando alocações intermediárias em listas grandes (KOTLIN DOCUMENTATION, 2024).

**Exemplo 2 — `map` para projetar a lista de usuários em uma lista de rótulos para a UI.**
Em vez de expor o objeto de domínio inteiro para uma `AutoCompleteTextView`, mapeia-se para `String`:

```kotlin
val sugestoes: List<String> =
    usuarios.map { "${it.nome} <${it.email}>" }

val adapter = ArrayAdapter(
    requireContext(),
    android.R.layout.simple_dropdown_item_1line,
    sugestoes
)
campoBusca.setAdapter(adapter)
```

Esse padrão — **DTO de UI** derivado via `map` — desacopla o modelo de domínio da camada de apresentação, exatamente o que o princípio de *separação de responsabilidades* recomenda.

**Exemplo bônus — `groupBy` para indexar usuários por inicial.**
Útil em telas de agenda que mostram rolagens alfabéticas (como a do app Contatos do iOS/Android):

```kotlin
val porInicial: Map<Char, List<Usuario>> =
    usuarios.groupBy { it.nome.first().uppercaseChar() }

// Uso na UI:
porInicial['A']?.forEach { /* renderizar seção "A" */ }
```

Esses três exemplos cobrem o cenário descrito no enunciado — "lidando com grandes quantidades de dados de usuários e serviços" — e demonstram que operações funcionais não são apenas açúcar sintático: são ferramentas de arquitetura que produzem código menor, mais seguro e mais fácil de testar.

---

## Conclusão

A análise das questões A, B e C evidencia que, no desenvolvimento Android em Kotlin, **collections devem ser a estrutura padrão** para dados de domínio, com arrays reservados a cenários de tamanho fixo, interoperabilidade ou hot paths de performance. A escolha entre `List` (imutável) e `MutableList` (mutável) deve seguir o princípio da menor privilégio — começando-se imutável e promovendo-se a mutabilidade apenas onde há CRUD explícito. Por fim, operações funcionais (`filter`, `map`, `sortedBy`, `groupBy`, entre outras) são o mecanismo idiomático para transformar essas coleções de forma declarativa, concisa e thread-safe, constituindo a base de pipelines de dados em ViewModels, Repositories e Use Cases em projetos Android profissionais.

---

## Referências

KOTLIN DOCUMENTATION. *Collections overview*. JetBrains, 2024. Disponível em: https://kotlinlang.org/docs/collections-overview.html. Acesso em: 24 jun. 2026.

SENNE, E. A. **Programação para dispositivos móveis**. Florianópolis: Arqué, 2025.