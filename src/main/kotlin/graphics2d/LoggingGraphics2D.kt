@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package org.example.graphics2d

import sun.awt.ConstrainableGraphics
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Composite
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsConfiguration
import java.awt.Image
import java.awt.Paint
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.Stroke
import java.awt.font.FontRenderContext
import java.awt.font.GlyphVector
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ImageObserver
import java.awt.image.RenderedImage
import java.awt.image.renderable.RenderableImage
import java.text.AttributedCharacterIterator

class LoggingGraphics2D(val delegate: Graphics2D, val id: String) : Graphics2D(), ConstrainableGraphics {
    private fun log(method: String, vararg args: Any?) {
        val argsStr = args.joinToString(", ")
        println("Graphics2D [$id].$method($argsStr)")
    }

    var copyCounter = 0
    // --- State ---
    private var currentColor: Color = Color.BLACK
    private var currentFont: Font = Font("Dialog", Font.PLAIN, 12)
    private var currentStroke: Stroke = BasicStroke()
    private var currentTransform: AffineTransform = AffineTransform()
    private var currentComposite: Composite = AlphaComposite.SrcOver
    private var currentPaint: Paint = currentColor
    private var currentBackground: Color = Color.WHITE
    private var currentClip: Shape? = null
    private val hints = RenderingHints(null as Map<RenderingHints.Key, Any>?)

    // --- Graphics2D methods ---
    override fun draw(s: Shape) = log("draw", s)
    override fun fill(s: Shape) = log("fill", s)

    override fun drawString(str: String, x: Int, y: Int) = log("drawString", str, x, y)
    override fun drawString(str: String, x: Float, y: Float) = log("drawString", str, x, y)
    override fun drawString(iterator: AttributedCharacterIterator, x: Int, y: Int) = log("drawString", "iterator", x, y)
    override fun drawString(iterator: AttributedCharacterIterator, x: Float, y: Float) = log("drawString", "iterator", x, y)

    override fun drawGlyphVector(g: GlyphVector, x: Float, y: Float) = log("drawGlyphVector", x, y)

    override fun drawImage(img: Image, xform: AffineTransform, obs: ImageObserver?): Boolean { log("drawImage", xform); return true }
    override fun drawImage(img: BufferedImage, op: BufferedImageOp?, x: Int, y: Int) = log("drawImage", op, x, y)
    override fun drawImage(img: Image, x: Int, y: Int, observer: ImageObserver?): Boolean { log("drawImage", x, y); return true }
    override fun drawImage(img: Image, x: Int, y: Int, w: Int, h: Int, observer: ImageObserver?): Boolean { log("drawImage", x, y, w, h); return true }
    override fun drawImage(img: Image, x: Int, y: Int, bgcolor: Color?, observer: ImageObserver?): Boolean { log("drawImage", x, y, bgcolor); return true }
    override fun drawImage(img: Image, x: Int, y: Int, w: Int, h: Int, bgcolor: Color?, observer: ImageObserver?): Boolean { log("drawImage", x, y, w, h, bgcolor); return true }
    override fun drawImage(img: Image, dx1: Int, dy1: Int, dx2: Int, dy2: Int, sx1: Int, sy1: Int, sx2: Int, sy2: Int, observer: ImageObserver?): Boolean { log("drawImage", dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2); return true }
    override fun drawImage(img: Image, dx1: Int, dy1: Int, dx2: Int, dy2: Int, sx1: Int, sy1: Int, sx2: Int, sy2: Int, bgcolor: Color?, observer: ImageObserver?): Boolean { log("drawImage", dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor); return true }

    override fun drawRenderedImage(img: RenderedImage, xform: AffineTransform) = log("drawRenderedImage", xform)
    override fun drawRenderableImage(img: RenderableImage, xform: AffineTransform) = log("drawRenderableImage", xform)

    override fun hit(rect: Rectangle, s: Shape, onStroke: Boolean): Boolean { log("hit", rect, s, onStroke); return false }

    override fun getDeviceConfiguration(): GraphicsConfiguration {
        log("getDeviceConfiguration")
        return BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().deviceConfiguration
    }

    override fun getFontRenderContext(): FontRenderContext { log("getFontRenderContext"); return FontRenderContext(
        null,
        false,
        false
    )
    }

    // --- Composite, Paint, Stroke ---
    override fun setComposite(comp: Composite) { log("setComposite", comp); currentComposite = comp }
    override fun getComposite(): Composite = currentComposite
    override fun setPaint(paint: Paint) { log("setPaint", paint); currentPaint = paint }
    override fun getPaint(): Paint = currentPaint
    override fun setStroke(s: Stroke) { log("setStroke", s); currentStroke = s }
    override fun getStroke(): Stroke = currentStroke

    // --- Rendering hints ---
    override fun setRenderingHint(key: RenderingHints.Key, value: Any?) { log("setRenderingHint", key, value); hints[key] = value }
    override fun getRenderingHint(key: RenderingHints.Key): Any? { log("getRenderingHint", key); return hints[key] }
    override fun setRenderingHints(h: Map<*, *>) { log("setRenderingHints"); hints.clear(); hints.putAll(h as Map<out RenderingHints.Key, Any>) }
    override fun addRenderingHints(h: Map<*, *>) { log("addRenderingHints"); hints.putAll(h as Map<out RenderingHints.Key, Any>) }
    override fun getRenderingHints(): RenderingHints = hints

