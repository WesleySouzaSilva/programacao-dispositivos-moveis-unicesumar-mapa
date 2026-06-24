package br.com.unicesumar.mapa.desktop.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Testes da data class [Usuario].
 *
 * Verificações cobertas:
 *  - Igualdade estrutural (gerada automaticamente pelo compilador Kotlin);
 *  - Função [copy] produz nova instância imutável;
 *  - [componentN] preserva a ordem dos campos declarada na assinatura.
 */
class UsuarioTest {

    @Test
    fun `data class gera igualdade estrutural por valor`() {
        val a = Usuario(1, "Ana Lima", "ana@empresa.com", "Engenharia", true)
        val b = Usuario(1, "Ana Lima", "ana@empresa.com", "Engenharia", true)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `data class considera diferente quando algum campo difere`() {
        val a = Usuario(1, "Ana Lima", "ana@empresa.com", "Engenharia", true)
        val b = a.copy(ativo = false)

        assertNotEquals(a, b)
    }

    @Test
    fun `copy retorna nova instancia sem mutar a original`() {
        val original = Usuario(1, "Ana", "ana@x.com", "Engenharia", true)
        val copia = original.copy(setor = "Operações")

        assertTrue(original.setor == "Engenharia", "original não pode ser mutado")
        assertEquals("Operações", copia.setor)
    }

    @Test
    fun `componentN preserva a ordem dos campos do construtor`() {
        val u = Usuario(7, "Carla", "carla@x.com", "Operações", true)
        val (id, nome, email, setor, ativo) = u

        assertEquals(7L, id)
        assertEquals("Carla", nome)
        assertEquals("carla@x.com", email)
        assertEquals("Operações", setor)
        assertEquals(true, ativo)
    }
}
