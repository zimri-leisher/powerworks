package graphics

import main.Game
import java.awt.*
import java.awt.geom.AffineTransform

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

    private val defaultParams = RenderParams()

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

    fun renderEmptyRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, color: Int = 0xFFFFFF, params: RenderParams = defaultParams, borderThickness: Int = 1) {
        val absoluteXPixel = (xPixel + params.xPixelOffset + xPixelOffset) * Game.SCALE
        val absoluteYPixel = (yPixel + params.yPixelOffset + yPixelOffset) * Game.SCALE
        val scaledScale = Game.SCALE * params.scale
        val absoluteWidthPixels = widthPixels * scaledScale * params.scaleWidth
        val absoluteHeightPixels = heightPixels * scaledScale * params.scaleHeight
        g2d.color = Color(color)
        var oldComposite: Composite? = null
        var oldTransform: AffineTransform? = null
        if (params.alpha != 1.0f) {
            oldComposite = g2d.composite
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, params.alpha)
        }
        if(params.rotation != 0f) {
            oldTransform = g2d.transform
            g2d.rotate(Math.toRadians(params.rotation.toDouble()), (absoluteXPixel + widthPixels / 2).toDouble(), (yPixel + heightPixels / 2).toDouble())
        }
        g2d.stroke = BasicStroke(Game.SCALE.toFloat() * borderThickness)
        g2d.drawRect(absoluteXPixel, absoluteYPixel, absoluteWidthPixels.toInt(), absoluteHeightPixels.toInt())
        if (params.alpha != 1.0f) {
            g2d.composite = oldComposite
        }
        if(params.rotation != 0f) {
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
    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, params: RenderParams) {
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
    fun renderTextureKeepAspect(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, params: RenderParams) {
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
    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, params: RenderParams) {
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
     * Renders the toString() of the given object at the x and y pixel with the given size and color, accounting for newlines
     */
    fun renderText(text: Any?, xPixel: Int, yPixel: Int, size: Int = Font.DEFAULT_SIZE, color: Int = 0xffffff) {
        val f = Font.getFont(size)
        g2d.font = f.font
        g2d.color = Color(color)
        val s = text.toString()
        if (s.contains("\n")) {
            s.split("\n").forEachIndexed { index, string ->
                g2d.drawString(string, (xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset + f.charHeight * index) * Game.SCALE)
            }
        } else
            g2d.drawString(s, (xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset + f.charHeight) * Game.SCALE)
    }
}