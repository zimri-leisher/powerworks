package graphics

import graphics.text.TaggedText
import graphics.text.TextManager
import graphics.text.TextRenderContext
import graphics.text.TextRenderParams
import main.Game
import java.awt.*
import java.awt.geom.AffineTransform

/**
 * The main render helper.
 * This should be called directly in the render() methods of appropriate level objects/screen elements.
 * If the object is being rendered inside of a GUILevelView, the coordinates are automatically converted from level coordinates
 * to screen coordinates. If it is a separate gui element, it will remain unconverted.
 * This means there is no need to specify at any time whether you mean level or screen pixel coordinates, as long as you're keeping
 * render methods in their appropriate places
 */
object Renderer {

    var defaultClip = Rectangle(Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE)

    /**
     * The x pixel that is added to render calls
     */
    var xPixelOffset = 0
    /**
     * The y pixel that is added to render calls
     */
    var yPixelOffset = 0

    private val defaultParams = TextureRenderParams()

    /**
     * Overarching render parameters
     */
    var params = defaultParams

    lateinit var g2d: Graphics2D

    fun resetParams() {
        params = defaultParams
    }

    /**
     * Will not render outside of this clip
     */
    fun setClip(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        g2d.clip = Rectangle(xPixel * Game.SCALE, yPixel * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE)
    }

    /**
     * Removes the clip
     */
    fun resetClip() {
        g2d.clip = defaultClip
    }

