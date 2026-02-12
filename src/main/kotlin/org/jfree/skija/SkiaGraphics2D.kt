/* ===============
 * SkijaGraphics2D
 * ===============
 *
 * (C)opyright 2021-present, by David Gilbert.
 *
 * The SkijaGraphics2D class has been developed by David Gilbert for
 * use with Orson Charts (https://github.com/jfree/orson-charts) and
 * JFreeChart (https://www.jfree.org/jfreechart).  It may be useful for other
 * code that uses the Graphics2D API provided by Java2D.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   - Neither the name of the Object Refinery Limited nor the
 *     names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL OBJECT REFINERY LIMITED BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.jfree.skija

import dispatchIfNeeded
import graphics2d.SkikoGraphicsConfiguration
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.GradientStyle
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.PathFillMode
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Typeface
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Composite
import java.awt.Font
import java.awt.FontMetrics
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsConfiguration
import java.awt.Image
import java.awt.LinearGradientPaint
import java.awt.MultipleGradientPaint
import java.awt.Paint
import java.awt.RadialGradientPaint
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.Stroke
import java.awt.font.FontRenderContext
import java.awt.font.GlyphVector
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.GeneralPath
import java.awt.geom.Line2D
import java.awt.geom.NoninvertibleTransformException
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ColorModel
import java.awt.image.DataBufferInt
import java.awt.image.ImageObserver
import java.awt.image.RenderedImage
import java.awt.image.WritableRaster
import java.awt.image.renderable.RenderableImage
import java.text.AttributedCharacterIterator
import java.util.Collections
import java.util.Hashtable
import java.util.Locale
import java.util.function.Function
import kotlin.collections.HashMap
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.contentEquals
import kotlin.collections.contentToString
import kotlin.collections.indices
import kotlin.math.max
import kotlin.text.StringBuilder
import kotlin.text.contains
import kotlin.text.lowercase

/**
 * An implementation of the Graphics2D API that targets the Skija graphics API
 * (https://github.com/JetBrains/skija).
 */
class SkiaGraphics2D : Graphics2D {
    /* members */
    /** Rendering hints.  */
    private val hints: RenderingHints = RenderingHints(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_DEFAULT
    )

    /** Surface from Skija  */
    private var surface: Surface? = null

    private var width = 0
    private var height = 0

    /** Canvas from Skija  */
    private var canvas: Canvas? = null

    /** Paint used for drawing on Skija canvas.  */
    private var skiaPaint: org.jetbrains.skia.Paint? = null

    /** The Skija save/restore count, used to restore the original clip in setClip().  */
    private var restoreCount = 0

    private var awtPaint: Paint? = null

    /** Stores the AWT Color object for get/setColor().  */
    private var color: Color? = null

    private var stroke: Stroke? = null

    private var awtFont: Font? = null

    private var typeface: Typeface? = null

    private var skijaFont: org.jetbrains.skia.Font? = null

    /**
     * Returns the background color (the default value is [Color.BLACK]).
     * This attribute is used by the [.clearRect]
     * method.
     * 
     * @return The background color (possibly `null`).
     * 
     * @see .setBackground
     */
    /**
     * Sets the background color.  This attribute is used by the
     * [.clearRect] method.  The reference
     * implementation allows `null` for the background color so
     * we allow that too (but for that case, the [.clearRect]
     * method will do nothing).
     * 
     * @param color  the color (`null` permitted).
     * 
     * @see .getBackground
     */
    /** The background color, used in the `clearRect()` method.  */
    var backgroundImpl: Color? = null

    override fun setBackground(color: Color?) {
        backgroundImpl = color
    }

    override fun getBackground(): Color? {
        return backgroundImpl
    }

    private var transform: AffineTransform = AffineTransform()
    private var composite: Composite = AlphaComposite.getInstance(
        AlphaComposite.SRC_OVER, 1.0f
    )

    /**
     * Sets the composite (only `AlphaComposite` is handled).
     *
     * @param comp  the composite (`null` not permitted).
     *
     * @see .getComposite
     */
    override fun setComposite(comp: Composite?) {
        Logger.debug { "setComposite($comp)" }
        requireNotNull(comp) { "Null 'comp' argument." }
        composite = comp
        if (comp is AlphaComposite) {
            val ac: AlphaComposite = comp
            val skiaPaint = this.skiaPaint!!
            skiaPaint.setAlphaf(ac.alpha)

            when (ac.getRule()) {
                AlphaComposite.CLEAR -> skiaPaint.blendMode = BlendMode.CLEAR
                AlphaComposite.SRC -> skiaPaint.blendMode = BlendMode.SRC
                AlphaComposite.SRC_OVER -> skiaPaint.blendMode = BlendMode.SRC_OVER
                AlphaComposite.DST_OVER -> skiaPaint.blendMode = BlendMode.DST_OVER
                AlphaComposite.SRC_IN -> skiaPaint.blendMode = BlendMode.SRC_IN
                AlphaComposite.DST_IN -> skiaPaint.blendMode = BlendMode.DST_IN
                AlphaComposite.SRC_OUT -> skiaPaint.blendMode = BlendMode.SRC_OUT
                AlphaComposite.DST_OUT -> skiaPaint.blendMode = BlendMode.DST_OUT
                AlphaComposite.DST -> skiaPaint.blendMode = BlendMode.DST
                AlphaComposite.SRC_ATOP -> skiaPaint.blendMode = BlendMode.SRC_ATOP
                AlphaComposite.DST_ATOP -> skiaPaint.blendMode = BlendMode.DST_ATOP
            }
        }
    }

    override fun getComposite(): Composite {
        return this.composite
    }

    /** The user clip (can be null).  */
    var clipImpl: Shape? = null

    /**
     * Returns the font render context.
     * 
     * @return The font render context (never `null`).
     */
    /**
     * The font render context.
     */
    val fontRenderContextImpl: FontRenderContext = DEFAULT_FONT_RENDER_CONTEXT

    override fun getFontRenderContext(): FontRenderContext? {
        return DEFAULT_FONT_RENDER_CONTEXT
    }

    /**
     * An instance that is lazily instantiated in drawLine and then
     * subsequently reused to avoid creating a lot of garbage.
     */
    private var line: Line2D? = null

    /**
     * An instance that is lazily instantiated in fillRect and then
     * subsequently reused to avoid creating a lot of garbage.
     */
    private var rect: Rectangle2D? = null

    /**
     * An instance that is lazily instantiated in draw/fillRoundRect and then
     * subsequently reused to avoid creating a lot of garbage.
     */
    private var roundRect: RoundRectangle2D? = null

    /**
     * An instance that is lazily instantiated in draw/fillOval and then
     * subsequently reused to avoid creating a lot of garbage.
     */
    private var oval: Ellipse2D? = null

    /**
     * An instance that is lazily instantiated in draw/fillArc and then
     * subsequently reused to avoid creating a lot of garbage.
     */
    private var arc: Arc2D? = null

    /**
     * The device configuration (this is lazily instantiated in the
     * getDeviceConfiguration() method).
     */
    var deviceConfigurationImpl: GraphicsConfiguration? = null

    /**
     * Returns the device configuration associated with this
     * `Graphics2D`.
     *
     * @return The device configuration (never `null`).
     */
    override fun getDeviceConfiguration(): GraphicsConfiguration {
        if (deviceConfigurationImpl == null) {
            val width = width
            val height = height
            deviceConfigurationImpl = SkikoGraphicsConfiguration(
                width,
                height
            )
        }
        return deviceConfigurationImpl!!
    }

    /** Used and reused in the path() method below.  */
    private val coords = DoubleArray(6)

    /** Used and reused in the drawString() method below.  */
    private val sbStr = StringBuilder(256)

    /**
     * Creates a new instance with the specified height and width.
     * 
     * @param width  the width.
     * @param height  the height.
     */
    constructor(width: Int, height: Int) {
        Logger.debug { "SkijaGraphics2D($width, $height)" }
        this.width = width
        this.height = height
        this.surface = Surface.makeRasterN32Premul(width, height)
        init(surface!!.canvas)
    }

    /**
     * Creates a new instance with the specified height and width using an existing
     * canvas.
     * 
     * @param canvas  the canvas (`null` not permitted).
     */
    constructor(canvas: Canvas) {
        Logger.debug { "SkijaGraphics2D(Canvas)" }
        init(canvas)
    }

