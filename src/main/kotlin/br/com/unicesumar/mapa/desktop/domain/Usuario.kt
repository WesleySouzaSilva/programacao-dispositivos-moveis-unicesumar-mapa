package br.com.unicesumar.mapa.desktop.domain

/**
 * Modelo de domínio imutável (data class) — espelha a entidade de domínio
 * declarada em snippets-kotlin.kt. Toda mutação ocorre por cópia, nunca por
 * alteração in-place, preservando o princípio de menor privilégio aplicado
 * na resposta do MAPA.
 */
data class Usuario(
    val id: Long,
    val nome: String,
    val email: String,
    val setor: String,
    val ativo: Boolean
)
