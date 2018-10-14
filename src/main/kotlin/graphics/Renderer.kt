package graphics

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import graphics.text.TaggedText
import graphics.text.TextManager
import graphics.text.TextRenderContext
import graphics.text.TextRenderParams
import main.Game
import main.heightPixels
import main.widthPixels

/**
 * Custom SpriteBatch draw method that takes in a texture render parameter object and uses its values appropriately
 */
fun SpriteBatch.draw(texture: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, params: TextureRenderParams) {
    draw(texture, x, y, originX, originY, width, height, params.scaleX, params.scaleY, 360f - params.rotation)
}

/**
 * The main render helper.
 * This should be called directly in the render() methods of appropriate level objects/screen elements.
 * If the object is being rendered inside of a GUILevelView, the coordinates are automatically converted from level coordinates
 * to screen coordinates. If it is a separate gui element, it will remain unconverted.
 * This means there is no need to specify at any time whether you mean level or screen pixel coordinates, as long as you're keeping
 * render methods in their appropriate places
 */
object Renderer {

    /**
     * The x pixel that is added to render calls
     */
    var xPixelOffset = 0

    /**
     * The y pixel that is added to render calls
     */
    var yPixelOffset = 0

    // 6000 is a good number, a bit more than the requirements for 1 batch to hold max tiles at max zoom on a fairly large monitor
    val batch = SpriteBatch(6000)

    private val defaultParams = TextureRenderParams()

    /**
     * Will not render outside of this clip
     */
    fun setClip(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        batch.flush()
        ScissorStack.pushScissors(Rectangle((xPixel * Game.SCALE).toFloat(), (yPixel * Game.SCALE).toFloat(), (widthPixels * Game.SCALE).toFloat(), (heightPixels * Game.SCALE).toFloat()))
    }

    /**
     * Removes the clip
     */
    fun resetClip() {
        batch.flush()
        ScissorStack.popScissors()
    }

