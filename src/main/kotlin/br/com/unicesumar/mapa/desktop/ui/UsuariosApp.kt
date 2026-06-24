package br.com.unicesumar.mapa.desktop.ui

import br.com.unicesumar.mapa.desktop.data.RepositorioEmMemoria
import br.com.unicesumar.mapa.desktop.domain.Usuario
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractButton
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants
import javax.swing.border.EmptyBorder

/**
 * UI Swing que emula um app Android de gestão de usuários em viewport de
 * celular (360x720). A janela é fixa nessa proporção e usa paleta/cromia
 * de Material Design (app bar cor primária, surface branca, FAB colorido,
 * lista com avatar) para dar a sensação visual de uso em smartphone,
 * apesar de rodar como .jar no desktop.
 *
 * Conceitos do MAPA exercitados em runtime:
 *  - [MutableList] para CRUD local (RepositorioEmMemoria).
 *  - [List] imutável exposto para a UI.
 *  - filter / map / sortedBy / groupBy aplicados via [RepositorioEmMemoria].
 */
class UsuariosApp(private val repositorio: RepositorioEmMemoria = RepositorioEmMemoria()) {

    // --- Paleta "Material" para simular visual mobile ---
    private val corPrimaria      = Color(0xFF1E88E5.toInt())   // azul Material Blue 600
    private val corPrimariaDark  = Color(0xFF1565C0.toInt())   // azul 800 — AppBar gradient bottom
    private val corAccent        = Color(0xFFFF4081.toInt())   // rosa accent
    private val corSurface       = Color(0xFFFAFAFA.toInt())   // fundo "screen"
    private val corCard          = Color.WHITE
    private val corOnPrimary     = Color.WHITE
    private val corTextoPrimario = Color(0xFF212121.toInt())
    private val corTextoSecund   = Color(0xFF757575.toInt())
    private val corDivider       = Color(0xFFE0E0E0.toInt())
    private val corInativo       = Color(0xFFBDBDBD.toInt())
    private val corFundoAvatar   = listOf(
        Color(0xFF42A5F5.toInt()), Color(0xFF66BB6A.toInt()), Color(0xFFFFA726.toInt()),
        Color(0xFFAB47BC.toInt()), Color(0xFFEF5350.toInt()), Color(0xFF26C6DA.toInt()),
        Color(0xFF8D6E63.toInt()), Color(0xFF7E57C2.toInt())
    )

    // --- Modelo da JList ---
    private val listaModel: DefaultListModel<ItemLista> = DefaultListModel()

    // --- Componentes ---
    private val listaUsuarios = JList(listaModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        fixedCellHeight = 72
        cellRenderer = ItemListaRenderer(corFundoAvatar)
        background = corSurface
    }

    private val campoBusca = JTextField().apply {
        font = Font("Segoe UI", Font.PLAIN, 14)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(corDivider, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        )
    }

    private val rotuloContador = JLabel("0 usuários").apply {
        font = Font("Segoe UI", Font.BOLD, 12)
        foreground = corTextoSecund
        horizontalAlignment = SwingConstants.CENTER
    }

    private val rotuloStatus = JLabel("Pronto.").apply {
        font = Font("Segoe UI", Font.PLAIN, 12)
        foreground = corTextoSecund
        horizontalAlignment = SwingConstants.CENTER
    }

    // Botões estilo "Material chip" — visual mobile
    private val chipListar  = criarChip("Listar",      corPrimaria)
    private val chipAtivos  = criarChip("Ativos",      Color(0xFF43A047.toInt()))
    private val chipBuscar  = criarChip("Buscar",      Color(0xFFFB8C00.toInt()))
    private val chipAgrupar = criarChip("Agrupar",     Color(0xFF8E24AA.toInt()))
    private val chipResetar = criarChip("Resetar",     Color(0xFF546E7A.toInt()))