    /**
     * Copy-constructor: creates a new instance with the given parent SkijaGraphics2D.
     * 
     * @param parent SkijaGraphics2D instance to copy (`null` not permitted).
     */
    private constructor(parent: SkiaGraphics2D) {
        Logger.debug { "SkijaGraphics2D(parent)" }
        nullNotPermitted(parent, "parent")

        this.canvas = parent.getCanvas()
        canvas!!

        this.setRenderingHints(parent.renderingHintsInternally)

        if (getRenderingHint(SkiaHints.KEY_FONT_MAPPING_FUNCTION) == null) {
            setRenderingHint(SkiaHints.KEY_FONT_MAPPING_FUNCTION, Function { s: String? -> FONT_MAPPING.get(s) })
        }
        this.clipImpl = parent.clipImpl
        this.backgroundImpl = parent.backgroundImpl
        this.skiaPaint = org.jetbrains.skia.Paint().apply {
            color = DEFAULT_PAINT.rgb
        }
        this.paint = parent.paint
        this.composite = parent.composite
        this.setStroke(parent.getStroke())
        this.font = parent.font
        this.setTransform(parent.transformInternally)

        // save the original clip settings so they can be restored later in setClip()
        this.restoreCount = this.canvas!!.save()
        Logger.debug { "restoreCount updated to ${this.restoreCount}" }
    }

    /**
     * Creates a new instance using an existing canvas.
     * 
     * @param canvas  the canvas (`null` not permitted).
     */
    private fun init(canvas: Canvas) {
        nullNotPermitted(canvas, "canvas")
        this.canvas = canvas

        if (getRenderingHint(SkiaHints.KEY_FONT_MAPPING_FUNCTION) == null) {
            setRenderingHint(SkiaHints.KEY_FONT_MAPPING_FUNCTION, Function { s: String? -> FONT_MAPPING.get(s) })
        }

        // use constants for quick initialization:
        this.backgroundImpl = DEFAULT_PAINT
        this.skiaPaint = org.jetbrains.skia.Paint().apply {
            color = DEFAULT_PAINT.rgb
        }
        this.paint = DEFAULT_PAINT
        setStroke(DEFAULT_STROKE)
        // use TYPEFACE_MAP cache:
        this.font = DEFAULT_FONT

        // save the original clip settings so they can be restored later in setClip()
        this.restoreCount = this.canvas!!.save()
        Logger.debug { "restoreCount updated to ${this.restoreCount}" }
    }

    private fun getCanvas(): Canvas {
        return this.canvas!!
    }

    /**
     * Returns the Skija surface that was created by this instance, or `null`.
     * 
     * @return The Skija surface (possibly `null`).
     */
    fun getSurface(): Surface {
        return this.surface!!
    }

    /**
     * Creates a Skija path from the outline of a Java2D shape.
     * 
     * @param shape  the shape (`null` not permitted).
     * 
     * @return A path.
     */
    private fun path(shape: Shape): Path {
        val p = Path() // TODO: reuse Path instances or not (async safety) ?

        val iterator: PathIterator = shape.getPathIterator(null)
        while (!iterator.isDone) {
            when (val segType: Int = iterator.currentSegment(coords)) {
                PathIterator.SEG_MOVETO -> {
                    Logger.debug { "SEG_MOVETO: (${coords[0]},${coords[1]})" }
                    p.moveTo(coords[0].toFloat(), coords[1].toFloat())
                }

                PathIterator.SEG_LINETO -> {
                    Logger.debug { "SEG_LINETO: (${coords[0]},${coords[1]})" }
                    p.lineTo(coords[0].toFloat(), coords[1].toFloat())
                }

                PathIterator.SEG_QUADTO -> {
                    Logger.debug { "SEG_QUADTO: (${coords[0]},${coords[1]} ${coords[2]},${coords[3]})" }
                    p.quadTo(
                        coords[0].toFloat(), coords[1].toFloat(),
                        coords[2].toFloat(), coords[3].toFloat()
                    )
                }

                PathIterator.SEG_CUBICTO -> {
                    Logger.debug { "SEG_CUBICTO: (${coords[0]},${coords[1]} ${coords[2]},${coords[3]} ${coords[4]},${coords[5]})" }
                    p.cubicTo(
                        coords[0].toFloat(), coords[1].toFloat(),
                        coords[2].toFloat(), coords[3].toFloat(),
                        coords[4].toFloat(), coords[5].toFloat()
                    )
                }

                PathIterator.SEG_CLOSE -> {
                    Logger.debug { "SEG_CLOSE: " }
                    p.closePath()
                }

                else -> throw RuntimeException("Unrecognised segment type " + segType)
            }
            iterator.next()
        }
        return p
    }

    /**
     * Draws the specified shape with the current `paint` and
     * `stroke`.  There is direct handling for `Line2D` and
     * `Rectangle2D`.  All other shapes are mapped to a `GeneralPath`
     * and then drawn (effectively as `Path2D` objects).
     * 
     * @param s  the shape (`null` not permitted).
     * 
     * @see .fill
     */
    override fun draw(s: Shape) {
        Logger.debug { "draw(Shape) : $s" }
        this.skiaPaint?.mode = PaintMode.STROKE
        if (s is Line2D) {
            val l: Line2D = s
            this.canvas!!.drawLine(
                l.getX1().toFloat(),
                l.getY1().toFloat(),
                l.getX2().toFloat(),
                l.getY2().toFloat(),
                this.skiaPaint!!
            )
        } else if (s is Rectangle2D) {
            val r: Rectangle2D = s
            if (r.width < 0.0 || r.height < 0.0) {
                return
            }
            this.canvas!!.drawRect(
                Rect.makeXYWH(
                    r.getX().toFloat(),
                    r.getY().toFloat(),
                    r.getWidth().toFloat(),
                    r.getHeight().toFloat()
                ), this.skiaPaint!!
            )
        } else if (s is Ellipse2D) {
            val e: Ellipse2D = s
            this.canvas!!.drawOval(
                Rect.makeXYWH(
                    e.minX.toFloat(),
                    e.minY.toFloat(),
                    e.width.toFloat(),
                    e.height.toFloat()
                ), this.skiaPaint!!
            )
        } else {
            path(s).use { p ->
                this.canvas!!.drawPath(p, this.skiaPaint!!)
            }
        }
    }

    /**
     * Fills the specified shape with the current `paint`.  There is
     * direct handling for `Rectangle2D`.
     * All other shapes are mapped to a path outline and then filled.
     * 
     * @param s  the shape (`null` not permitted).
     * 
     * @see .draw
     */
    override fun fill(s: Shape) {
        Logger.debug { "fill($s)" }
        this.skiaPaint!!.mode = PaintMode.FILL
        if (s is Rectangle2D) {
            val r: Rectangle2D = s
            if (r.width <= 0.0 || r.height <= 0.0) {
                return
            }
            this.canvas!!.drawRect(
                Rect.makeXYWH(
                    r.x.toFloat(),
                    r.y.toFloat(),
                    r.width.toFloat(),
                    r.height.toFloat()
                ), this.skiaPaint!!
            )
        } else if (s is Ellipse2D) {
            val e: Ellipse2D = s
            if (e.width <= 0.0 || e.height <= 0.0) {
                return
            }
            this.canvas!!.drawOval(
                Rect.makeXYWH(
                    e.minX.toFloat(),
                    e.minY.toFloat(),
                    e.width.toFloat(),
                    e.height.toFloat()
                ), this.skiaPaint!!
            )
        } else if (s is Path2D) {
            val p2d: Path2D = s

            path(s).use { p ->
                if (p2d.getWindingRule() == Path2D.WIND_EVEN_ODD) {
                    p.fillMode = PathFillMode.EVEN_ODD
                } else {
                    p.fillMode = PathFillMode.WINDING
                }
                this.canvas!!.drawPath(p, this.skiaPaint!!)
            }
        } else {
            path(s).use { p ->
                this.canvas!!.drawPath(p, this.skiaPaint!!)
            }
        }
    }

    /**
     * Draws an image with the specified transform. Note that the
     * `observer` is ignored in this implementation.
     * 
     * @param img  the image.
     * @param xform  the transform (`null` permitted).
     * @param obs  the image observer (ignored).
     * 
     * @return `true` if the image is drawn.
     */
    override fun drawImage(
        img: Image?, xform: AffineTransform?,
        obs: ImageObserver?
    ): Boolean {
        Logger.debug { "drawImage(Image, AffineTransform, ImageObserver)" }
        val savedTransform: AffineTransform = getTransform()
        if (xform != null) {
            transform(xform)
        }
        val result = drawImage(img, 0, 0, obs)
        if (xform != null) {
            setTransform(savedTransform)
        }
        return result
    }

