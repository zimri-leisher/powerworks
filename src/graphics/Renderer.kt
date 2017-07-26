package graphics

import level.CameraObject
import main.Game
import java.awt.AlphaComposite
import java.awt.Composite
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.geom.AffineTransform

object Renderer {
    /* Default camera position */
    var camera: CameraObject = object : CameraObject {
        override var xPixel = 0;
        override var yPixel = 0
    }

    var zoom: Double = 1.0

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
        var scaledScale = Game.SCALE * params.scale
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
            g2d.rotate(Math.toRadians(params.rotation * 90.0), absoluteXPixel + absoluteWidthPixels / 2, absoluteYPixel + absoluteHeightPixels / 2)
        }
        if(params.alpha != 1.0) {
            oldComposite = g2d.composite
            g2d.composite =  AlphaComposite.getInstance(AlphaComposite.SRC_OVER, params.alpha.toFloat())
        }
        g2d.drawImage(t.currentImage, absoluteXPixel, absoluteYPixel, absoluteWidthPixels.toInt(), absoluteHeightPixels.toInt(), null)
        if(params.rotation != 0) {
            g2d.transform = oldTransform
        }
        if(params.alpha != 1.0) {
            g2d.composite = oldComposite
        }
    }

    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        g2d.drawImage(t.currentImage, xPixel * Game.SCALE, yPixel * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE, null)
    }

    fun renderTexture(t: Texture, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, params: RenderParams) {
        var absoluteXPixel = xPixel * Game.SCALE + params.xPixelOffset
        var absoluteYPixel = yPixel * Game.SCALE + params.yPixelOffset
        var scaledScale = Game.SCALE * params.scale
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
            g2d.rotate(Math.toRadians(params.rotation * 90.0), absoluteXPixel + absoluteWidthPixels / 2, absoluteYPixel + absoluteHeightPixels / 2)
        }
        if(params.alpha != 1.0) {
            oldComposite = g2d.composite
            g2d.composite =  AlphaComposite.getInstance(AlphaComposite.SRC_OVER, params.alpha.toFloat())
        }
        g2d.drawImage(t.currentImage, absoluteXPixel, absoluteYPixel, absoluteWidthPixels.toInt(), absoluteHeightPixels.toInt(), null)
        if(params.rotation != 0) {
            g2d.transform = oldTransform
        }
        if(params.alpha != 1.0) {
            g2d.composite = oldComposite
        }
    }
}