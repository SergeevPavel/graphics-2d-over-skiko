@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package org.example

import makeUseSkikoGraphics
import java.awt.*
import javax.swing.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun swingSample() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Graphics2D over Skiko").apply {
            makeUseSkikoGraphics()
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            setSize(400, 200)
//            contentPane.add(createUiPanel())
            contentPane.add(createScrollPanel())
            isVisible = true
        }
    }
}

fun createScrollPanel(): JScrollPane {
    return JScrollPane(createUiPanel())
}

fun createUiPanel(): JPanel {
    // Transparent UI panel on top
    val uiPanel = JPanel(FlowLayout()).apply {
//        isOpaque = false
        bounds = Rectangle(0, 0, 400, 200)
    }

    val label = JLabel("Ready")
    val btnHello = JButton("Say Hello").apply {
        addActionListener { label.text = "Hello, World!" }
    }
    val btnReset = JButton("Reset").apply {
        addActionListener {
            println("Button was pressed!!!")
            label.text = "Ready"
        }
    }

    val swingSpinner = swingSpinner()

    uiPanel.add(btnHello)
    uiPanel.add(btnReset)
    uiPanel.add(label)
    uiPanel.add(swingSpinner)
    return uiPanel
}

fun swingSpinner(): JComponent {
    return object : JComponent() {
        private var angle = 0.0
        init {
            preferredSize = Dimension(40, 40)
            Timer(30) {
                angle += 0.1
                repaint()
            }.start()
        }
        override fun paintComponent(g: Graphics) {
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val cx = width / 2.0
            val cy = height / 2.0
            val r = minOf(cx, cy) - 2
            for (i in 0 until 8) {
                val a = angle + i * PI / 4
                val alpha = (255 * (i + 1) / 8f).toInt()
                g2.color = Color(60, 120, 200, alpha)
                val x = cx + cos(a) * r * 0.6
                val y = cy + sin(a) * r * 0.6
                g2.fillOval((x - 3).toInt(), (y - 3).toInt(), 6, 6)
            }
        }
    }
}