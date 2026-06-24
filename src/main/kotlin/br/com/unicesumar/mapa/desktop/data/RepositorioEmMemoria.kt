package br.com.unicesumar.mapa.desktop.data

import br.com.unicesumar.mapa.desktop.domain.Usuario

/**
 * Repositório em memória.
 *
 * - A lista inicial (seed) é fixa e declarada em [seed]: garante o
 *   comportamento exigido pela Etapa 2 — ao fechar e abrir o app,
 *   as inclusões e exclusões somem, e os dados voltam ao estado de origem.
 * - A lista de trabalho é uma [MutableList] interna; o que sai para a UI
 *   e para os serviços é sempre uma [List] imutável (princípio do default
 *   imutável).
 */
class RepositorioEmMemoria {

    private val seed: List<Usuario> = listOf(
        Usuario(1, "Ana Lima",      "ana.lima@empresa.com.br",   "Engenharia",  true),
        Usuario(2, "Bruno Sá",      "bruno.sa@empresa.com.br",   "Engenharia",  false),
        Usuario(3, "Carla Mota",    "carla.mota@empresa.com.br", "Operações",   true),
        Usuario(4, "Diego Pires",   "diego.pires@empresa.com.br","Comercial",   true),
        Usuario(5, "Elena Rocha",   "elena.rocha@empresa.com.br","Comercial",   false),
        Usuario(6, "Felipe Tanaka", "felipe.tanaka@empresa.com.br","Engenharia", true)
    )

    private val usuarios: MutableList<Usuario> = seed.toMutableList()

    /** Sequência monotônica usada para gerar IDs a cada inclusão. */
    private var proximoId: Long = (usuarios.maxOf { it.id } + 1)

    // ---------- Leituras (sempre devolvem List imutável) ----------

    /** Lista completa na ordem atual de inserção. */
    fun listarTodos(): List<Usuario> = usuarios.toList()

    /** Apenas usuários ativos — exemplo direto do uso de filter. */
    fun listarAtivos(): List<Usuario> = usuarios.filter { it.ativo }

    /**
     * Busca textual case-insensitive por prefixo de nome.
     * Pipeline: asSequence → filter → sortedBy → map → toList.
     */
    fun buscarPorPrefixo(prefixo: String): List<Usuario> =
        usuarios.asSequence()
            .filter { it.ativo }
            .filter { it.nome.startsWith(prefixo, ignoreCase = true) }
            .sortedBy { it.nome }
            .toList()

    /**
     * Indexação por inicial — exemplo do uso de groupBy.
     * Útil para renderizar seções A, B, C... como em agendas nativas.
     */
    fun agruparPorInicial(): Map<Char, List<Usuario>> =
        usuarios.groupBy { it.nome.first().uppercaseChar() }

    fun tamanho(): Int = usuarios.size

    // ---------- Mutações (apenas em runtime) ----------

    /** Inclui um novo usuário; gera ID automaticamente. */
    fun incluir(nome: String, email: String, setor: String, ativo: Boolean): Usuario {
        val novo = Usuario(proximoId++, nome.trim(), email.trim(), setor.trim(), ativo)
        usuarios.add(novo)
        return novo
    }

    /** Remove o usuário com o ID informado. Retorna true se removeu. */
    fun excluir(id: Long): Boolean =
        usuarios.removeIf { it.id == id }

    /** Limpa todos os usuários inseridos em runtime, voltando ao estado de seed. */
    fun resetar(): Unit {
        usuarios.clear()
        usuarios.addAll(seed)
        proximoId = (usuarios.maxOf { it.id } + 1)
    }
}
