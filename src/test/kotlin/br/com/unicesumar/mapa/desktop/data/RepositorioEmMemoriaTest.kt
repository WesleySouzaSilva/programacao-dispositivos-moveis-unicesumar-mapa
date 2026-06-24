package br.com.unicesumar.mapa.desktop.data

import br.com.unicesumar.mapa.desktop.domain.Usuario
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Suíte de testes do [RepositorioEmMemoria].
 *
 * Cada teste opera sobre uma instância nova (criada em [BeforeEach]) para
 * garantir isolamento — o repositório mantém estado mutável em runtime,
 * portanto qualquer alteração em um teste poderia contaminar o seguinte.
 *
 * Os testes cobrem o pipeline funcional exigido pelo MAPA:
 *   - [listarAtivos]        → filter
 *   - [buscarPorPrefixo]    → asSequence → filter → sortedBy → toList
 *   - [agruparPorInicial]   → groupBy
 *
 * e o CRUD básico (incluir / excluir / resetar) exigido pela Etapa 2.
 */
class RepositorioEmMemoriaTest {

    private lateinit var repo: RepositorioEmMemoria

    @BeforeEach
    fun setUp() {
        repo = RepositorioEmMemoria()
    }

    // ---------- Seed (estado inicial) ----------

    @Test
    fun `seed contem seis usuarios conforme declarado`() {
        assertEquals(6, repo.listarTodos().size)
        assertEquals(6, repo.tamanho())
    }

    @Test
    fun `seed possui mistura de usuarios ativos e inativos`() {
        val ativos = repo.listarAtivos()
        val todos = repo.listarTodos()

        assertTrue(ativos.isNotEmpty(), "deve haver ao menos um ativo no seed")
        assertTrue(ativos.size < todos.size, "deve haver tambem inativos no seed")
    }

    // ---------- Leituras: imutabilidade ----------

    @Test
    fun `listarTodos retorna copia desacoplada do estado interno`() {
        val leitura: List<Usuario> = repo.listarTodos()
        // mutar a lista devolvida não pode afetar o repositório —
        // `toList()` produz uma cópia defensiva.
        val copiaMutavel = leitura.toMutableList()
        copiaMutavel.add(Usuario(99, "Hacker", "h@x.com", "Invasão", true))

        assertEquals(6, repo.tamanho(), "o repositório não pode ter sido mutado")
        assertEquals(7, copiaMutavel.size)
    }

    // ---------- Leituras: pipeline filter / sortedBy / groupBy ----------

    @Test
    fun `listarAtivos aplica filter e retorna apenas usuarios com ativo=true`() {
        val ativos = repo.listarAtivos()

        assertTrue(ativos.isNotEmpty())
        assertTrue(ativos.all { it.ativo }, "nenhum inativo pode aparecer")
    }

    @Test
    fun `buscarPorPrefixo e case-insensitive e devolve apenas ativos ordenados`() {
        val encontrados = repo.buscarPorPrefixo("aN")

        assertTrue(encontrados.isNotEmpty())
        // deve estar ordenado alfabeticamente por nome
        val nomes = encontrados.map { it.nome }
        assertEquals(nomes.sorted(), nomes, "resultado deve estar ordenado por nome")
        // todos devem ser ativos (o filtro de ativos está embutido)
        assertTrue(encontrados.all { it.ativo })
    }

    @Test
    fun `buscarPorPrefixo com string vazia retorna seed de ativos ordenado`() {
        val vazio = repo.buscarPorPrefixo("")
        val ativosOrdenados = repo.listarAtivos().sortedBy { it.nome }

        assertEquals(ativosOrdenados.map { it.id }, vazio.map { it.id })
    }

    @Test
    fun `buscarPorPrefixo sem matches retorna lista vazia`() {
        val resultado = repo.buscarPorPrefixo("ZZZZ")
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `agruparPorInicial retorna Map cuja chave e a inicial em maiusculo`() {
        val mapa = repo.agruparPorInicial()

        assertTrue(mapa.isNotEmpty())
        mapa.forEach { (letra, grupo) ->
            assertTrue(letra.isUpperCase(), "chave '$letra' deve estar em maiúsculo")
            assertTrue(grupo.all { it.nome.first().uppercaseChar() == letra })
        }
    }

    // ---------- Mutações: incluir ----------

    @Test
    fun `incluir gera id monotonicamente crescente e adiciona ao final`() {
        val maiorIdAntes = repo.listarTodos().maxOf { it.id }
        val novo = repo.incluir("Zélia Prado", "zelia@empresa.com", "RH", true)

        assertTrue(novo.id > maiorIdAntes, "novo id deve ser maior que o maior existente")
        assertEquals(7, repo.tamanho())
        assertEquals(novo, repo.listarTodos().last())
    }

    @Test
    fun `incluir com ativo false aparece em listarTodos mas nao em listarAtivos`() {
        val novo = repo.incluir("Inativo X", "x@empresa.com", "Geral", ativo = false)

        assertTrue(repo.listarTodos().contains(novo))
        assertFalse(repo.listarAtivos().contains(novo))
    }

    // ---------- Mutações: excluir ----------

    @Test
    fun `excluir remove usuario pelo id e retorna true`() {
        val alvo = repo.listarTodos().first()
        val removido = repo.excluir(alvo.id)

        assertTrue(removido)
        assertEquals(5, repo.tamanho())
        assertFalse(repo.listarTodos().any { it.id == alvo.id })
    }

    @Test
    fun `excluir id inexistente retorna false e nao altera o tamanho`() {
        val removido = repo.excluir(999_999L)

        assertFalse(removido)
        assertEquals(6, repo.tamanho())
    }

    // ---------- Mutações: resetar ----------

    @Test
    fun `resetar volta ao estado original do seed e descarta inclusoes`() {
        repo.incluir("Temp", "temp@x.com", "X", true)
        assertEquals(7, repo.tamanho())

        repo.resetar()

        assertEquals(6, repo.tamanho())
        assertEquals(setOf("Ana Lima", "Bruno Sá", "Carla Mota"),
            repo.listarTodos().take(3).map { it.nome }.toSet())
    }

    @Test
    fun `resetar descarta exclusoes e restaura usuarios removidos`() {
        val alvo = repo.listarTodos().first()
        repo.excluir(alvo.id)
        assertEquals(5, repo.tamanho())

        repo.resetar()

        assertEquals(6, repo.tamanho())
        assertTrue(repo.listarTodos().any { it.id == alvo.id })
    }
}
