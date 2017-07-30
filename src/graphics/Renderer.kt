package graphics

import level.CameraObject
import main.Game
import java.awt.*
import java.awt.geom.AffineTransform

object Renderer {
    /* Default camera position */
    var camera: CameraObject = object : CameraObject {
        override var xPixel = 0
        override var yPixel = 0
    }

    var zoom = 1.0f

    var defaultClip = Rectangle(0, 0, Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE)

    lateinit var g2d: Graphics2D

    fun feed(graphics2D: Graphics2D) {
        g2d = graphics2D
    }

    fun setClip(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        g2d.clip = Rectangle(xPixel * Game.SCALE, yPixel * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE)
    }

    fun resetClip() {
        g2d.clip = defaultClip
    }

    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int) {
        g2d.drawImage(t.currentImage, xPixel * Game.SCALE, yPixel * Game.SCALE, t.widthPixels * Game.SCALE, t.heightPixels * Game.SCALE, null)
    }

    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, params: RenderParams) {
        var absoluteXPixel = xPixel * Game.SCALE + params.xPixelOffset
        var absoluteYPixel = yPixel * Game.SCALE + params.yPixelOffset
        val scaledScale = Game.SCALE * params.scale
        var absoluteWidthPixels = t.widthPixels * scaledScale * params.scaleWidth
        var absoluteHeightPixels = t.heightPixels * scaledScale * params.scaleHeight
        if(params.renderToLevel) {
            absoluteXPixel -= camera.xPixel
            absoluteYPixel -= camera.yPixel
            absoluteXPixel = (absoluteXPixel * zoom).toInt()
            absoluteYPixel = (absoluteYPixel * zoom).toInt()
            absoluteWidthPixels *= zoom
            absoluteHeightPixels *= zoom
        }
        var oldTransform: AffineTransform? = null
        var oldComposite: Composite? = null
        if(params.rotation != 0) {
            oldTransform = g2d.transform
            g2d.rotate(Math.toRadians(params.rotation * 90.0), (absoluteXPixel + absoluteWidthPixels / 2).toDouble(), (absoluteYPixel + absoluteHeightPixels / 2).toDouble())
        }
        if(params.alpha != 1.0f) {
            oldComposite = g2d.composite
            g2d.composite =  AlphaComposite.getInstance(AlphaComposite.SRC_OVER, params.alpha)
        }
        g2d.drawImage(t.currentImage, absoluteXPixel, absoluteYPixel, absoluteWidthPixels.toInt(), absoluteHeightPixels.toInt(), null)
        if(params.rotation != 0) {
            g2d.transform = oldTransform
        }
        if(params.alpha != 1.0f) {
            g2d.composite = oldComposite
        }
    }

    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        g2d.drawImage(t.currentImage, xPixel * Game.SCALE, yPixel * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE, null)
    }

    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, params: RenderParams) {
        var absoluteXPixel = xPixel * Game.SCALE + params.xPixelOffset
        var absoluteYPixel = yPixel * Game.SCALE + params.yPixelOffset
        val scaledScale = Game.SCALE * params.scale
        var absoluteWidthPixels = widthPixels * scaledScale * params.scaleWidth
        var absoluteHeightPixels = heightPixels * scaledScale * params.scaleHeight
        if(params.renderToLevel) {
            absoluteXPixel -= camera.xPixel
            absoluteYPixel -= camera.yPixel
            absoluteXPixel = (absoluteXPixel * zoom).toInt()
            absoluteYPixel = (absoluteYPixel * zoom).toInt()
            absoluteWidthPixels *= zoom
            absoluteHeightPixels *= zoom
        }
        var oldTransform: AffineTransform? = null
        var oldComposite: Composite? = null
        if(params.rotation != 0) {
            oldTransform = g2d.transform
            g2d.rotate(Math.toRadians(params.rotation * 90.0), (absoluteXPixel + absoluteWidthPixels / 2).toDouble(), (absoluteYPixel + absoluteHeightPixels / 2).toDouble())
        }
        if(params.alpha != 1.0f) {
            oldComposite = g2d.composite
            g2d.composite =  AlphaComposite.getInstance(AlphaComposite.SRC_OVER, params.alpha)
        }
        g2d.drawImage(t.currentImage, absoluteXPixel, absoluteYPixel, absoluteWidthPixels.toInt(), absoluteHeightPixels.toInt(), null)
        if(params.rotation != 0) {
            g2d.transform = oldTransform
        }
        if(params.alpha != 1.0f) {
            g2d.composite = oldComposite
        }
    }

    fun renderText(text: String, xPixel: Int, yPixel: Int, size: Int = 28, color: Int = 0xffffff) {
        g2d.font = Game.getFont(size)
        g2d.color = Color(color)
        g2d.drawString(text, xPixel * Game.SCALE, yPixel * Game.SCALE)
    }
}