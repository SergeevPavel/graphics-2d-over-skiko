@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package org.example.graphics2d

import graphics2d.Logger
import graphics2d.SkikoGraphics2D
import org.jetbrains.skia.ColorSpace
import sun.java2d.SunGraphics2D
import sun.java2d.SurfaceData
import sun.java2d.metal.MTLSurfaceData
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingTask
import java.awt.Window
import javax.swing.JFrame
import javax.swing.RepaintManager

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
                Logger.error {
                    "Exception in rendering task: $e"
                }
            }
        }
    })
}

fun JFrame.makeUseSkikoGraphics() {
    overrideGraphics2D { surfaceData, fg, bg, font ->
        val sunGraphics2D = SunGraphics2D(surfaceData, fg, bg, font)
        (surfaceData as? MTLSurfaceData)?.let {
            Logger.debug { "Surface data width: ${surfaceData.width}, height: ${surfaceData.height}" }
        } ?: run {
            Logger.debug { "Unsupported surface data type: $surfaceData" }
        }
        val skikoGraphics2D = SkikoGraphics2D { picture ->
            withSkiaCanvas(sunGraphics2D) { canvas, _, _ ->
                canvas.clear(org.jetbrains.skia.Color.MAGENTA)
                canvas.translate(0f, 32f * 2) // titlebar
                canvas.drawPicture(picture)
            }
        }
        skikoGraphics2D.transform(sunGraphics2D.transform)
        skikoGraphics2D.color = fg
        skikoGraphics2D.background = bg
        skikoGraphics2D.paint = fg
        skikoGraphics2D.font = font
        skikoGraphics2D
    }
    disableDoubleBuffering()
}

var counter = 0

fun JFrame.makeUseLoggingGraphics() {
    overrideGraphics2D { surfaceData, fg, bg, font ->
        val sunGraphics2D = SunGraphics2D(surfaceData, fg, bg, font)
        (surfaceData as? MTLSurfaceData)?.let {
            Logger.debug { "Surface data width: ${surfaceData.width}, height: ${surfaceData.height}" }
        } ?: run {
            Logger.debug { "Unsupported surface data type: $surfaceData" }
        }
        val skikoGraphics2D = SkikoGraphics2D { picture ->
            withSkiaCanvas(sunGraphics2D) { canvas, _, _ ->
                canvas.clear(org.jetbrains.skia.Color.MAGENTA)
                canvas.translate(0f, 32f * 2) // titlebar
                canvas.drawPicture(picture)
            }
        }
        counter += 1
        val loggingGraphics2D = LoggingGraphics2D(delegate = skikoGraphics2D, "$counter")
        loggingGraphics2D.color = fg
        loggingGraphics2D.background = bg
        loggingGraphics2D.paint = fg
        loggingGraphics2D.font = font
        loggingGraphics2D
    }
    disableDoubleBuffering()
}

fun JFrame.disableDoubleBuffering() {
    RepaintManager.currentManager(this)?.setDoubleBufferingEnabled(false)
}