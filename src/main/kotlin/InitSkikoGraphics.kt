@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

import graphics2d.SkikoGraphics2D
import org.jetbrains.skia.ColorSpace
import org.jfree.skija.SkiaGraphics2D
import sun.java2d.SunGraphics2D
import sun.java2d.SurfaceData
import sun.java2d.metal.MTLSurfaceData
import java.awt.Color
import java.awt.EventQueue
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingTask
import java.awt.Window
import javax.swing.JFrame
import javax.swing.RepaintManager

fun Window.overrideGraphics2D(factory: (surfaceData: SurfaceData, fg: Color, bg: Color, font: Font) -> Graphics?) {
    try {
        this.setGraphicsFactory { surfaceData, fg, bg, font ->
            try {
                factory(surfaceData, fg, bg, font)
            } catch (e: Throwable) {
                Logger.error { "Failed to create graphics for surfaceData=$surfaceData, fg=$fg, bg=$bg, font=$font: $e" }
                null
            }
        }
    } catch (e: Throwable) {
        Logger.error { "Failed to override graphics factory: $e" }
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
    makeUseSkikoGraphics1WithPicture()
//    makeUseSkikoGraphics2WithSurface()
//    makeUseSkikoGraphics2WithPicture()
}

fun JFrame.makeUseSkikoGraphics1WithPicture() {
    overrideGraphics2D { surfaceData, fg, bg, font ->
        val sunGraphics2D = SunGraphics2D(surfaceData, fg, bg, font)
        (surfaceData as? MTLSurfaceData)?.let {
            Logger.debug { "Surface data width: ${surfaceData.width}, height: ${surfaceData.height}" }
        } ?: run {
            Logger.debug { "Unsupported surface data type: $surfaceData" }
            return@overrideGraphics2D null
        }
        if (!EventQueue.isDispatchThread()) return@overrideGraphics2D null
        assert(EventQueue.isDispatchThread()) {
            "Current thread is not event dispatch thread: ${Thread.currentThread()}"
        }
        val pictureRecorder = org.jetbrains.skia.PictureRecorder()
        val width = surfaceData.width.toFloat()
        val height = surfaceData.height.toFloat()
        assert(width > 0 && height > 0) { "Surface data width and height must be positive: $width, $height" }
        val skikoGraphics2D = SkikoGraphics2D(pictureRecorder.beginRecording(0f, 0f, width, height, null))
        skikoGraphics2D.onDispose = {
            val picture = pictureRecorder.finishRecordingAsPicture()
            withSkiaCanvas(sunGraphics2D) { canvas, directContext, scale ->
                // Swing assumes that the content persists between frames
                // even when double buffering is enabled
//                canvas.clear(org.jetbrains.skia.Color.MAGENTA)
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

fun JFrame.makeUseSkikoGraphics2WithSurface() {
    overrideGraphics2D { surfaceData, fg, bg, font ->
        val sunGraphics2D = SunGraphics2D(surfaceData, fg, bg, font)
        (surfaceData as? MTLSurfaceData)?.let {
            val skikoGraphics2D = SkiaGraphics2D(surfaceData.width, surfaceData.height)
            skikoGraphics2D.onDispose = {
                withSkiaCanvas(sunGraphics2D) { canvas, directContext, scale ->
                    //canvas.clear(org.jetbrains.skia.Color.MAGENTA)
                    canvas.drawImage(skikoGraphics2D.getSurface().makeImageSnapshot(), 0f, 0f)
                }
            }
            skikoGraphics2D.transform(sunGraphics2D.transform)
            skikoGraphics2D.color = fg
            skikoGraphics2D.background = bg
            skikoGraphics2D.paint = fg
            skikoGraphics2D.font = font
            skikoGraphics2D
        }
    }
    disableDoubleBuffering()
}

fun JFrame.makeUseSkikoGraphics2WithPicture() {
    overrideGraphics2D { surfaceData, fg, bg, font ->
        val sunGraphics2D = SunGraphics2D(surfaceData, fg, bg, font)
        (surfaceData as? MTLSurfaceData)?.let {
            Logger.debug { "Surface data width: ${surfaceData.width}, height: ${surfaceData.height}" }
        } ?: run {
            Logger.debug { "Unsupported surface data type: $surfaceData" }
            return@overrideGraphics2D null
        }
        if (!EventQueue.isDispatchThread()) return@overrideGraphics2D null
        assert(EventQueue.isDispatchThread()) {
            "Current thread is not event dispatch thread: ${Thread.currentThread()}"
        }
        val pictureRecorder = org.jetbrains.skia.PictureRecorder()
        val width = surfaceData.width.toFloat()
        val height = surfaceData.height.toFloat()
        assert(width > 0 && height > 0) { "Surface data width and height must be positive: $width, $height" }
        val skiaGraphics2D = SkiaGraphics2D(pictureRecorder.beginRecording(0f, 0f, width, height, null))
        skiaGraphics2D.onDispose = {
            val picture = pictureRecorder.finishRecordingAsPicture()
            withSkiaCanvas(sunGraphics2D) { canvas, directContext, scale ->
                // Swing assumes that the content persists between frames
                // even when double buffering is enabled
                canvas.clear(org.jetbrains.skia.Color.MAGENTA)
                canvas.drawPicture(picture)
            }
        }
        skiaGraphics2D.transform(sunGraphics2D.transform)
        skiaGraphics2D.color = fg
        skiaGraphics2D.background = bg
        skiaGraphics2D.paint = fg
        skiaGraphics2D.font = font
        skiaGraphics2D
    }
    disableDoubleBuffering()
}

var counter = 0

//fun JFrame.makeUseLoggingGraphics() {
//    overrideGraphics2D { surfaceData, fg, bg, font ->
//        val sunGraphics2D = SunGraphics2D(surfaceData, fg, bg, font)
//        (surfaceData as? MTLSurfaceData)?.let {
//            Logger.debug { "Surface data width: ${surfaceData.width}, height: ${surfaceData.height}" }
//        } ?: run {
//            Logger.debug { "Unsupported surface data type: $surfaceData" }
//        }
//        val skikoGraphics2D = SkikoGraphics2D { x, y, picture ->
//            withSkiaCanvas(sunGraphics2D) { canvas, _, _ ->
//                canvas.clear(org.jetbrains.skia.Color.MAGENTA)
//                canvas.translate(0f, 32f * 2) // titlebar
//                canvas.drawPicture(picture)
//            }
//        }
//        counter += 1
//        val loggingGraphics2D =
//            LoggingGraphics2D(delegate = skikoGraphics2D, "$counter")
//        loggingGraphics2D.color = fg
//        loggingGraphics2D.background = bg
//        loggingGraphics2D.paint = fg
//        loggingGraphics2D.font = font
//        loggingGraphics2D
//    }
//    disableDoubleBuffering()
//}

fun JFrame.disableDoubleBuffering() {
    RepaintManager.currentManager(this)?.setDoubleBufferingEnabled(false)
}