    /**
     * Draws the image resulting from applying the `BufferedImageOp`
     * to the specified image at the location `(x, y)`.
     * 
     * @param img  the image.
     * @param op  the operation (`null` permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    override fun drawImage(img: BufferedImage?, op: BufferedImageOp?, x: Int, y: Int) {
        Logger.debug { "drawImage(BufferedImage, BufferedImageOp, $x, $y)" }
        var imageToDraw: BufferedImage? = img
        if (op != null) {
            imageToDraw = op.filter(img, null)
        }
        drawImage(imageToDraw, AffineTransform(1f, 0f, 0f, 1f, x.toFloat(), y.toFloat()), null)
    }

    /**
     * Draws the rendered image. When `img` is `null` this method
     * does nothing.
     * 
     * @param img  the image (`null` permitted).
     * @param xform  the transform.
     */
    override fun drawRenderedImage(img: RenderedImage?, xform: AffineTransform?) {
        Logger.debug { "drawRenderedImage(RenderedImage, AffineTransform)" }
        if (img == null) { // to match the behaviour specified in the JDK
            return
        }
        val bi: BufferedImage = convertRenderedImage(img)
        drawImage(bi, xform, null)
    }

    /**
     * Draws the renderable image.
     * 
     * @param img  the renderable image.
     * @param xform  the transform.
     */
    override fun drawRenderableImage(
        img: RenderableImage,
        xform: AffineTransform?
    ) {
        Logger.debug { "drawRenderableImage(RenderableImage, AffineTransform xform)" }
        val ri: RenderedImage? = img.createDefaultRendering()
        drawRenderedImage(ri, xform)
    }

    /**
     * Draws a string at `(x, y)`.  The start of the text at the
     * baseline level will be aligned with the `(x, y)` point.
     * 
     * @param str  the string (`null` not permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * 
     * @see .drawString
     */
    override fun drawString(str: String, x: Int, y: Int) {
        Logger.debug { "drawString($str, $x, $y)" }
        drawString(str, x.toFloat(), y.toFloat())
    }

    /**
     * Draws a string at `(x, y)`. The start of the text at the
     * baseline level will be aligned with the `(x, y)` point.
     * 
     * @param str  the string (`null` not permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    override fun drawString(str: String, x: Float, y: Float) {
        Logger.debug { "drawString($str, $x, $y)" }
        this.skiaPaint!!.mode = PaintMode.FILL
        this.canvas!!.drawString(str, x, y, this.skijaFont, this.skiaPaint!!)
    }

    /**
     * Draws a string of attributed characters at `(x, y)`.  The
     * call is delegated to
     * [.drawString].
     * 
     * @param iterator  an iterator for the characters.
     * @param x  the x-coordinate.
     * @param y  the x-coordinate.
     */
    override fun drawString(iterator: AttributedCharacterIterator, x: Int, y: Int) {
        Logger.debug { "drawString(AttributedCharacterIterator, $x, $y)" }
        drawString(iterator, x.toFloat(), y.toFloat())
    }

    /**
     * Draws a string of attributed characters at `(x, y)`.
     * 
     * @param iterator  an iterator over the characters (`null` not
     * permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    override fun drawString(
        iterator: AttributedCharacterIterator, x: Float,
        y: Float
    ) {
        Logger.debug { "drawString(AttributedCharacterIterator, $x, $y)" }
        val s: MutableSet<AttributedCharacterIterator.Attribute?> = iterator.getAllAttributeKeys()
        if (!s.isEmpty()) {
            val layout: TextLayout = TextLayout(iterator, this.fontRenderContextImpl)
            layout.draw(this, x, y)
        } else {
            val sb = sbStr // not thread-safe
            sb.setLength(0)
            iterator.first()
            var i: Int = iterator.getBeginIndex()
            while (i < iterator.getEndIndex()) {
                sb.append(iterator.current())
                i++
                iterator.next()
            }
            drawString(sb.toString(), x, y)
        }
    }

    /**
     * Draws the specified glyph vector at the location `(x, y)`.
     * 
     * @param g  the glyph vector (`null` not permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    override fun drawGlyphVector(g: GlyphVector, x: Float, y: Float) {
        Logger.debug { "drawGlyphVector(GlyphVector, $x, $y)" }
        fill(g.getOutline(x, y))
    }

    /**
     * Returns `true` if the rectangle (in device space) intersects
     * with the shape (the interior, if `onStroke` is `false`,
     * otherwise the stroked outline of the shape).
     * 
     * @param rect  a rectangle (in device space).
     * @param s the shape.
     * @param onStroke  test the stroked outline only?
     * 
     * @return A boolean.
     */
    override fun hit(rect: Rectangle, s: Shape, onStroke: Boolean): Boolean {
        Logger.debug { "hit(Rectangle, Shape, boolean)" }
        val ts: Shape
        if (onStroke) {
            ts = _createTransformedShape(this.stroke!!.createStrokedShape(s), false)
        } else {
            ts = _createTransformedShape(s, false)
        }
        if (!rect.bounds2D.intersects(ts.bounds2D)) {
            return false
        }
        // note: Area class is very slow (especially for rect):
        val a1 = Area(rect)
        val a2 = Area(ts)
        a1.intersect(a2)
        return !a1.isEmpty
    }

    /**
     * Sets the stroke that will be used to draw shapes.
     * 
     * @param s  the stroke (`null` not permitted).
     * 
     * @see .getStroke
     */
    override fun setStroke(s: Stroke) {
        nullNotPermitted(s, "s")
        Logger.debug { "setStroke($stroke)" }
        if (s === this.stroke) { // quick test, full equals test later
            return
        }
        if (stroke is BasicStroke) {
            val bs: BasicStroke = s as BasicStroke
            if (bs == this.stroke) {
                return  // no change
            }
            this.skiaPaint!!.strokeWidth = max(bs.getLineWidth().toDouble(), MIN_LINE_WIDTH).toFloat()
            this.skiaPaint!!.strokeCap = awtToSkiaLineCap(bs.endCap)
            this.skiaPaint!!.strokeJoin = awtToSkiaLineJoin(bs.getLineJoin())
            this.skiaPaint!!.strokeMiter = bs.getMiterLimit()

            val dashes: FloatArray? = bs.getDashArray()
            if (dashes != null) {
                try {
                    this.skiaPaint!!.pathEffect = PathEffect.makeDash(dashes, bs.dashPhase)
                } catch (re: RuntimeException) {
                    System.err.println("Unable to create skija paint for dashes: " + dashes.contentToString())
                    re.printStackTrace(System.err)
                    this.skiaPaint!!.pathEffect = null
                }
            } else {
                this.skiaPaint!!.pathEffect = null
            }
        }
        this.stroke = s
    }

    /**
     * Maps a line cap code from AWT to the corresponding Skija `PaintStrokeCap`
     * enum value.
     * 
     * @param c  the line cap code.
     * 
     * @return A Skija stroke cap value.
     */
    private fun awtToSkiaLineCap(c: Int): PaintStrokeCap {
        when (c) {
            BasicStroke.CAP_BUTT -> return PaintStrokeCap.BUTT
            BasicStroke.CAP_ROUND -> return PaintStrokeCap.ROUND
            BasicStroke.CAP_SQUARE -> return PaintStrokeCap.SQUARE
            else -> throw IllegalArgumentException("Unrecognised cap code: $c")
        }
    }

    /**
     * Maps a line join code from AWT to the corresponding Skija
     * `PaintStrokeJoin` enum value.
     * 
     * @param j  the line join code.
     * 
     * @return A Skija stroke join value.
     */
    private fun awtToSkiaLineJoin(j: Int): PaintStrokeJoin {
        when (j) {
            BasicStroke.JOIN_BEVEL -> return PaintStrokeJoin.BEVEL
            BasicStroke.JOIN_MITER -> return PaintStrokeJoin.MITER
            BasicStroke.JOIN_ROUND -> return PaintStrokeJoin.ROUND
            else -> throw IllegalArgumentException("Unrecognised join code: $j")
        }
    }

    /**
     * Maps a linear gradient paint cycle method from AWT to the corresponding Skija
     * `FilterTileMode` enum value.
     * 
     * @param method  the cycle method.
     * 
     * @return A Skija stroke join value.
     */
    private fun awtCycleMethodToSkiaFilterTileMode(method: MultipleGradientPaint.CycleMethod): FilterTileMode {
        return when (method) {
            MultipleGradientPaint.CycleMethod.NO_CYCLE -> FilterTileMode.CLAMP
            MultipleGradientPaint.CycleMethod.REPEAT -> FilterTileMode.REPEAT
            MultipleGradientPaint.CycleMethod.REFLECT -> FilterTileMode.MIRROR
        }
    }

