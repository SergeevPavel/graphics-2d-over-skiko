@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package org.example

import org.example.graphics2d.LoggingGraphics2D
import org.example.graphics2d.withSkiaCanvas
import sun.java2d.SunGraphics2D
import java.awt.Dimension
import java.awt.Graphics
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.Timer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun skikoSpinner(): JComponent {
    return object : JComponent() {
        private var angle = 0.0
        init {
            preferredSize = Dimension(400, 200)
            Timer(30) {
                angle += 0.1
                repaint()
            }.start()
        }
        override fun paintComponent(g: Graphics) {
            val g2 = when {
                g is LoggingGraphics2D -> g.delegate
                g is SunGraphics2D -> g
                else -> TODO("Isn't possible")
            }

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            withSkiaCanvas(g2) { canvas, _, _ ->
                // Clearing the canvas destroys all content
                //  canvas.clear(org.jetbrains.skia.Color.TRANSPARENT)

                val cx = 200 //width / 2f + 50f
                val cy = 200 //height / 2f + 50f
                val r = minOf(cx, cy) - 2f
                val paint = org.jetbrains.skia.Paint().apply {
                    isAntiAlias = true
                }
                for (i in 0 until 8) {
                    val a = (angle + i * PI / 4).toFloat()
                    val alpha = (255 * (i + 1) / 8)
                    paint.color = (alpha shl 24) or 0x3C78C8 // ARGB: Color(60, 120, 200, alpha)
                    val x = cx + cos(a) * r * 0.6f
                    val y = cy + sin(a) * r * 0.6f
                    canvas.drawCircle(x, y, 10f, paint)
                }
            }
        }
    }
}