    fun renderFilledRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, color: Int = 0xFFFFFF, alpha: Float = 1.0f) {
        var oldComposite: Composite? = null
        if (alpha != 1.0f) {
            oldComposite = g2d.composite
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
        }
        g2d.color = Color(color)
        g2d.fillRect((xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset) * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE)
        if (oldComposite != null) {
            g2d.composite = oldComposite
        }
    }

    fun renderEmptyRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, color: Int = 0xFFFFFF, params: TextureRenderParams = defaultParams, borderThickness: Int = 1) {
        val scaledScale = Game.SCALE * params.scale
        val absoluteXPixel = (xPixel + params.xPixelOffset + xPixelOffset) * Game.SCALE + if(widthPixels < 0) (widthPixels * scaledScale).toInt() else 0
        val absoluteYPixel = (yPixel + params.yPixelOffset + yPixelOffset) * Game.SCALE + if(heightPixels < 0) (heightPixels * scaledScale).toInt() else 0
        val absoluteWidthPixels = Math.abs(widthPixels) * scaledScale * params.scaleWidth
        val absoluteHeightPixels = Math.abs(heightPixels) * scaledScale * params.scaleHeight
        g2d.color = Color(color)
        var oldComposite: Composite? = null
        var oldTransform: AffineTransform? = null
        if (params.alpha != 1.0f) {
            oldComposite = g2d.composite
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, params.alpha)
        }
        if (params.rotation != 0f) {
            oldTransform = g2d.transform
            g2d.rotate(Math.toRadians(params.rotation.toDouble()), (absoluteXPixel + absoluteWidthPixels / 2).toDouble(), (absoluteYPixel + absoluteHeightPixels/ 2).toDouble())
        }
        g2d.stroke = BasicStroke(Game.SCALE.toFloat() * borderThickness)
        g2d.drawRect(absoluteXPixel, absoluteYPixel, absoluteWidthPixels.toInt(), absoluteHeightPixels.toInt())
        if (params.alpha != 1.0f) {
            g2d.composite = oldComposite
        }
        if (params.rotation != 0f) {
            g2d.transform = oldTransform
        }
    }

    /**
     * Quicker method for rendering a texture that skips all parameter calculations
     */
    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int) {
        g2d.drawImage(t.currentImage, (xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset) * Game.SCALE, t.widthPixels * Game.SCALE, t.heightPixels * Game.SCALE, null)
    }

    /**
     * Renders a texture at the x and y pixel with the given parameters
     */
    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, params: TextureRenderParams) {
        val absoluteXPixel = (xPixel + params.xPixelOffset + xPixelOffset) * Game.SCALE
        val absoluteYPixel = (yPixel + params.yPixelOffset + yPixelOffset) * Game.SCALE
        val scaledScale = Game.SCALE * params.scale
        val absoluteWidthPixels = t.widthPixels * scaledScale * params.scaleWidth
        val absoluteHeightPixels = t.heightPixels * scaledScale * params.scaleHeight
        var oldTransform: AffineTransform? = null
        var oldComposite: Composite? = null
        if (params.rotation != 0f) {
            oldTransform = g2d.transform
            g2d.rotate(Math.toRadians(params.rotation.toDouble()), (absoluteXPixel + absoluteWidthPixels / 2).toDouble(), (absoluteYPixel + absoluteHeightPixels / 2).toDouble())
        }
        if (params.alpha != 1.0f) {
            oldComposite = g2d.composite
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, params.alpha)
        }
        g2d.drawImage(t.currentImage, absoluteXPixel, absoluteYPixel, absoluteWidthPixels.toInt(), absoluteHeightPixels.toInt(), null)
        if (params.rotation != 0f) {
            g2d.transform = oldTransform
        }
        if (params.alpha != 1.0f) {
            g2d.composite = oldComposite
        }
    }

    /**
     * Renders a texture at the x and y pixel, keeping the texture at its original aspect ratio but also fitting it inside
     * of the width and height pixels
     */
    fun renderTextureKeepAspect(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        var w = widthPixels
        var h = heightPixels
        if (t.widthPixels > t.heightPixels) {
            if (t.widthPixels > widthPixels) {
                w = widthPixels
                val ratio = widthPixels.toFloat() / t.widthPixels
                h = (t.heightPixels * ratio).toInt()
            }
        }
        if (t.heightPixels > t.widthPixels) {
            if (t.heightPixels > heightPixels) {
                h = heightPixels
                val ratio = heightPixels.toFloat() / t.heightPixels
                w = (t.widthPixels * ratio).toInt()
            }
        }
        Renderer.renderTexture(t, xPixel + (widthPixels - w) / 2, yPixel + (heightPixels - h) / 2, w, h)
    }

    /**
     * Renders a texture at the x and y pixel, keeping the texture at its original aspect ratio but also fitting it inside
     * of the width and height pixels. The result of that has the render params applied to it
     */
    fun renderTextureKeepAspect(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, params: TextureRenderParams) {
        var w = widthPixels
        var h = heightPixels
        if (t.widthPixels > t.heightPixels) {
            if (t.widthPixels > widthPixels) {
                w = widthPixels
                val ratio = widthPixels.toFloat() / t.widthPixels
                h = (t.heightPixels * ratio).toInt()
            }
        }
        if (t.heightPixels > t.widthPixels) {
            if (t.heightPixels > heightPixels) {
                h = heightPixels
                val ratio = heightPixels.toFloat() / t.heightPixels
                w = (t.widthPixels * ratio).toInt()
            }
        }
        Renderer.renderTexture(t, xPixel + (widthPixels - w) / 2, yPixel + (heightPixels - h) / 2, w, h, params)
    }

    /**
     * Renders a texture at the x and y pixel, stretching it to fit the width and height pixels
     */
    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        g2d.drawImage(t.currentImage, (xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset) * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE, null)
    }

    /**
     * Renders a texture at the x and y pixel with the given params, stretching it to fit the width and height pixels
     */
    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, params: TextureRenderParams) {
        val absoluteXPixel = (xPixel + params.xPixelOffset + xPixelOffset) * Game.SCALE
        val absoluteYPixel = (yPixel + params.yPixelOffset + yPixelOffset) * Game.SCALE
        val scaledScale = Game.SCALE * params.scale
        val absoluteWidthPixels: Float
        val absoluteHeightPixels: Float
        var w = widthPixels
        var h = heightPixels
        if (t.widthPixels > t.heightPixels) {
            if (t.widthPixels > widthPixels) {
                w = widthPixels
                val ratio = widthPixels.toFloat() / t.widthPixels
                h = (t.heightPixels * ratio).toInt()
            }
        }
        if (t.heightPixels > t.widthPixels) {
            if (t.heightPixels > heightPixels) {
                h = heightPixels
                val ratio = heightPixels.toFloat() / t.heightPixels
                w = (t.widthPixels * ratio).toInt()
            }
        }
        absoluteWidthPixels = w * scaledScale * params.scaleWidth
        absoluteHeightPixels = h * scaledScale * params.scaleHeight
        var oldTransform: AffineTransform? = null
        var oldComposite: Composite? = null
        if (params.rotation != 0f) {
            oldTransform = g2d.transform
            g2d.rotate(Math.toRadians(params.rotation.toDouble()), (absoluteXPixel + absoluteWidthPixels / 2).toDouble(), (absoluteYPixel + absoluteHeightPixels / 2).toDouble())
        }
        if (params.alpha != 1.0f) {
            oldComposite = g2d.composite
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, params.alpha)
        }
        g2d.drawImage(t.currentImage, absoluteXPixel, absoluteYPixel, absoluteWidthPixels.toInt(), absoluteHeightPixels.toInt(), null)
        if (params.rotation != 0f) {
            g2d.transform = oldTransform
        }
        if (params.alpha != 1.0f) {
            g2d.composite = oldComposite
        }
    }

    /**
     * Renders the toString() of the given object at the x and y pixel, accounting for newlines
     */
    fun renderText(text: Any?, xPixel: Int, yPixel: Int, ignoreLines: Boolean = false) {
        val f = TextManager.getFont()
        g2d.font = f.font
        g2d.color = Color(0xFFFFFF)
        val s = text.toString()
        if (!ignoreLines && s.contains("\n")) {
            s.split("\n").forEachIndexed { index, string ->
                g2d.drawString(string, (xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset + f.charHeight * index) * Game.SCALE)
            }
        } else
            g2d.drawString(s, (xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset + f.charHeight) * Game.SCALE)
    }

    /**
     * Renders the toString() of the given object at the x and y pixel, accounting for newlines
     * @param params the rendering parameters to use for the text. Intended to be used by text tags
     */
    fun renderText(text: Any?, xPixel: Int, yPixel: Int, params: TextRenderParams, ignoreLines: Boolean = false) {
        val f = TextManager.getFont(params.size, params.style)
        g2d.font = f.font
        g2d.color = Color(params.color)
        val s = text.toString()
        if (!ignoreLines && s.contains("\n")) {
            s.split("\n").forEachIndexed { index, string ->
                g2d.drawString(string, (xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset + f.charHeight * index) * Game.SCALE)
            }
        } else
            g2d.drawString(s, (xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset + f.charHeight) * Game.SCALE)
    }

    /**
     * Renders a tagged text object.
     * @params the parameters to use initiallly, may be changed by tags later
     */
    fun renderTaggedText(taggedText: TaggedText, xPixel: Int, yPixel: Int, params: TextRenderParams = TextRenderParams()) {
        val original = params.copy()
        val context = TextRenderContext(Rectangle(xPixel, yPixel, 0, 0), params)
        var lastTagIndex = 0
        for ((thisTagIndex, tag) in taggedText.tags) {
            val substring = taggedText.text.substring(lastTagIndex, thisTagIndex)
            renderText(substring, context.currentBounds.width + context.currentBounds.x, context.currentBounds.y, context.currentRenderParams)
            val bounds = TextManager.getStringBounds(substring, context.currentRenderParams.size, context.currentRenderParams.style)
            context.currentBounds.width += bounds.width
            context.currentBounds.height = Math.max(context.currentBounds.height, bounds.height)
            tag.forEach { it.type.execute(context, it.argument) }
            lastTagIndex = thisTagIndex
        }
        renderText(taggedText.text.substring(lastTagIndex), context.currentBounds.width + context.currentBounds.x, context.currentBounds.y, context.currentRenderParams)
        params.color = original.color
        params.size = original.size
        params.style = original.style
    }
}