    // --- Transform ---
    override fun translate(x: Int, y: Int) { log("translate", x, y); currentTransform.translate(x.toDouble(), y.toDouble()) }
    override fun translate(tx: Double, ty: Double) { log("translate", tx, ty); currentTransform.translate(tx, ty) }
    override fun rotate(theta: Double) { log("rotate", theta); currentTransform.rotate(theta) }
    override fun rotate(theta: Double, x: Double, y: Double) { log("rotate", theta, x, y); currentTransform.rotate(theta, x, y) }
    override fun scale(sx: Double, sy: Double) { log("scale", sx, sy); currentTransform.scale(sx, sy) }
    override fun shear(shx: Double, shy: Double) { log("shear", shx, shy); currentTransform.shear(shx, shy) }
    override fun transform(tx: AffineTransform) { log("transform", tx); currentTransform.concatenate(tx) }
    override fun setTransform(tx: AffineTransform) { log("setTransform", tx); currentTransform = AffineTransform(tx)
    }
    override fun getTransform(): AffineTransform = AffineTransform(currentTransform)

    // --- Color, Font ---
    override fun getColor(): Color = currentColor
    override fun setColor(c: Color) { log("setColor", c); currentColor = c; currentPaint = c }
    override fun getFont(): Font = currentFont
    override fun setFont(font: Font) { log("setFont", font); currentFont = font }
    override fun getFontMetrics(f: Font): FontMetrics {
        log("getFontMetrics", f)
        return BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().getFontMetrics(f)
    }
    override fun setBackground(color: Color) { log("setBackground", color); currentBackground = color }
    override fun getBackground(): Color = currentBackground
    override fun setPaintMode() = log("setPaintMode")
    override fun setXORMode(c: Color) = log("setXORMode", c)

    // --- Clip ---
    override fun getClip(): Shape? = currentClip
    override fun setClip(clip: Shape?) { log("setClip", clip); currentClip = clip }
    override fun setClip(x: Int, y: Int, w: Int, h: Int) { log("setClip", x, y, w, h); currentClip =
        Rectangle(x, y, w, h)
    }
    override fun clip(s: Shape) { log("clip", s); currentClip = s }
    override fun clipRect(x: Int, y: Int, w: Int, h: Int) { log("clipRect", x, y, w, h) }
    override fun getClipBounds(): Rectangle? { log("getClipBounds"); return (currentClip as? Rectangle) ?: currentClip?.bounds }

    // --- Drawing primitives ---
    override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) = log("drawLine", x1, y1, x2, y2)
    override fun fillRect(x: Int, y: Int, w: Int, h: Int) = log("fillRect", x, y, w, h)
    override fun clearRect(x: Int, y: Int, w: Int, h: Int) = log("clearRect", x, y, w, h)
    override fun drawRoundRect(x: Int, y: Int, w: Int, h: Int, arcW: Int, arcH: Int) = log("drawRoundRect", x, y, w, h, arcW, arcH)
    override fun fillRoundRect(x: Int, y: Int, w: Int, h: Int, arcW: Int, arcH: Int) = log("fillRoundRect", x, y, w, h, arcW, arcH)
    override fun drawOval(x: Int, y: Int, w: Int, h: Int) = log("drawOval", x, y, w, h)
    override fun fillOval(x: Int, y: Int, w: Int, h: Int) = log("fillOval", x, y, w, h)
    override fun drawArc(x: Int, y: Int, w: Int, h: Int, startAngle: Int, arcAngle: Int) = log("drawArc", x, y, w, h, startAngle, arcAngle)
    override fun fillArc(x: Int, y: Int, w: Int, h: Int, startAngle: Int, arcAngle: Int) = log("fillArc", x, y, w, h, startAngle, arcAngle)
    override fun drawPolyline(xPoints: IntArray, yPoints: IntArray, nPoints: Int) = log("drawPolyline", nPoints)
    override fun drawPolygon(xPoints: IntArray, yPoints: IntArray, nPoints: Int) = log("drawPolygon", nPoints)
    override fun fillPolygon(xPoints: IntArray, yPoints: IntArray, nPoints: Int) = log("fillPolygon", nPoints)

    // --- Misc ---
    override fun copyArea(x: Int, y: Int, w: Int, h: Int, dx: Int, dy: Int) = log("copyArea", x, y, w, h, dx, dy)
    override fun create(): Graphics {
        log("create")
        copyCounter++
        val copyId = "$id copy $copyCounter"
        return LoggingGraphics2D(delegate, copyId)
    }
    override fun dispose() = log("dispose")

//    override fun constrain(x: Int, y: Int, width: Int, height: Int, visibleRegion: Region?) {
//        log("constrain", x, y, width, height, visibleRegion)
//    }

    override fun constrain(x: Int, y: Int, w: Int, h: Int) {
        log("constrain", x, y, w, h)
    }
}