    /**
     * Draws a 'default' rectangle, i.e. one using the Image.GUI.DEFAULT_[...] components of a rectangle. This is useful for
     * making more visually interesting backgrounds of sizes determined at runtime
     */
    fun renderDefaultRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, params: TextureRenderParams) {
        val absoluteXPixel = ((xPixel + xPixelOffset) * Game.SCALE).toFloat()
        val absoluteYPixel = ((yPixel + yPixelOffset) * Game.SCALE).toFloat()
        val absoluteWidthPixels = (widthPixels * Game.SCALE).toFloat()
        val absoluteHeightPixels = (heightPixels * Game.SCALE).toFloat()
        val oldColor = batch.color
        batch.color = params.color
        val originX = absoluteWidthPixels / 2
        val originY = absoluteHeightPixels / 2
        batch.draw(Image.GUI.DEFAULT_BACKGROUND, absoluteXPixel, absoluteYPixel, originX, originY,
                absoluteWidthPixels, absoluteHeightPixels, params)
        with(Image.GUI.DEFAULT_EDGE_BOTTOM) {
            batch.draw(this, absoluteXPixel, absoluteYPixel, originX, originY,
                    absoluteWidthPixels, (this.heightPixels * Game.SCALE).toFloat(), params)
        }
        with(Image.GUI.DEFAULT_EDGE_LEFT) {
            batch.draw(this, absoluteXPixel, absoluteYPixel, originX, originY,
                    (this.widthPixels * Game.SCALE).toFloat(), absoluteHeightPixels, params)
        }
        // origins need adjusting to keep them relative to the center of the rectangle not the texture
        with(Image.GUI.DEFAULT_EDGE_RIGHT) {
            val imgWidth = (this.widthPixels * Game.SCALE).toFloat()
            batch.draw(this, absoluteXPixel + absoluteWidthPixels - imgWidth, absoluteYPixel, -(absoluteWidthPixels / 2) + imgWidth, originY,
                    imgWidth, absoluteHeightPixels, params)
        }
        with(Image.GUI.DEFAULT_EDGE_TOP) {
            val imgHeight = (this.heightPixels * Game.SCALE).toFloat()
            batch.draw(this, absoluteXPixel, absoluteYPixel + absoluteHeightPixels - imgHeight, originX, -(absoluteHeightPixels / 2) + imgHeight,
                    absoluteWidthPixels, imgHeight, params)
        }
        batch.color = oldColor
        if (params.brightness > 1f) {
            brightPatch(absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absoluteHeightPixels, params)
        } else if (params.brightness < 1f) {
            darkPatch(absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absoluteHeightPixels, params)
        }
    }

    /**
     * Draws a 'default' rectangle, i.e. one using the Image.GUI.DEFAULT_[...] components of a rectangle. This is useful for
     * making more visually interesting backgrounds of sizes determined at runtime
     */
    fun renderDefaultRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        val absoluteXPixel = ((xPixel + xPixelOffset) * Game.SCALE).toFloat()
        val absoluteYPixel = ((yPixel + yPixelOffset) * Game.SCALE).toFloat()
        val absoluteWidthPixels = (widthPixels * Game.SCALE).toFloat()
        val absoluteHeightPixels = (heightPixels * Game.SCALE).toFloat()
        // background
        batch.draw(Image.GUI.DEFAULT_BACKGROUND, absoluteXPixel, absoluteYPixel, absoluteWidthPixels, absoluteHeightPixels)
        // edges
        with(Image.GUI.DEFAULT_EDGE_RIGHT) {
            batch.draw(this, absoluteXPixel + absoluteWidthPixels - (this.widthPixels * Game.SCALE), absoluteYPixel,
                    (this.widthPixels * Game.SCALE).toFloat(), absoluteHeightPixels)
        }
        with(Image.GUI.DEFAULT_EDGE_BOTTOM) {
            batch.draw(this, absoluteXPixel, absoluteYPixel,
                    absoluteWidthPixels, (this.heightPixels * Game.SCALE).toFloat())
        }
        with(Image.GUI.DEFAULT_EDGE_LEFT) {
            batch.draw(this, absoluteXPixel, absoluteYPixel,
                    (this.widthPixels * Game.SCALE).toFloat(), absoluteHeightPixels)
        }
        with(Image.GUI.DEFAULT_EDGE_TOP) {
            batch.draw(this, absoluteXPixel, absoluteYPixel + absoluteHeightPixels - (this.heightPixels * Game.SCALE),
                    absoluteWidthPixels, (this.heightPixels * Game.SCALE).toFloat())
        }
    }

    fun renderFilledRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, params: TextureRenderParams) {
        val absoluteXPixel = ((xPixel + xPixelOffset) * Game.SCALE).toFloat()
        val absoluteYPixel = ((yPixel + yPixelOffset) * Game.SCALE).toFloat()
        val absoluteWidthPixels = (widthPixels * Game.SCALE).toFloat()
        val absoluteHeightPixels = (heightPixels * Game.SCALE).toFloat()
        val originX = absoluteWidthPixels / 2
        val originY = absoluteHeightPixels / 2
        val oldColor = batch.color
        batch.color = params.color
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absoluteHeightPixels, params)
        batch.color = oldColor
    }

    fun renderFilledRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        val absoluteXPixel = ((xPixel + xPixelOffset) * Game.SCALE).toFloat()
        val absoluteYPixel = ((yPixel + yPixelOffset) * Game.SCALE).toFloat()
        val absoluteWidthPixels = (widthPixels * Game.SCALE).toFloat()
        val absoluteHeightPixels = (heightPixels * Game.SCALE).toFloat()
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, absoluteWidthPixels, absoluteHeightPixels)
    }

    private fun brightPatch(absoluteXPixel: Float, absoluteYPixel: Float, originX: Float, originY: Float, absoluteWidthPixels: Float, absoluteHeightPixels: Float, params: TextureRenderParams) {
        val oldColor = batch.color
        val oldSrcFunc = batch.blendSrcFunc
        val oldDestFunc = batch.blendDstFunc
        batch.setColor(1f, 1f, 1f, params.brightness - 1f)
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absoluteHeightPixels, params)
        batch.color = oldColor
    }

    private fun darkPatch(absoluteXPixel: Float, absoluteYPixel: Float, originX: Float, originY: Float, absoluteWidthPixels: Float, absoluteHeightPixels: Float, params: TextureRenderParams) {
        val oldColor = batch.color
        val oldSrcFunc = batch.blendSrcFunc
        val oldDestFunc = batch.blendDstFunc
        batch.setColor(1f, 1f, 1f, 1f - params.brightness)
        batch.draw(Image.GUI.BLACK_FILLER, absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absoluteHeightPixels, params)
        batch.color = oldColor
    }

    /**
     * @param borderThickness how thicc to make the lines
     */
    fun renderEmptyRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, borderThickness: Float = 1f, params: TextureRenderParams) {
        val absoluteXPixel = ((xPixel + xPixelOffset) * Game.SCALE).toFloat()
        val absoluteYPixel = ((yPixel + yPixelOffset) * Game.SCALE).toFloat()
        val absoluteWidthPixels = (widthPixels * Game.SCALE).toFloat()
        val absoluteHeightPixels = (heightPixels * Game.SCALE).toFloat()
        val absBorderThickness = borderThickness * Game.SCALE
        val oldColor = batch.color
        batch.color = params.color
        val originX = absoluteWidthPixels / 2
        val originY = absoluteHeightPixels / 2
        // bottom
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absBorderThickness, params)
        // left
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, originX, originY, absBorderThickness, absoluteHeightPixels, params)
        // right
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel + absoluteWidthPixels - absBorderThickness, absoluteYPixel, originX, originY, absBorderThickness, absoluteHeightPixels, params)
        // top
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel + absoluteHeightPixels - absBorderThickness, originX, originY, absoluteWidthPixels, absBorderThickness, params)
        batch.color = oldColor
    }

    /**
     * @param borderThickness how thicc to make the lines
     */
    fun renderEmptyRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, borderThickness: Float = 1f) {
        val absoluteXPixel = ((xPixel + xPixelOffset) * Game.SCALE + if (widthPixels < 0) (widthPixels * Game.SCALE) else 0).toFloat()
        val absoluteYPixel = ((yPixel + yPixelOffset) * Game.SCALE + if (heightPixels < 0) (heightPixels * Game.SCALE) else 0).toFloat()
        val absoluteWidthPixels = (Math.abs(widthPixels) * Game.SCALE).toFloat()
        val absoluteHeightPixels = (Math.abs(heightPixels) * Game.SCALE).toFloat()
        val absBorderThickness = borderThickness * Game.SCALE
        // bottom
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, absoluteWidthPixels, absBorderThickness)
        // left
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, absBorderThickness, absoluteHeightPixels)
        // right
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel + absoluteWidthPixels - absBorderThickness, absoluteYPixel, absBorderThickness, absoluteHeightPixels)
        // top
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel + absoluteHeightPixels - absBorderThickness, absoluteWidthPixels, absBorderThickness)
    }

    /**
     * Quicker method for rendering a texture that skips all parameter calculations. Renders a texture at the x and
     * y pixel with the given parameters
     */
    fun renderTexture(t: TextureRegion, xPixel: Int, yPixel: Int) {
        batch.draw(t, ((xPixel + xPixelOffset) * Game.SCALE).toFloat(), ((yPixel + yPixelOffset) * Game.SCALE).toFloat(), (t.widthPixels * Game.SCALE).toFloat(), (t.heightPixels * Game.SCALE).toFloat())
    }

    /**
     * Renders a texture at the x and y pixel with the given parameters
     */
    fun renderTexture(t: TextureRegion, xPixel: Int, yPixel: Int, params: TextureRenderParams) {
        val absoluteXPixel = ((xPixel + xPixelOffset) * Game.SCALE).toFloat()
        val absoluteYPixel = ((yPixel + yPixelOffset) * Game.SCALE).toFloat()
        val absoluteWidthPixels = (t.widthPixels * Game.SCALE).toFloat()
        val absoluteHeightPixels = (t.heightPixels * Game.SCALE).toFloat()
        val originX = absoluteWidthPixels / 2
        val originY = absoluteHeightPixels / 2
        val oldColor = batch.color
        batch.color = params.color
        batch.draw(t, absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absoluteHeightPixels, params)
        batch.color = oldColor
    }

    /**
     * Renders a texture at the x and y pixel, keeping the texture at its original aspect ratio but also fitting it inside
     * of the width and height pixels
     */
    fun renderTextureKeepAspect(t: TextureRegion, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
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
    fun renderTextureKeepAspect(t: TextureRegion, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, params: TextureRenderParams) {
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
    fun renderTexture(t: TextureRegion, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        batch.draw(t, ((xPixel + xPixelOffset) * Game.SCALE).toFloat(), ((yPixel + yPixelOffset) * Game.SCALE).toFloat(), (widthPixels * Game.SCALE).toFloat(), (heightPixels * Game.SCALE).toFloat())
    }

    /**
     * Renders a texture at the x and y pixel with the given params, stretching it to fit the width and height pixels
     */
    fun renderTexture(t: TextureRegion, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, params: TextureRenderParams) {
        val absoluteXPixel = ((xPixel + xPixelOffset) * Game.SCALE).toFloat()
        val absoluteYPixel = ((yPixel + yPixelOffset) * Game.SCALE).toFloat()
        val absoluteWidthPixels = (widthPixels * Game.SCALE).toFloat()
        val absoluteHeightPixels = (heightPixels * Game.SCALE).toFloat()
        val originX = absoluteWidthPixels / 2
        val originY = absoluteHeightPixels / 2
        val oldColor = batch.color
        batch.color = params.color
        batch.draw(t, absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absoluteHeightPixels, params)
        batch.color = oldColor
    }

    /**
     * Renders the toString() of the given object at the x and y pixel
     * @param ignoreLines whether to ignore the '\n' character or not
     */
    fun renderText(text: Any?, xPixel: Int, yPixel: Int, ignoreLines: Boolean = false) {
        val f = TextManager.getFont()
        val font = f.font
        val s = text.toString()
        if (!ignoreLines && s.contains("\n")) {
            val lines = s.split("\n")
            lines.forEachIndexed { index, string ->
                font.draw(batch, string, ((xPixel + xPixelOffset) * Game.SCALE).toFloat(), ((yPixel + f.charHeight + yPixelOffset + ((f.charHeight + 1) * (lines.lastIndex - index))) * Game.SCALE))
            }
        } else {
            font.draw(batch, s, ((xPixel + xPixelOffset) * Game.SCALE).toFloat(), ((yPixel + f.charHeight + yPixelOffset) * Game.SCALE))
        }
    }

    /**
     * Renders the toString() of the given object at the x and y pixel, accounting for newlines
     * @param params the rendering parameters to use for the text. Intended to be used by text tags
     * @param ignoreLines whether to ignore the '\n' character or not
     */
    fun renderText(text: Any?, xPixel: Int, yPixel: Int, params: TextRenderParams, ignoreLines: Boolean = false) {
        val f = TextManager.getFont(params.size, params.style)
        val font = f.font
        val oldColor = font.color
        font.color = params.color
        val s = text.toString()
        if (!ignoreLines && s.contains("\n")) {
            val lines = s.split("\n")
            lines.forEachIndexed { index, string ->
                font.draw(batch, string, ((xPixel + xPixelOffset) * Game.SCALE).toFloat(), ((yPixel + f.charHeight + yPixelOffset + ((f.charHeight + 1) * (lines.lastIndex - index))) * Game.SCALE))
            }
        } else {
            font.draw(batch, s, ((xPixel + xPixelOffset) * Game.SCALE).toFloat(), ((yPixel + f.charHeight + yPixelOffset) * Game.SCALE))
        }
        font.color = oldColor
    }

    /**
     * Renders a tagged text object
     * @params the parameters to use initially, may be changed by tags later
     */
    fun renderTaggedText(taggedText: TaggedText, xPixel: Int, yPixel: Int, params: TextRenderParams = TextRenderParams()) {
        val original = params.copy()
        val context = TextRenderContext(java.awt.Rectangle(xPixel, yPixel, 0, 0), params)
        var lastTagIndex = 0
        for ((thisTagIndex, tag) in taggedText.tags) {
            val substring = taggedText.text.substring(lastTagIndex, thisTagIndex)
            renderText(substring, context.currentBounds.width + context.currentBounds.x, context.currentBounds.y, context.currentRenderParams)
            val bounds = TextManager.getStringBounds(substring, context.currentRenderParams.size, context.currentRenderParams.style)
            context.currentBounds.width += bounds.width
            context.currentBounds.height = Math.max(context.currentBounds.height, bounds.height)
            tag.forEach { it.type.execute(context, it.argument, false) }
            lastTagIndex = thisTagIndex
        }
        renderText(taggedText.text.substring(lastTagIndex), context.currentBounds.width + context.currentBounds.x, context.currentBounds.y, context.currentRenderParams)
        params.color = original.color
        params.size = original.size
        params.style = original.style
    }
}