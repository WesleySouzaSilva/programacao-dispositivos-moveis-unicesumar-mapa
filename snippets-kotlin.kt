// snippets-kotlin.kt
// Blocos de código referenciados em mapa-resposta-revisada.md (MAPA — Programação para Dispositivos Móveis).
// Compilável com Kotlin 1.9+ / AndroidX Lifecycle 2.7+.

package br.com.unicesumar.mapa.snippets

import androidx.lifecycle.ViewModel

// -----------------------------------------------------------------------------
// Modelo de domínio usado nos snippets B e C
// -----------------------------------------------------------------------------
data class Usuario(
    val nome: String,
    val email: String,
    val ativo: Boolean
)

// =============================================================================
// Seção B — Manipulação de lista de nomes de usuários
// =============================================================================

/**
 * ViewModel Android demonstrando pipeline declarativo:
 *   carrega lista mutável (camada de dados) -> expõe lista imutável (camada de UI).
 *
 * Operações encadeadas:
 *   - asSequence(): ativa avaliação lazy para listas grandes
 *   - filter { it.ativo }: apenas usuários ativos
 *   - filter { startsWith(prefixo) }: busca por prefixo (case-insensitive)
 *   - sortedBy { it.nome }: ordenação alfabética
 *   - map { it.nome }: projeção para a UI
 *   - toList(): materializa o resultado
 */
class UsuariosViewModel : ViewModel() {

    private val _usuarios = mutableListOf(
        Usuario("Ana Lima", "ana@exemplo.com", true),
        Usuario("Bruno Sá", "bruno@exemplo.com", false),
        Usuario("Carla Mota", "carla@exemplo.com", true)
    )

    /**
     * Retorna nomes ordenados, filtrados por status ativo e prefixo opcional.
     * Complexidade: O(n log n) devido ao sortedBy.
     */
    fun nomesOrdenadosAtivos(prefixo: String = ""): List<String> =
        _usuarios
            .asSequence()
            .filter { it.ativo }
            .filter { it.nome.startsWith(prefixo, ignoreCase = true) }
            .sortedBy { it.nome }
            .map { it.nome }
            .toList()
}

// =============================================================================
// Seção C — Operações funcionais em collections
// =============================================================================

/**
 * Exemplo 1 — filter para renderizar apenas usuários ativos em um RecyclerView.
 *
 * Padrão: filtra-se uma vez no construtor do adapter e trabalha-se apenas
 * com a sublista resultante. `filter` retorna nova List (imutável por padrão),
 * eliminando mutação acidental durante o bind do ViewHolder.
 */
class ContatosFiltro(private val todos: List<Usuario>) {

    val ativos: List<Usuario> = todos.filter { it.ativo }

    val inativos: List<Usuario> = todos.filterNot { it.ativo }

    val total: Int = ativos.size
}

/**
 * Exemplo 2 — map para projetar usuários em rótulos de UI (DTO de apresentação).
 *
 * Padrão: desacopla modelo de domínio da camada de apresentação. O widget
 * de busca recebe apenas String, não conhece a estrutura de Usuario.
 */
fun sugestoesParaAutoComplete(usuarios: List<Usuario>): List<String> =
    usuarios.map { "${it.nome} <${it.email}>" }

/**
 * Exemplo bônus — groupBy para indexar usuários por inicial (estilo agenda iOS/Android).
 *
 * Padrão: gera um Map<Char, List<Usuario>> pronto para renderização com seções A, B, C...
 */
fun usuariosAgrupadosPorInicial(usuarios: List<Usuario>): Map<Char, List<Usuario>> =
    usuarios.groupBy { it.nome.first().uppercaseChar() }