    // FAB (Floating Action Button) — "Incluir"
    private val fabIncluir = JButton("+").apply {
        font = Font("Segoe UI", Font.BOLD, 28)
        foreground = Color.WHITE
        background = corAccent
        isFocusPainted = false
        isContentAreaFilled = false   // pintamos manualmente para ficar circular
        isOpaque = false
        preferredSize = Dimension(60, 60)
        toolTipText = "Incluir usuário"
        border = BorderFactory.createEmptyBorder()
    }

    init { configurarEventos() }

    private fun configurarEventos() {
        chipListar.addActionListener  { recarregarUsuarios(repositorio.listarTodos(), "Listagem completa") }
        chipAtivos.addActionListener  { recarregarUsuarios(repositorio.listarAtivos(), "Apenas ativos (filter)") }
        chipBuscar.addActionListener  {
            val p = campoBusca.text.trim()
            recarregarUsuarios(repositorio.buscarPorPrefixo(p), if (p.isEmpty()) "Busca limpa" else "Busca: '$p'")
        }
        chipAgrupar.addActionListener { acaoAgrupar() }
        chipResetar.addActionListener {
            val op = JOptionPane.showConfirmDialog(
                null,
                "Resetar a lista ao estado inicial (seed)?\nAs inclusões/exclusões em runtime serão perdidas.",
                "Confirmar reset", JOptionPane.YES_NO_OPTION
            )
            if (op == JOptionPane.YES_OPTION) {
                repositorio.resetar()
                recarregarUsuarios(repositorio.listarTodos(), "Lista resetada para o seed")
            }
        }
        fabIncluir.addActionListener { acaoIncluir() }
        // Excluir: clique longo na linha da lista
        listaUsuarios.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) acaoExcluir()
            }
        })
    }

    // ---------- Ações de negócio ----------

    private fun acaoIncluir() {
        val painel = JPanel(GridBagLayout()).apply { background = corSurface }
        val tfNome  = campoEstilizado("Ex.: Ana Lima")
        val tfEmail = campoEstilizado("Ex.: ana@empresa.com.br")
        val tfSetor = campoEstilizado("Ex.: Engenharia")

        painel.add(labelEstilizado("Nome"),   gbc(0, 0)); painel.add(tfNome,  gbc(1, 0))
        painel.add(labelEstilizado("E-mail"), gbc(0, 1)); painel.add(tfEmail, gbc(1, 1))
        painel.add(labelEstilizado("Setor"),  gbc(0, 2)); painel.add(tfSetor, gbc(1, 2))

        val res = JOptionPane.showConfirmDialog(
            null, painel, "Novo usuário",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        )
        if (res == JOptionPane.OK_OPTION) {
            val nome = tfNome.text.trim()
            val email = tfEmail.text.trim()
            val setor = tfSetor.text.trim()
            if (nome.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Nome e e-mail são obrigatórios.")
                return
            }
            val novo = repositorio.incluir(nome, email, setor.ifEmpty { "Geral" }, ativo = true)
            recarregarUsuarios(repositorio.listarTodos(), "Incluído: ${novo.nome}")
        }
    }

    private fun acaoExcluir() {
        val item = listaUsuarios.selectedValue
        if (item == null || item.id == null) {
            JOptionPane.showMessageDialog(null, "Selecione um usuário (clique duas vezes na linha) para excluir.")
            return
        }
        val op = JOptionPane.showConfirmDialog(
            null, "Excluir ${item.nome}?", "Confirmar exclusão",
            JOptionPane.YES_NO_OPTION
        )
        if (op == JOptionPane.YES_OPTION) {
            val removeu = repositorio.excluir(item.id)
            recarregarUsuarios(repositorio.listarTodos(), if (removeu) "Excluído: ${item.nome}" else "Nada removido")
        }
    }

    private fun acaoAgrupar() {
        val mapa = repositorio.agruparPorInicial()
        val itens: List<ItemLista> = mapa.toSortedMap().flatMap { (letra, grupo) ->
            listOf(ItemLista.cabecalho(letra)) + grupo.map { ItemLista.linha(it) }
        }
        recarregarItens(itens, "Agrupado por inicial (groupBy)")
    }

    // ---------- Helpers ----------

    private fun recarregarUsuarios(usuarios: List<Usuario>, status: String) =
        recarregarItens(usuarios.map { ItemLista.linha(it) }, status)

    private fun recarregarItens(itens: List<ItemLista>, status: String) {
        listaModel.clear()
        itens.forEach { listaModel.addElement(it) }
        rotuloContador.text = "${itens.size} item(ns)"
        rotuloStatus.text = status
    }

    private fun criarChip(texto: String, cor: Color): JButton {
        val b = JButton(texto).apply {
            font = Font("Segoe UI", Font.BOLD, 12)
            foreground = cor
            background = corCard
            isFocusPainted = false
            isContentAreaFilled = false
            isOpaque = false
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cor, 1, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
            )
            cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
        }
        // Hover effect
        b.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { b.foreground = Color.WHITE; b.background = cor }
            override fun mouseExited(e: MouseEvent)  { b.foreground = cor;            b.background = corCard }
        })
        return b
    }

    private fun campoEstilizado(placeholder: String): JTextField =
        JTextField().apply {
            font = Font("Segoe UI", Font.PLAIN, 14)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(corDivider, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            )
            toolTipText = placeholder
        }

    private fun labelEstilizado(texto: String): JLabel =
        JLabel(texto).apply {
            font = Font("Segoe UI", Font.BOLD, 13)
            foreground = corTextoPrimario
        }

    private fun gbc(x: Int, y: Int): GridBagConstraints = GridBagConstraints().apply {
        gridx = x; gridy = y
        insets = Insets(4, 4, 4, 4)
        fill = GridBagConstraints.HORIZONTAL
        weightx = if (x == 1) 1.0 else 0.0
    }

    // ---------- Modelo de item da lista (linha ou cabeçalho de seção) ----------

    private data class ItemLista(
        val id: Long?,
        val nome: String,
        val email: String,
        val setor: String,
        val ativo: Boolean,
        val cabecalho: Char?
    ) {
        companion object {
            fun linha(u: Usuario) = ItemLista(u.id, u.nome, u.email, u.setor, u.ativo, null)
            fun cabecalho(letra: Char) = ItemLista(null, "", "", "", true, letra)
        }
    }

    // ---------- Renderer customizado para a lista (visual mobile) ----------

    private inner class ItemListaRenderer(private val paletaAvatar: List<Color>) : ListCellRenderer<ItemLista> {
        private val baseRenderer = DefaultListCellRenderer()

        override fun getListCellRendererComponent(
            list: JList<out ItemLista>, value: ItemLista, index: Int,
            isSelected: Boolean, cellHasFocus: Boolean
        ): Component {
            val comp = baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            // Reset
            comp.text = ""
            comp.icon = null
            comp.border = EmptyBorder(0, 0, 0, 0)
            comp.background = if (isSelected) Color(0xFFE3F2FD.toInt()) else corSurface
            comp.isOpaque = true

            if (value.cabecalho != null) {
                // Cabeçalho de seção — pill rosa-claro com a letra
                comp.text = "  ${value.cabecalho}  "
                comp.font = Font("Segoe UI", Font.BOLD, 13)
                comp.foreground = corPrimaria
                comp.background = Color(0xFFE3F2FD.toInt())
                comp.horizontalAlignment = SwingConstants.LEFT
                comp.border = EmptyBorder(8, 16, 4, 16)
                return comp
            }

            // Linha de usuário — avatar circular + textos
            val panel = JPanel(BorderLayout(12, 0)).apply {
                background = if (isSelected) Color(0xFFE3F2FD.toInt()) else corCard
                border = EmptyBorder(10, 12, 10, 12)
            }

            // Avatar
            val corAvatar = paletaAvatar[Math.floorMod(value.nome.hashCode(), paletaAvatar.size)]
            val avatar = AvatarCirculo(value.nome.takeIf { it.isNotBlank() }?.first()?.uppercaseChar() ?: '?',
                                       if (value.ativo) corAvatar else corInativo)
            panel.add(JLabel(avatar).apply {
                border = EmptyBorder(0, 0, 0, 0)
            }, BorderLayout.WEST)

            // Textos
            val textos = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = panel.background
            }
            val lblNome = JLabel(value.nome).apply {
                font = Font("Segoe UI", Font.BOLD, 14)
                foreground = if (value.ativo) corTextoPrimario else corInativo
            }
            val lblEmail = JLabel(value.email).apply {
                font = Font("Segoe UI", Font.PLAIN, 12)
                foreground = corTextoSecund
            }
            val lblSetor = JLabel(value.setor + if (!value.ativo) "  •  inativo" else "").apply {
                font = Font("Segoe UI", Font.PLAIN, 11)
                foreground = corAccent
            }
            textos.add(lblNome)
            textos.add(Box.createVerticalStrut(2))
            textos.add(lblEmail)
            textos.add(Box.createVerticalStrut(2))
            textos.add(lblSetor)
            panel.add(textos, BorderLayout.CENTER)

            // Divisor inferior
            val wrapper = JPanel(BorderLayout()).apply { background = corSurface }
            wrapper.add(panel, BorderLayout.CENTER)
            wrapper.add(JPanel().apply {
                background = corDivider
                preferredSize = Dimension(0, 1)
            }, BorderLayout.SOUTH)

            return wrapper
        }
    }

    /** Ícone pintado em runtime: círculo colorido + inicial branca. */
    private class AvatarCirculo(private val letra: Char, private val cor: Color) : Icon {
        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            // sombra leve
            g2.color = Color(0, 0, 0, 30)
            g2.fillOval(x + 2, y + 3, 44, 44)
            g2.color = cor
            g2.fillOval(x, y, 44, 44)
            // letra
            g2.color = Color.WHITE
            g2.font = Font("Segoe UI", Font.BOLD, 20)
            val fm = g2.fontMetrics
            val tx = x + (44 - fm.stringWidth(letra.toString())) / 2
            val ty = y + (44 - fm.height) / 2 + fm.ascent
            g2.drawString(letra.toString(), tx, ty)
            g2.dispose()
        }
        override fun getIconWidth(): Int = 44
        override fun getIconHeight(): Int = 44
    }

    // ---------- AppBar (top bar) ----------

    private inner class AppBar(val titulo: String, val subtitulo: String) : JPanel() {
        init {
            layout = BorderLayout()
            preferredSize = Dimension(360, 96)
            isOpaque = false
        }
        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            // Gradiente primário
            g2.paint = GradientPaint(0f, 0f, corPrimaria, 0f, height.toFloat(), corPrimariaDark)
            g2.fillRect(0, 0, width, height)
            // Sombra inferior sutil
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f)
            g2.color = Color.BLACK
            g2.fillRect(0, height - 4, width, 4)
            g2.dispose()
            super.paintComponent(g)
        }
    }

    // ---------- FAB pintado (círculo) ----------

    private inner class FabButton : JButton() {
        init {
            preferredSize = Dimension(60, 60)
            isContentAreaFilled = false
            isFocusPainted = false
            isBorderPainted = false
            cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
        }
        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            // sombra
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f)
            g2.color = Color.BLACK
            g2.fillOval(2, 4, 56, 56)
            g2.composite = AlphaComposite.SrcOver
            g2.color = if (model.isPressed) corAccent.darker() else corAccent
            g2.fillOval(0, 0, 56, 56)
            g2.color = Color.WHITE
            g2.font = Font("Segoe UI", Font.BOLD, 28)
            val fm = g2.fontMetrics
            val s = "+"
            val tx = (56 - fm.stringWidth(s)) / 2
            val ty = (56 - fm.height) / 2 + fm.ascent
            g2.drawString(s, tx, ty)
            g2.dispose()
        }
    }

    // ---------- Montagem da "tela de celular" ----------

    fun mostrar() {
        val fab = FabButton().apply {
            toolTipText = "Incluir usuário"
            addActionListener { acaoIncluir() }
        }

        val appBar = AppBar("Usuários", "MAPA Mobile — Kotlin").apply {
            val tituloLbl = JLabel("Usuários").apply {
                font = Font("Segoe UI", Font.BOLD, 20)
                foreground = corOnPrimary
            }
            val subtituloLbl = JLabel("MAPA Mobile — Kotlin").apply {
                font = Font("Segoe UI", Font.PLAIN, 12)
                foreground = Color(255, 255, 255, 220)
            }
            val textos = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = EmptyBorder(20, 16, 12, 16)
            }
            textos.add(tituloLbl)
            textos.add(Box.createVerticalStrut(2))
            textos.add(subtituloLbl)
            add(textos, BorderLayout.CENTER)
        }

        val chips = JPanel(FlowLayout(FlowLayout.CENTER, 8, 8)).apply {
            background = corSurface
            border = EmptyBorder(4, 8, 4, 8)
        }
        chips.add(chipListar); chips.add(chipAtivos); chips.add(chipBuscar); chips.add(chipAgrupar); chips.add(chipResetar)

        val topo = JPanel(BorderLayout()).apply { background = corSurface }
        topo.add(appBar, BorderLayout.NORTH)
        topo.add(campoBusca.apply {
            border = BorderFactory.createCompoundBorder(
                EmptyBorder(8, 16, 8, 16),
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(corDivider, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                )
            )
        }, BorderLayout.CENTER)
        topo.add(chips, BorderLayout.SOUTH)

        val rodape = JPanel(BorderLayout()).apply {
            background = corCard
            border = EmptyBorder(8, 16, 12, 16)
        }
        rodape.add(rotuloContador, BorderLayout.NORTH)
        rodape.add(rotuloStatus, BorderLayout.SOUTH)

        // Layered: lista como conteúdo + FAB flutuante no canto inferior direito
        val listaWrap = JPanel(BorderLayout()).apply { background = corSurface }
        listaWrap.add(JScrollPane(listaUsuarios).apply {
            border = BorderFactory.createEmptyBorder()
            viewport.background = corSurface
        }, BorderLayout.CENTER)
        listaWrap.add(rodape, BorderLayout.SOUTH)

        val tela = JPanel(BorderLayout()).apply { background = corSurface }
        tela.add(topo, BorderLayout.NORTH)
        tela.add(listaWrap, BorderLayout.CENTER)

        val frame = JFrame("Usuários").apply {
            preferredSize = Dimension(360, 720)
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            isResizable = false
            layout = BorderLayout()
            add(tela, BorderLayout.CENTER)

            // FAB flutuante sobreposto
            val fabHolder = JPanel(null).apply {
                isOpaque = false
                preferredSize = Dimension(360, 80)
            }
            fabHolder.add(fab)
            fab.setBounds(360 - 60 - 16, 8, 60, 60)
            add(fabHolder, BorderLayout.SOUTH)

            pack()
            setLocationRelativeTo(null)
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            SwingUtilities.updateComponentTreeUI(frame)
        } catch (_: Exception) { /* fallback L&F padrão */ }

        recarregarUsuarios(repositorio.listarTodos(), "Lista inicial (seed)")
        frame.isVisible = true
    }

    companion object {
        /** Entry point estático — usado pelo Main-Class do JAR executável. */
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater { UsuariosApp().mostrar() }
        }
    }
}
