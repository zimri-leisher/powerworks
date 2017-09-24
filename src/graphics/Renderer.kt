package graphics

import main.Game
import java.awt.*
import java.awt.geom.AffineTransform

object Renderer {

    var defaultClip = Rectangle(0, 0, Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE)

    var xPixelOffset = 0
    var yPixelOffset = 0

    private val defaultParams = RenderParams()

    var params = defaultParams

    lateinit var g2d: Graphics2D

    fun resetParams() {
        params = defaultParams
    }

    fun feed(graphics2D: Graphics2D) {
        g2d = graphics2D
    }

    fun setClip(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        g2d.clip = Rectangle(xPixel * Game.SCALE, yPixel * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE)
    }

    fun resetClip() {
        g2d.clip = defaultClip
    }

    fun renderFilledRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, color: Int = 0xFFFFFF) {
        g2d.color = Color(color)
        g2d.fillRect((xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset) * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE)
    }

    fun renderEmptyRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, color: Int = 0xFFFFFF) {
        g2d.color = Color(color)
        g2d.stroke = BasicStroke(Game.SCALE.toFloat())
        g2d.drawRect((xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset) * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE)
    }

    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int) {
        g2d.drawImage(t.currentImage, (xPixel + xPixelOffset)* Game.SCALE, (yPixel + yPixelOffset) * Game.SCALE, t.widthPixels * Game.SCALE, t.heightPixels * Game.SCALE, null)
    }

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

    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        g2d.drawImage(t.currentImage, (xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset) * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE, null)
    }

    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, params: RenderParams) {
        val absoluteXPixel = (xPixel + params.xPixelOffset + xPixelOffset) * Game.SCALE
        val absoluteYPixel = (yPixel + params.yPixelOffset + yPixelOffset) * Game.SCALE
        val scaledScale = Game.SCALE * params.scale
        val absoluteWidthPixels = widthPixels * scaledScale * params.scaleWidth
        val absoluteHeightPixels = heightPixels * scaledScale * params.scaleHeight
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

    fun renderText(text: Any?, xPixel: Int, yPixel: Int, size: Int = 28, color: Int = 0xffffff) {
        g2d.font = Game.getFont(size)
        g2d.color = Color(color)
        g2d.drawString(text.toString(), (xPixel + xPixelOffset) * Game.SCALE, (yPixel + yPixelOffset) * Game.SCALE)
    }
}