    /**
     * Returns the current value for the specified hint.  Note that all hints
     * are currently ignored in this implementation.
     * 
     * @param hintKey  the hint key (`null` permitted, but the
     * result will be `null` also in that case).
     * 
     * @return The current value for the specified hint
     * (possibly `null`).
     * 
     * @see .setRenderingHint
     */
    override fun getRenderingHint(hintKey: RenderingHints.Key): Any? {
        Logger.debug { "getRenderingHint($hintKey)" }
        return this.hints[hintKey]
    }

    /**
     * Sets the value for a hint.  See the `FXHints` class for
     * information about the hints that can be used with this implementation.
     * 
     * @param hintKey  the hint key (`null` not permitted).
     * @param hintValue  the hint value.
     * 
     * @see .getRenderingHint
     */
    override fun setRenderingHint(hintKey: RenderingHints.Key, hintValue: Any) {
        Logger.debug { "setRenderingHint($hintKey, $hintValue)" }
        this.hints[hintKey] = hintValue
    }

    /**
     * Sets the rendering hints to the specified collection.
     * 
     * @param hints  the new set of hints (`null` not permitted).
     * 
     * @see .getRenderingHints
     */
    override fun setRenderingHints(hints: MutableMap<*, *>?) {
        Logger.debug { "setRenderingHints(Map<?, ?>): $hints" }
        this.hints.clear()
        if (hints != null) {
            this.hints.putAll(hints)
        }
    }

    /**
     * Adds all the supplied rendering hints.
     * 
     * @param hints  the hints (`null` not permitted).
     */
    override fun addRenderingHints(hints: MutableMap<*, *>?) {
        Logger.debug { "addRenderingHints(Map<?, ?>): $hints" }
        if (hints != null) {
            this.hints.putAll(hints)
        }
    }

    /**
     * Returns a copy of the rendering hints.  Modifying the returned copy
     * will have no impact on the state of this `Graphics2D`
     * instance.
     *
     * @return The rendering hints (never `null`).
     *
     * @see .setRenderingHints
     */
    override fun getRenderingHints(): RenderingHints? {
        Logger.debug { "getRenderingHints()" }
        return this.hints.clone() as RenderingHints?

    }

    private val renderingHintsInternally: RenderingHints
        get() {
            Logger.debug { "getRenderingHintsInternally()" }
            return this.hints
        }

    /**
     * Applies the translation `(tx, ty)`.  This call is delegated
     * to [.translate].
     * 
     * @param tx  the x-translation.
     * @param ty  the y-translation.
     * 
     * @see .translate
     */
    override fun translate(tx: Int, ty: Int) {
        Logger.debug { "translate($tx, $ty)" }
        translate(tx.toDouble(), ty.toDouble())
    }

    /**
     * Applies the translation `(tx, ty)`.
     * 
     * @param tx  the x-translation.
     * @param ty  the y-translation.
     */
    override fun translate(tx: Double, ty: Double) {
        Logger.debug { "translate($tx, $ty)" }
        this.transform.translate(tx, ty)
        this.canvas!!.translate(tx.toFloat(), ty.toFloat())
    }

    /**
     * Applies a rotation (anti-clockwise) about `(0, 0)`.
     * 
     * @param theta  the rotation angle (in radians).
     */
    override fun rotate(theta: Double) {
        Logger.debug { "rotate($theta)" }
        this.transform.rotate(theta)
        this.canvas!!.rotate(Math.toDegrees(theta).toFloat())
    }

    /**
     * Applies a rotation (anti-clockwise) about `(x, y)`.
     * 
     * @param theta  the rotation angle (in radians).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    override fun rotate(theta: Double, x: Double, y: Double) {
        Logger.debug { "rotate($theta, $x, $y)" }
        translate(x, y)
        rotate(theta)
        translate(-x, -y)
    }

    /**
     * Applies a scale transformation.
     * 
     * @param sx  the x-scaling factor.
     * @param sy  the y-scaling factor.
     */
    override fun scale(sx: Double, sy: Double) {
        Logger.debug { "scale($sx, $sy)" }
        this.transform.scale(sx, sy)
        this.canvas!!.scale(sx.toFloat(), sy.toFloat())
    }

    /**
     * Applies a shear transformation. This is equivalent to the following
     * call to the `transform` method:
     * <br></br><br></br>
     *  * 
     * `transform(AffineTransform.getShearInstance(shx, shy));`
     * 
     * 
     * @param shx  the x-shear factor.
     * @param shy  the y-shear factor.
     */
    override fun shear(shx: Double, shy: Double) {
        Logger.debug { "shear($shx, $shy)" }
        this.transform.shear(shx, shy)
        this.canvas!!.skew(shx.toFloat(), shy.toFloat())
    }

    /**
     * Applies this transform to the existing transform by concatenating it.
     * 
     * @param t  the transform (`null` not permitted).
     */
    override fun transform(t: AffineTransform?) {
        Logger.debug { "transform(AffineTransform) : $t" }
        val tx: AffineTransform = getTransform()
        tx.concatenate(t)
        setTransform(tx)
    }

    /**
     * Returns a copy of the current transform.
     * 
     * @return A copy of the current transform (never `null`).
     * 
     * @see .setTransform
     */
    override fun getTransform(): AffineTransform {
        Logger.debug { "getTransform()" }
        return this.transform.clone() as AffineTransform
    }

    private val transformInternally: AffineTransform
        get() {
            Logger.debug { "getTransformInternally()" }
            return this.transform
        }

    /**
     * Sets the transform.
     * 
     * @param t  the new transform (`null` permitted, resets to the
     * identity transform).
     * 
     * @see .getTransform
     */
    override fun setTransform(t: AffineTransform?) {
        var t: AffineTransform? = t
        Logger.debug { "setTransform($t)" }
        if (t == null) {
            t = AffineTransform()
            this.transform = t
        } else {
            this.transform = AffineTransform(t)
        }
        val m33 = Matrix33(
            t.scaleX.toFloat(), t.shearX.toFloat(), t.translateX.toFloat(),
            t.shearY.toFloat(), t.scaleY.toFloat(), t.translateY.toFloat(), 0f, 0f, 1f
        )
        this.canvas!!.setMatrix(m33)
    }

    override fun setPaint(paint: Paint?) {
        Logger.debug { "setPaint($paint)" }
        if (paint == null) {
            return
        }
        if (paintsAreEqual(paint, this.awtPaint)) {
            return
        }
        this.awtPaint = paint
        when (paint) {
            is Color -> {
                val c = paint
                this.color = c
                this.skiaPaint!!.shader = Shader.makeColor(c.rgb)
            }

            is LinearGradientPaint -> {
                val lgp: LinearGradientPaint = paint
                val x0 = lgp.startPoint.x.toFloat()
                val y0 = lgp.startPoint.y.toFloat()
                val x1 = lgp.endPoint.x.toFloat()
                val y1 = lgp.endPoint.y.toFloat()

                val colors = IntArray(lgp.getColors().size)
                for (i in lgp.getColors().indices) {
                    colors[i] = lgp.getColors()[i].rgb
                }
                val fractions: FloatArray = lgp.getFractions()
                val gs: GradientStyle =
                    GradientStyle.DEFAULT.withTileMode(awtCycleMethodToSkiaFilterTileMode(lgp.getCycleMethod()))
                this.skiaPaint!!.shader = Shader.makeLinearGradient(x0, y0, x1, y1, colors, fractions, gs)
            }

            is RadialGradientPaint -> {
                val rgp: RadialGradientPaint = paint
                val x = rgp.centerPoint.x.toFloat()
                val y = rgp.centerPoint.y.toFloat()

                val colors = IntArray(rgp.getColors().size)
                for (i in rgp.getColors().indices) {
                    colors[i] = rgp.getColors()[i].getRGB()
                }
                val gs: GradientStyle =
                    GradientStyle.DEFAULT.withTileMode(awtCycleMethodToSkiaFilterTileMode(rgp.getCycleMethod()))
                val fx = rgp.focusPoint.getX().toFloat()
                val fy = rgp.focusPoint.getY().toFloat()

                val shader: Shader?
                if (rgp.focusPoint == rgp.centerPoint) {
                    shader = Shader.makeRadialGradient(x, y, rgp.radius, colors, rgp.getFractions(), gs)
                } else {
                    shader = Shader.makeTwoPointConicalGradient(
                        fx,
                        fy,
                        0f,
                        x,
                        y,
                        rgp.radius,
                        colors,
                        rgp.getFractions(),
                        gs
                    )
                }
                this.skiaPaint!!.shader = shader
            }

            is GradientPaint -> {
                val gp: GradientPaint = paint
                val x1 = gp.point1.x.toFloat()
                val y1 = gp.point1.y.toFloat()
                val x2 = gp.point2.x.toFloat()
                val y2 = gp.point2.y.toFloat()

                val colors = intArrayOf(gp.getColor1().rgb, gp.getColor2().rgb)
                val gs: GradientStyle = if (gp.isCyclic)
                    GradientStyle.DEFAULT.withTileMode(FilterTileMode.MIRROR)
                else
                    GradientStyle.DEFAULT

                this.skiaPaint!!.shader = Shader.makeLinearGradient(
                    x1,
                    y1,
                    x2,
                    y2,
                    colors,
                    null as FloatArray?,
                    gs
                )
            }
        }
    }

