@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package org.example

import graphics2d.SkikoGraphics2D
import org.jetbrains.skia.ColorSpace
import sun.java2d.SunGraphics2D
import sun.java2d.SurfaceData
import sun.java2d.metal.MTLSurfaceData
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.RenderingTask
import java.awt.Window
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.RepaintManager
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

var counter = 0

fun Window.overrideGraphics2D(factory: (surfaceData: SurfaceData, fg: Color, bg: Color, font: Font) -> Graphics?) {
    this.setGraphicsFactory { surfaceData, fg, bg, font ->
        factory(surfaceData, fg, bg, font)
    }
}

fun withSkiaCanvas(
    g2d: Graphics2D,
    block: (org.jetbrains.skia.Canvas, org.jetbrains.skia.DirectContext, Float) -> Unit
) {
    g2d.runExternal(object : RenderingTask {
        override fun run(surfaceType: String?, pointers: List<Long>, names: List<String?>) {
            try {
                val device = pointers[RenderingTask.MTL_DEVICE_ARG_INDEX]
                val queue = pointers[RenderingTask.MTL_COMMAND_QUEUE_ARG_INDEX]
                val texture = pointers[RenderingTask.MTL_TEXTURE_ARG_INDEX]
                if (device == 0L || queue == 0L || texture == 0L) return

                val gc = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
                val scale = gc.defaultTransform.scaleX.toFloat()

                org.jetbrains.skia.DirectContext.makeMetal(device, queue).use { grCtx ->
                    val sd = ((g2d as SunGraphics2D).surfaceData as MTLSurfaceData)
//                    println("nativeWidth=${sd.nativeWidth} nativeHeight=${sd.nativeHeight}")
//                    println("height=${sd.height} width=${sd.width}")
                    val textureWidth = sd.nativeWidth
                    val textureHeight = sd.nativeHeight
                    org.jetbrains.skia.BackendRenderTarget.makeMetal(textureWidth, textureHeight, texture).use { backendRT ->
                        org.jetbrains.skia.Surface.makeFromBackendRenderTarget(
                            grCtx,
                            backendRT,
                            org.jetbrains.skia.SurfaceOrigin.TOP_LEFT,
                            org.jetbrains.skia.SurfaceColorFormat.BGRA_8888,
                            colorSpace = ColorSpace.sRGB,
                            null
                        )?.use { surface ->
                            block(surface.canvas, grCtx, scale)
                            grCtx.flushAndSubmit(surface)
                        }
                    }
                }
            } catch (e: Throwable) {
                println(e)
            }
        }
    })
}


fun swingSample() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Graphics2D over Skiko").apply {
            overrideGraphics2D { surfaceData, fg, bg, font ->
                val sunGraphics2D = SunGraphics2D(surfaceData, fg, bg, font)
                val skikoGraphics2D = SkikoGraphics2D { picture ->
                    withSkiaCanvas(sunGraphics2D) { canvas, _, _ ->
                        canvas.drawPicture(picture)
                    }
                }
                skikoGraphics2D.color = fg
                skikoGraphics2D.background = bg
                skikoGraphics2D.paint = fg
                skikoGraphics2D.font = font
                skikoGraphics2D
//                LoggingGraphics2D(delegate = gr, "LoggingGraphics2D[${counter++}]")

            }
            RepaintManager.currentManager(this)?.setDoubleBufferingEnabled(false)
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            setSize(400, 200)
//            contentPane.add(skikoSpinner())
            contentPane.add(createUiPanel())
            isVisible = true
        }
    }
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
//    uiPanel.add(skikoSpinner())
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