    override fun getPaint(): Paint? {
        return this.awtPaint
    }

    /**
     * Returns the current stroke (this attribute is used when drawing shapes).
     * 
     * @return The current stroke (never `null`).
     * 
     * @see .setStroke
     */
    override fun getStroke(): Stroke {
        return this.stroke!!
    }

    /**
     * Creates a new graphics object that is a copy of this graphics object.
     * 
     * @return A new graphics object.
     */
    override fun create(): Graphics {
        Logger.debug { "create()" }
        // use special copy-constructor:
        return SkiaGraphics2D(this)
    }

    override fun create(x: Int, y: Int, width: Int, height: Int): Graphics? {
        Logger.debug { "create($x, $y, $width, $height)" }
        return super.create(x, y, width, height)
    }

    /**
     * Returns the foreground color.  This method exists for backwards
     * compatibility in AWT, you should use the [.getPaint] method.
     * This attribute is updated by the [.setColor]
     * method, and also by the [.setPaint] method if
     * a `Color` instance is passed to the method.
     * 
     * @return The foreground color (never `null`).
     * 
     * @see .getPaint
     */
    override fun getColor(): Color? {
        return this.color
    }

    /**
     * Sets the foreground color.  This method exists for backwards
     * compatibility in AWT, you should use the
     * [.setPaint] method.
     * 
     * @param c  the color (`null` permitted but ignored).
     * 
     * @see .setPaint
     */
    override fun setColor(c: Color?) {
        Logger.debug { "setColor(Color) : $c" }
        if (c == null || c == this.color) {
            return
        }
        this.color = c
        this.paint = c
    }

    /**
     * Not implemented - the method does nothing.
     */
    override fun setPaintMode() {
        // not implemented
    }

    /**
     * Not implemented - the method does nothing.
     */
    override fun setXORMode(c1: Color?) {
        // not implemented
    }

    /**
     * Returns the current font used for drawing text.
     *
     * @return The current font (never `null`).
     *
     * @see .setFont
     */
    override fun getFont(): Font {
        return this.awtFont!!
    }

    /**
     * Sets the font to be used for drawing text.
     *
     * @param font  the font (`null` is permitted but ignored).
     *
     * @see .getFont
     */
    override fun setFont(font: Font?) {
        Logger.debug { "setFont($font)" }
        if (font == null) {
            return
        }
        this.awtFont = font

        var fontName: String? = font.getName()
        // check if there is a font name mapping to apply
        val fm =
            getRenderingHint(SkiaHints.KEY_FONT_MAPPING_FUNCTION) as Function<String?, String?>?
        if (fm != null) {
            val mappedFontName = fm.apply(fontName)
            if (mappedFontName != null) {
                Logger.debug { "Mapped font name is $mappedFontName" }
                fontName = mappedFontName
            }
        }
        val style: FontStyle = awtFontStyleToSkijaFontStyle(font.getStyle())
        val key = TypefaceKey(fontName!!, style)

        this.typeface = TYPEFACE_MAP.get(key)
        if (this.typeface == null) {
            Logger.debug { "Typeface.makeFromName($fontName style=$style)" }
            this.typeface = FontMgr.default.legacyMakeTypeface(fontName, style)
            TYPEFACE_MAP[key] = this.typeface
        }
        this.skijaFont = org.jetbrains.skia.Font(this.typeface, font.size.toFloat())

    }

    private fun awtFontStyleToSkijaFontStyle(style: Int): FontStyle {
        when (style) {
            Font.PLAIN -> return FontStyle.NORMAL
            Font.BOLD -> return FontStyle.BOLD
            Font.ITALIC -> return FontStyle.ITALIC
            Font.BOLD + Font.ITALIC -> return FontStyle.BOLD_ITALIC
            else -> return FontStyle.NORMAL
        }
    }

    /**
     * Returns the font metrics for the specified font.
     * 
     * @param f  the font.
     * 
     * @return The font metrics.
     */
    override fun getFontMetrics(f: Font?): FontMetrics {
        return SkiaFontMetrics(this.skijaFont!!, this.awtFont)
    }

    /**
     * Returns the bounds of the user clipping region.
     *
     * @return The clip bounds (possibly `null`).
     *
     * @see .getClip
     */
    override fun getClipBounds(): Rectangle? {
        if (this.clipImpl == null) {
            return null
        }
        return this.clipInternally!!.bounds

    }

    /**
     * Returns the user clipping region.  The initial default value is
     * `null`.
     * 
     * @return The user clipping region (possibly `null`).
     * 
     * @see .setClip
     */
    override fun getClip(): Shape? {
        Logger.debug { "getClip()" }
        if (this.clipImpl == null) {
            return null
        }
        return _inverseTransform(this.clipImpl, true)
    }

    private val clipInternally: Shape?
        get() {
            if (this.clipImpl == null) {
                return null
            }
            return _inverseTransform(this.clipImpl, false)
        }

    /**
     * Clips to the intersection of the current clipping region and the
     * specified rectangle.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     */
    override fun clipRect(x: Int, y: Int, width: Int, height: Int) {
        Logger.debug { "clipRect($x, $y, $width, $height)" }
        clip(rect(x, y, width, height))
    }

    /**
     * Sets the user clipping region to the specified rectangle.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @see .getClip
     */
    override fun setClip(x: Int, y: Int, width: Int, height: Int) {
        Logger.debug { "setClip($x, $y, $width, $height)" }
        setClip(rect(x, y, width, height))
    }

    /**
     * Sets the user clipping region.
     * 
     * @param shape  the new user clipping region (`null` permitted).
     * 
     * @see .getClip
     */
    override fun setClip(shape: Shape?) {
        setClip(shape, true)
    }

    private fun setClip(shape: Shape?, clone: Boolean) {
        Logger.debug { "setClip($shape)" }
        // a new clip is being set, so first restore the original clip (and save
        // it again for future restores)
        this.canvas!!.restoreToCount(this.restoreCount)
        this.restoreCount = this.canvas!!.save()
        // restoring the clip might also reset the transform, so reapply it
        setTransform(getTransform())
        // null is handled fine here...
        this.clipImpl = _createTransformedShape(shape!!, clone) // device space
        // now apply on the Skija canvas
        this.canvas!!.clipPath(path(shape))
    }

    /**
     * Clips to the intersection of the current clipping region and the
     * specified shape.
     * 
     * According to the Oracle API specification, this method will accept a
     * `null` argument, but there is an open bug report (since 2004)
     * that suggests this is wrong:
     * 
     * 
     * [
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6206189](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6206189)
     * 
     * @param s  the clip shape (`null` not permitted).
     */
    override fun clip(s: Shape) {
        var s = s
        Logger.debug { "clip($s)" }
        if (s is Line2D) {
            s = s.bounds2D
        }
        if (this.clipImpl == null) {
            setClip(s)
            return
        }
        val clipUser = this.clipInternally
        val clipNew: Shape
        if (!s.intersects(clipUser!!.bounds2D)) {
            clipNew = Rectangle2D.Double()
        } else {
            // note: Area class is very slow (especially for rectangles)
            val a1 = Area(s)
            val a2 = Area(clipUser)
            a1.intersect(a2)
            clipNew = Path2D.Double(a1)
        }
        setClip(clipNew, false) // in user space
    }

    /**
     * Not yet implemented.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width of the area.
     * @param height  the height of the area.
     * @param dx  the delta x.
     * @param dy  the delta y.
     */
    override fun copyArea(x: Int, y: Int, width: Int, height: Int, dx: Int, dy: Int) {
        // FIXME: implement this, low priority
        Logger.error { "copyArea($x, $y, $width, $height, $dx, $dy) - NOT IMPLEMENTED" }
    }

    /**
     * Draws a line from `(x1, y1)` to `(x2, y2)` using
     * the current `paint` and `stroke`.
     * 
     * @param x1  the x-coordinate of the start point.
     * @param y1  the y-coordinate of the start point.
     * @param x2  the x-coordinate of the end point.
     * @param y2  the x-coordinate of the end point.
     */
    override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) {
        Logger.debug { "drawLine()" }
        if (this.line == null) {
            this.line = Line2D.Double(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        } else {
            this.line!!.setLine(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        }
        draw(this.line!!)
    }

    /**
     * Fills the specified rectangle with the current `paint`.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the rectangle width.
     * @param height  the rectangle height.
     */
    override fun fillRect(x: Int, y: Int, width: Int, height: Int) {
        Logger.debug { "fillRect($x, $y, $width, $height)" }
        fill(rect(x, y, width, height))
    }

    /**
     * Clears the specified rectangle by filling it with the current
     * background color.  If the background color is `null`, this
     * method will do nothing.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @see .getBackground
     */
    override fun clearRect(x: Int, y: Int, width: Int, height: Int) {
        Logger.debug { "clearRect($x, $y, $width, $height)" }
        if (this.backgroundImpl == null) {
            return  // we can't do anything
        }
        val saved = this.paint
        this.paint = this.backgroundImpl
        fillRect(x, y, width, height)
        this.paint = saved
    }

    /**
     * Sets the attributes of the reusable [Rectangle2D] object that is
     * used by the [SkijaGraphics2D.drawRect] and
     * [SkiaGraphics2D.fillRect] methods.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @return A rectangle (never `null`).
     */
    private fun rect(x: Int, y: Int, width: Int, height: Int): Rectangle2D {
        if (this.rect == null) {
            this.rect = Rectangle2D.Double(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        } else {
            this.rect!!.setRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        }
        return this.rect!!
    }

    /**
     * Draws a rectangle with rounded corners using the current
     * `paint` and `stroke`.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param arcWidth  the arc-width.
     * @param arcHeight  the arc-height.
     * 
     * @see .fillRoundRect
     */
    override fun drawRoundRect(
        x: Int, y: Int, width: Int, height: Int,
        arcWidth: Int, arcHeight: Int
    ) {
        Logger.debug { "drawRoundRect($x, $y, $width, $height, $arcWidth, $arcHeight)" }
        draw(roundRect(x, y, width, height, arcWidth, arcHeight))
    }

    /**
     * Fills a rectangle with rounded corners using the current `paint`.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param arcWidth  the arc-width.
     * @param arcHeight  the arc-height.
     * 
     * @see .drawRoundRect
     */
    override fun fillRoundRect(
        x: Int, y: Int, width: Int, height: Int,
        arcWidth: Int, arcHeight: Int
    ) {
        Logger.debug { "fillRoundRect($x, $y, $width, $height, $arcWidth, $arcHeight)" }
        fill(roundRect(x, y, width, height, arcWidth, arcHeight))
    }

    /**
     * Sets the attributes of the reusable [RoundRectangle2D] object that
     * is used by the [.drawRoundRect] and
     * [.fillRoundRect] methods.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param arcWidth  the arc width.
     * @param arcHeight  the arc height.
     * 
     * @return A round rectangle (never `null`).
     */
    private fun roundRect(
        x: Int, y: Int, width: Int, height: Int,
        arcWidth: Int, arcHeight: Int
    ): RoundRectangle2D {
        if (this.roundRect == null) {
            this.roundRect = RoundRectangle2D.Double(
                x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(),
                arcWidth.toDouble(), arcHeight.toDouble()
            )
        } else {
            this.roundRect!!.setRoundRect(
                x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(),
                arcWidth.toDouble(), arcHeight.toDouble()
            )
        }
        return this.roundRect!!
    }

    /**
     * Draws an oval framed by the rectangle `(x, y, width, height)`
     * using the current `paint` and `stroke`.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @see .fillOval
     */
    override fun drawOval(x: Int, y: Int, width: Int, height: Int) {
        Logger.debug { "drawOval($x, $y, $width, $height)" }
        draw(oval(x, y, width, height))
    }

    /**
     * Fills an oval framed by the rectangle `(x, y, width, height)`.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @see .drawOval
     */
    override fun fillOval(x: Int, y: Int, width: Int, height: Int) {
        Logger.debug { "fillOval($x, $y, $width, $height)" }
        fill(oval(x, y, width, height))
    }

    /**
     * Returns an [Ellipse2D] object that may be reused (so this instance
     * should be used for short term operations only). See the
     * [.drawOval] and
     * [.fillOval] methods for usage.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * 
     * @return An oval shape (never `null`).
     */
    private fun oval(x: Int, y: Int, width: Int, height: Int): Ellipse2D {
        if (this.oval == null) {
            this.oval = Ellipse2D.Double(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        } else {
            this.oval!!.setFrame(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        }
        return this.oval!!
    }

    /**
     * Draws an arc contained within the rectangle
     * `(x, y, width, height)`, starting at `startAngle`
     * and continuing through `arcAngle` degrees using
     * the current `paint` and `stroke`.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param startAngle  the start angle in degrees, 0 = 3 o'clock.
     * @param arcAngle  the angle (anticlockwise) in degrees.
     * 
     * @see .fillArc
     */
    override fun drawArc(
        x: Int, y: Int, width: Int, height: Int, startAngle: Int,
        arcAngle: Int
    ) {
        Logger.debug { "drawArc($x, $y, $width, $height, $startAngle, $arcAngle)" }
        draw(arc(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN))
    }

    /**
     * Fills an arc contained within the rectangle
     * `(x, y, width, height)`, starting at `startAngle`
     * and continuing through `arcAngle` degrees, using
     * the current `paint`.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param startAngle  the start angle in degrees, 0 = 3 o'clock.
     * @param arcAngle  the angle (anticlockwise) in degrees.
     * 
     * @see .drawArc
     */
    override fun fillArc(
        x: Int, y: Int, width: Int, height: Int, startAngle: Int,
        arcAngle: Int
    ) {
        Logger.debug { "fillArc($x, $y, $width, $height, $startAngle, $arcAngle)" }
        fill(arc(x, y, width, height, startAngle, arcAngle, Arc2D.PIE))
    }

    /**
     * Sets the attributes of the reusable [Arc2D] object that is used by
     * [.drawArc] and
     * [.fillArc] methods.
     * 
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param startAngle  the start angle in degrees, 0 = 3 o'clock.
     * @param arcAngle  the angle (anticlockwise) in degrees.
     * @param arcStyle  the arc style.
     * 
     * @return An arc (never `null`).
     */
    private fun arc(
        x: Int, y: Int, width: Int, height: Int, startAngle: Int,
        arcAngle: Int, arcStyle: Int
    ): Arc2D {
        if (this.arc == null) {
            this.arc = Arc2D.Double(
                x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), startAngle.toDouble(),
                arcAngle.toDouble(), arcStyle
            )
        } else {
            this.arc!!.setArc(
                x.toDouble(),
                y.toDouble(),
                width.toDouble(),
                height.toDouble(),
                startAngle.toDouble(),
                arcAngle.toDouble(),
                arcStyle
            )
        }
        return this.arc!!
    }

    /**
     * Draws the specified multi-segment line using the current
     * `paint` and `stroke`.
     * 
     * @param xPoints  the x-points.
     * @param yPoints  the y-points.
     * @param nPoints  the number of points to use for the polyline.
     */
    override fun drawPolyline(xPoints: IntArray, yPoints: IntArray, nPoints: Int) {
        Logger.debug { "drawPolyline(int[], int[], int)" }
        val p: GeneralPath = createPolygon(xPoints, yPoints, nPoints, false)
        draw(p)
    }

    /**
     * Draws the specified polygon using the current `paint` and
     * `stroke`.
     * 
     * @param xPoints  the x-points.
     * @param yPoints  the y-points.
     * @param nPoints  the number of points to use for the polygon.
     * 
     * @see .fillPolygon
     */
    override fun drawPolygon(xPoints: IntArray, yPoints: IntArray, nPoints: Int) {
        Logger.debug { "drawPolygon(int[], int[], int)" }
        val p: GeneralPath = createPolygon(xPoints, yPoints, nPoints, true)
        draw(p)
    }

    /**
     * Fills the specified polygon using the current `paint`.
     * 
     * @param xPoints  the x-points.
     * @param yPoints  the y-points.
     * @param nPoints  the number of points to use for the polygon.
     * 
     * @see .drawPolygon
     */
    override fun fillPolygon(xPoints: IntArray, yPoints: IntArray, nPoints: Int) {
        Logger.debug { "fillPolygon(int[], int[], int)" }
        val p: GeneralPath = createPolygon(xPoints, yPoints, nPoints, true)
        fill(p)
    }

    /**
     * Creates a polygon from the specified `x` and
     * `y` coordinate arrays.
     * 
     * @param xPoints  the x-points.
     * @param yPoints  the y-points.
     * @param nPoints  the number of points to use for the polyline.
     * @param close  closed?
     * 
     * @return A polygon.
     */
    fun createPolygon(
        xPoints: IntArray, yPoints: IntArray,
        nPoints: Int, close: Boolean
    ): GeneralPath {
        Logger.debug { "createPolygon(int[], int[], int, boolean)" }
        val p: GeneralPath = GeneralPath()
        p.moveTo(xPoints[0].toFloat(), yPoints[0].toFloat())
        for (i in 1..<nPoints) {
            p.lineTo(xPoints[i].toFloat(), yPoints[i].toFloat())
        }
        if (close) {
            p.closePath()
        }
        return p
    }

    /**
     * Draws an image at the location `(x, y)`.  Note that the
     * `observer` is ignored.
     * 
     * @param img  the image (`null` permitted...method will do nothing).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param observer  ignored.
     * 
     * @return `true` if there is no more drawing to be done.
     */
    override fun drawImage(img: Image?, x: Int, y: Int, observer: ImageObserver?): Boolean {
        Logger.debug { "drawImage(Image, $x, $y, ImageObserver)" }
        if (img == null) {
            return true
        }
        val w = img.getWidth(observer)
        if (w < 0) {
            return false
        }
        val h = img.getHeight(observer)
        if (h < 0) {
            return false
        }
        return drawImage(img, x, y, w, h, observer)
    }

    /**
     * Draws the image into the rectangle defined by `(x, y, w, h)`.
     * Note that the `observer` is ignored (it is not useful in this
     * context).
     * 
     * @param img  the image (`null` permitted...draws nothing).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param observer  ignored.
     * 
     * @return `true` if there is no more drawing to be done.
     */
    override fun drawImage(img: Image?, x: Int, y: Int, width: Int, height: Int, observer: ImageObserver?): Boolean {
        Logger.debug { "drawImage(Image, $x, $y, $width, $height, ImageObserver)" }
        val buffered: BufferedImage
        if ((img is BufferedImage) && (img as BufferedImage).getType() == BufferedImage.TYPE_INT_ARGB) {
            buffered = img as BufferedImage
        } else {
            buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val g2: Graphics2D = buffered.createGraphics()
            g2.drawImage(img, 0, 0, width, height, null)
            g2.dispose()
        }
        convertToSkijaImage(buffered).use { skijaImage ->
            this.canvas!!.drawImageRect(skijaImage, Rect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()))
        }
        return true
    }

    /**
     * Draws an image at the location `(x, y)`.  Note that the
     * `observer` is ignored.
     * 
     * @param img  the image (`null` permitted...draws nothing).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param bgcolor  the background color (`null` permitted).
     * @param observer  ignored.
     * 
     * @return `true` if there is no more drawing to be done.
     */
    override fun drawImage(
        img: Image?, x: Int, y: Int, bgcolor: Color?,
        observer: ImageObserver?
    ): Boolean {
        Logger.debug { "drawImage(Image, $x, $y, Color, ImageObserver)" }
        if (img == null) {
            return true
        }
        val w = img.getWidth(null)
        if (w < 0) {
            return false
        }
        val h = img.getHeight(null)
        if (h < 0) {
            return false
        }
        return drawImage(img, x, y, w, h, bgcolor, observer)
    }

    override fun drawImage(
        img: Image?,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        bgcolor: Color?,
        observer: ImageObserver?
    ): Boolean {
        Logger.debug { "drawImage(Image, $x, $y, $width, $height, Color, ImageObserver)" }
        val saved = this.paint
        this.paint = bgcolor
        fillRect(x, y, width, height)
        this.paint = saved
        return drawImage(img, x, y, width, height, observer)
    }

    /**
     * Draws part of an image (defined by the source rectangle
     * `(sx1, sy1, sx2, sy2)`) into the destination rectangle
     * `(dx1, dy1, dx2, dy2)`.  Note that the `observer`
     * is ignored in this implementation.
     * 
     * @param img  the image.
     * @param dx1  the x-coordinate for the top left of the destination.
     * @param dy1  the y-coordinate for the top left of the destination.
     * @param dx2  the x-coordinate for the bottom right of the destination.
     * @param dy2  the y-coordinate for the bottom right of the destination.
     * @param sx1  the x-coordinate for the top left of the source.
     * @param sy1  the y-coordinate for the top left of the source.
     * @param sx2  the x-coordinate for the bottom right of the source.
     * @param sy2  the y-coordinate for the bottom right of the source.
     * 
     * @return `true` if the image is drawn.
     */
    override fun drawImage(
        img: Image?,
        dx1: Int,
        dy1: Int,
        dx2: Int,
        dy2: Int,
        sx1: Int,
        sy1: Int,
        sx2: Int,
        sy2: Int,
        observer: ImageObserver?
    ): Boolean {
        Logger.debug { "drawImage(Image, $dx1, $dy1, $dx2, $dy2, $sx1, $sy1, $sx2, $sy2, ImageObserver)" }
        val w = dx2 - dx1
        val h = dy2 - dy1
        val img2: BufferedImage = BufferedImage(
            w, h,
            BufferedImage.TYPE_INT_ARGB
        )
        val g2: Graphics2D = img2.createGraphics()
        g2.drawImage(img, 0, 0, w, h, sx1, sy1, sx2, sy2, null)
        return drawImage(img2, dx1, dy1, null)
    }

    /**
     * Draws part of an image (defined by the source rectangle
     * `(sx1, sy1, sx2, sy2)`) into the destination rectangle
     * `(dx1, dy1, dx2, dy2)`.  The destination rectangle is first
     * cleared by filling it with the specified `bgcolor`. Note that
     * the `observer` is ignored.
     * 
     * @param img  the image.
     * @param dx1  the x-coordinate for the top left of the destination.
     * @param dy1  the y-coordinate for the top left of the destination.
     * @param dx2  the x-coordinate for the bottom right of the destination.
     * @param dy2  the y-coordinate for the bottom right of the destination.
     * @param sx1 the x-coordinate for the top left of the source.
     * @param sy1 the y-coordinate for the top left of the source.
     * @param sx2 the x-coordinate for the bottom right of the source.
     * @param sy2 the y-coordinate for the bottom right of the source.
     * @param bgcolor  the background color (`null` permitted).
     * @param observer  ignored.
     * 
     * @return `true` if the image is drawn.
     */
    override fun drawImage(
        img: Image?,
        dx1: Int,
        dy1: Int,
        dx2: Int,
        dy2: Int,
        sx1: Int,
        sy1: Int,
        sx2: Int,
        sy2: Int,
        bgcolor: Color?,
        observer: ImageObserver?
    ): Boolean {
        Logger.debug { "drawImage(Image, $dx1, $dy1, $dx2, $dy2, $sx1, $sy1, $sx2, $sy2, Color, ImageObserver)" }
        val saved = this.paint
        this.paint = bgcolor
        fillRect(dx1, dy1, dx2 - dx1, dy2 - dy1)
        this.paint = saved
        return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer)
    }

    public var onDispose = {}

    /**
     * This method does nothing.
     */
    override fun dispose() {
        Logger.debug { "dispose()" }
        dispatchIfNeeded { // Might be called from Finalized thread
            this.canvas!!.restoreToCount(this.restoreCount)
            onDispose()
        }
    }

    private fun _createTransformedShape(s: Shape, clone: Boolean): Shape {
        if (this.transform.isIdentity) {
            return (if (clone) _clone(s) else s)!!
        }
        return this.transform.createTransformedShape(s)
    }

    private fun _inverseTransform(s: Shape?, clone: Boolean): Shape? {
        if (this.transform.isIdentity) {
            return if (clone) _clone(s) else s
        }
        try {
            val inv: AffineTransform = this.transform.createInverse()
            return inv.createTransformedShape(s)
        } catch (nite: NoninvertibleTransformException) {
            // System.err.println("NoninvertibleTransformException: " + this.transform);
            return null
        }
    }

    private fun _clone(s: Shape?): Shape? {
        if (s == null) {
            return null
        }
        if (s is Rectangle2D) {
            val r: Rectangle2D = Rectangle2D.Double()
            r.setRect(s)
            return r
        }
        return Path2D.Double(s, null)
    }

//    override fun constrain(x: Int, y: Int, w: Int, h: Int) {
//        translate(x * 2, y * 2) // todo [pavel.sergeev] is this correct?
//        clipRect(0, 0, w, h)
//    }
//
//    override fun constrain(x: Int, y: Int, width: Int, height: Int, visibleRegion: Region?) {
//        constrain(x, y, width, height)
//    }

    companion object {
        /** The line width to use when a BasicStroke with line width = 0.0 is applied.  */
        private const val MIN_LINE_WIDTH = 0.1

        /** default paint  */
        private val DEFAULT_PAINT: Color = Color.BLACK

        /** default stroke  */
        private val DEFAULT_STROKE: Stroke = BasicStroke(1.0f)

        /** default font  */
        private val DEFAULT_FONT = Font("SansSerif", Font.PLAIN, 12)

        /** font mapping  */
        private val FONT_MAPPING: MutableMap<String?, String?> = createDefaultFontMap()

        private val TYPEFACE_MAP: MutableMap<TypefaceKey?, Typeface?> =
            Collections.synchronizedMap<TypefaceKey?, Typeface?>(HashMap<TypefaceKey?, Typeface?>(16))

        /**
         * The font render context.  The fractional metrics flag solves the glyph
         * positioning issue identified by Christoph Nahr:
         * http://news.kynosarges.org/2014/06/28/glyph-positioning-in-jfreesvg-orsonpdf/
         */
        private val DEFAULT_FONT_RENDER_CONTEXT: FontRenderContext = FontRenderContext(
            null, false, true
        )

        /**
         * Throws an `IllegalArgumentException` if `arg` is
         * `null`.
         * 
         * @param arg  the argument to check.
         * @param name  the name of the argument (to display in the exception
         * message).
         */
        private fun nullNotPermitted(arg: Any, name: String?) {
            requireNotNull(arg) { "Null '" + name + "' argument." }
        }

        /**
         * Creates a map containing default mappings from the Java logical font names
         * to suitable physical font names.  This is not a particularly great solution,
         * but so far I don't see a better alternative.
         * 
         * @return A map.
         */
        fun createDefaultFontMap(): MutableMap<String?, String?> {
            val result: MutableMap<String?, String?> = HashMap<String?, String?>(8)
            val os = System.getProperty("os.name").lowercase(Locale.getDefault())
            if (os.contains("win")) { // Windows
                result.put(Font.MONOSPACED, "Courier New")
                result.put(Font.SANS_SERIF, "Arial")
                result.put(Font.SERIF, "Times New Roman")
            } else if (os.contains("mac")) { // MacOS
                result.put(Font.MONOSPACED, "Courier New")
                result.put(Font.SANS_SERIF, "Helvetica")
                result.put(Font.SERIF, "Times New Roman")
            } else { // assume Linux
                result.put(Font.MONOSPACED, "Courier New")
                result.put(Font.SANS_SERIF, "Arial")
                result.put(Font.SERIF, "Times New Roman")
            }
            result[Font.DIALOG] = result[Font.SANS_SERIF]
            result[Font.DIALOG_INPUT] = result[Font.SANS_SERIF]
            return result
        }

        /**
         * Returns `true` if the two `Paint` objects are equal
         * OR both `null`.  This method handles
         * `GradientPaint`, `LinearGradientPaint`
         * and `RadialGradientPaint` as special cases, since those classes do
         * not override the `equals()` method.
         * 
         * @param p1  paint 1 (`null` permitted).
         * @param p2  paint 2 (`null` permitted).
         * 
         * @return A boolean.
         */
        private fun paintsAreEqual(p1: Paint?, p2: Paint?): Boolean {
            if (p1 === p2) {
                return true
            }

            // handle cases where either or both arguments are null
            if (p1 == null) {
                return (p2 == null)
            }
            if (p2 == null) {
                return false
            }

            // handle cases...
            if (p1 is Color && p2 is Color) {
                return p1 == p2
            }
            if (p1 is GradientPaint && p2 is GradientPaint) {
                val gp1: GradientPaint = p1 as GradientPaint
                val gp2: GradientPaint = p2 as GradientPaint
                return gp1.getColor1() == gp2.getColor1()
                        && gp1.getColor2() == gp2.getColor2()
                        && gp1.getPoint1() == gp2.getPoint1()
                        && gp1.getPoint2() == gp2.getPoint2()
                        && gp1.isCyclic() == gp2.isCyclic() && gp1.getTransparency() == gp1.getTransparency()
            }
            if (p1 is LinearGradientPaint
                && p2 is LinearGradientPaint
            ) {
                val lgp1: LinearGradientPaint = p1 as LinearGradientPaint
                val lgp2: LinearGradientPaint = p2 as LinearGradientPaint
                return lgp1.getStartPoint() == lgp2.getStartPoint()
                        && lgp1.getEndPoint() == lgp2.getEndPoint()
                        && lgp1.getFractions().contentEquals(lgp2.getFractions()) && lgp1.getColors()
                    .contentEquals(lgp2.getColors()) && lgp1.getCycleMethod() == lgp2.getCycleMethod() && lgp1.getColorSpace() == lgp2.getColorSpace() && lgp1.getTransform() == lgp2.getTransform()
            }
            if (p1 is RadialGradientPaint
                && p2 is RadialGradientPaint
            ) {
                val rgp1: RadialGradientPaint = p1 as RadialGradientPaint
                val rgp2: RadialGradientPaint = p2 as RadialGradientPaint
                return rgp1.getCenterPoint() == rgp2.getCenterPoint()
                        && rgp1.getRadius() == rgp2.getRadius() && rgp1.getFocusPoint() == rgp2.getFocusPoint()
                        && rgp1.getFractions().contentEquals(rgp2.getFractions()) && rgp1.getColors()
                    .contentEquals(rgp2.getColors()) && rgp1.getCycleMethod() == rgp2.getCycleMethod() && rgp1.getColorSpace() == rgp2.getColorSpace() && rgp1.getTransform() == rgp2.getTransform()
            }
            return p1 == p2
        }

        /**
         * Converts a rendered image to a `BufferedImage`.  This utility
         * method has come from a forum post by Jim Moore at:
         * 
         * 
         * [
         * http://www.jguru.com/faq/view.jsp?EID=114602](http://www.jguru.com/faq/view.jsp?EID=114602)
         * 
         * @param img  the rendered image.
         * 
         * @return A buffered image.
         */
        private fun convertRenderedImage(img: RenderedImage): BufferedImage {
            if (img is BufferedImage) {
                return img
            }
            val width: Int = img.width
            val height: Int = img.height
            val cm: ColorModel = img.colorModel
            val isAlphaPremultiplied = cm.isAlphaPremultiplied()

            val raster: WritableRaster = cm.createCompatibleWritableRaster(width, height)

            val properties: Hashtable<String, Any?> = Hashtable<String, Any?>()
            val keys = img.propertyNames
            if (keys != null) {
                for (key in keys) {
                    properties[key] = img.getProperty(key)
                }
            }
            val result = BufferedImage(cm, raster, isAlphaPremultiplied, properties)
            img.copyData(raster)
            return result
        }

        private fun convertToSkijaImage(image: BufferedImage): org.jetbrains.skia.Image {
            // TODO: monitor performance:
            val w = image.width
            val h = image.height

            val db: DataBufferInt = image.getRaster().getDataBuffer() as DataBufferInt
            val pixels: IntArray = db.getData()

            val bytes = ByteArray(pixels.size * 4) // Big alloc!

            for (i in pixels.indices) {
                val p = pixels[i]
                bytes[i * 4 + 3] = ((p and -0x1000000) shr 24).toByte()
                bytes[i * 4 + 2] = ((p and 0xFF0000) shr 16).toByte()
                bytes[i * 4 + 1] = ((p and 0xFF00) shr 8).toByte()
                bytes[i * 4] = (p and 0xFF).toByte()
            }
            val imageInfo = ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
            return org.jetbrains.skia.Image.makeRaster(imageInfo, bytes, 4 * w)
        }
    }
}
