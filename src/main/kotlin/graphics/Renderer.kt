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
 * The main render helper.
 * This should be called directly in the render() methods of appropriate level objects/screen elements.
 * If the object is being rendered inside of a GUILevelView, the coordinates are automatically converted from level coordinates
 * to screen coordinates. If it is a separate gui element, it will remain unconverted.
 * This means there is no need to specify at any time whether you mean level or screen pixel coordinates, as long as you're keeping
 * render methods in their appropriate places. It's not magical, of course, the reason it works is that just before
 * the Level.render method is called from the GUILevelView's render method, the xPixelOffset and yPixelOffset are set to the
 * camera position. These are then subtracted from the coordinates being passed in, thus adequately converting from the level
 * to the screen
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

    val batch = SpriteBatch()

    private val defaultParams = TextureRenderParams()

    /**
     * Will not render outside of this clip
     */
    fun setClip(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        ScissorStack.pushScissors(Rectangle((xPixel * Game.SCALE).toFloat(), (yPixel * Game.SCALE).toFloat(), (widthPixels * Game.SCALE).toFloat(), (heightPixels * Game.SCALE).toFloat()))
    }

    /**
     * Removes the clip
     */
    fun resetClip() {
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
        val color = batch.color
        batch.color = params.color
        val originX = Math.ceil(absoluteWidthPixels / 2.0).toFloat()
        val originY = Math.ceil(absoluteHeightPixels / 2.0).toFloat()
        batch.draw(Image.GUI.DEFAULT_BACKGROUND, absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absoluteHeightPixels, params.scaleX, params.scaleY, params.rotation)
        with(Image.GUI.DEFAULT_EDGE_RIGHT) {
            batch.draw(this, absoluteXPixel + absoluteWidthPixels - (this.widthPixels * Game.SCALE), absoluteYPixel, originX, originY,
                    (this.widthPixels * Game.SCALE).toFloat(), absoluteHeightPixels, params.scaleX, params.scaleY, params.rotation)
        }
        with(Image.GUI.DEFAULT_EDGE_BOTTOM) {
            batch.draw(this, absoluteXPixel, absoluteYPixel, originX, originY,
                    absoluteWidthPixels, (this.heightPixels * Game.SCALE).toFloat(), params.scaleX, params.scaleY, params.rotation)
        }
        with(Image.GUI.DEFAULT_EDGE_LEFT) {
            batch.draw(this, absoluteXPixel, absoluteYPixel, originX, originY,
                    (this.widthPixels * Game.SCALE).toFloat(), absoluteHeightPixels, params.scaleX, params.scaleY, params.rotation)
        }
        with(Image.GUI.DEFAULT_EDGE_TOP) {
            batch.draw(this, absoluteXPixel, absoluteYPixel + absoluteHeightPixels - (this.heightPixels * Game.SCALE), originX, originY,
                    absoluteWidthPixels, (this.heightPixels * Game.SCALE).toFloat(), params.scaleX, params.scaleY, params.rotation)
        }
        batch.color = color
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
        val colorBatch = batch.color
        val originX = Math.ceil(absoluteWidthPixels / 2.0).toFloat()
        val originY = Math.ceil(absoluteHeightPixels / 2.0).toFloat()
        batch.color = params.color
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absoluteHeightPixels, params.scaleX, params.scaleY, params.rotation)
        batch.color = colorBatch
    }

    fun renderFilledRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) {
        val absoluteXPixel = ((xPixel + xPixelOffset) * Game.SCALE).toFloat()
        val absoluteYPixel = ((yPixel + yPixelOffset) * Game.SCALE).toFloat()
        val absoluteWidthPixels = (widthPixels * Game.SCALE).toFloat()
        val absoluteHeightPixels = (heightPixels * Game.SCALE).toFloat()
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, absoluteWidthPixels, absoluteHeightPixels)
    }

    /**
     * @param borderThickness how thicc to make the lines
     */
    fun renderEmptyRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, borderThickness: Float = 1f, params: TextureRenderParams) {
        val absoluteXPixel = ((xPixel + xPixelOffset) * Game.SCALE + if (widthPixels < 0) (widthPixels * Game.SCALE) else 0).toFloat()
        val absoluteYPixel = ((yPixel + yPixelOffset) * Game.SCALE + if (heightPixels < 0) (heightPixels * Game.SCALE) else 0).toFloat()
        val absoluteWidthPixels = (Math.abs(widthPixels) * Game.SCALE).toFloat()
        val absoluteHeightPixels = (Math.abs(heightPixels) * Game.SCALE).toFloat()
        val absBorderThickness = borderThickness * Game.SCALE
        val colorBatch = batch.color
        batch.color = params.color
        val originX = Math.ceil(absoluteWidthPixels / 2.0).toFloat()
        val originY = Math.ceil(absoluteHeightPixels / 2.0).toFloat()
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absBorderThickness, params.scaleX, params.scaleY, params.rotation)
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel + absoluteWidthPixels, absoluteYPixel, originX, originY, absBorderThickness, absoluteHeightPixels, params.scaleX, params.scaleY, params.rotation)
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, originX, originY, absBorderThickness, absoluteHeightPixels, params.scaleX, params.scaleY, params.rotation)
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel + absoluteHeightPixels, originX, originY, absoluteWidthPixels + 4, absBorderThickness, params.scaleX, params.scaleY, params.rotation)
        batch.color = colorBatch
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
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, absoluteWidthPixels, absBorderThickness)
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel + absoluteWidthPixels, absoluteYPixel, absBorderThickness, absoluteHeightPixels)
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel, absBorderThickness, absoluteHeightPixels)
        batch.draw(Image.GUI.WHITE_FILLER, absoluteXPixel, absoluteYPixel + absoluteHeightPixels, absoluteWidthPixels + 4, absBorderThickness)
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
        val originX = Math.ceil(absoluteWidthPixels / 2.0).toFloat()
        val originY = Math.ceil(absoluteHeightPixels / 2.0).toFloat()
        val color = batch.color
        batch.color = params.color
        batch.draw(t, absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absoluteHeightPixels, params.scaleX, params.scaleY, params.rotation)
        batch.color = color
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
        val originX = Math.ceil(absoluteWidthPixels / 2.0).toFloat()
        val originY = Math.ceil(absoluteHeightPixels / 2.0).toFloat()
        val color = batch.color
        batch.color = params.color
        batch.draw(t, absoluteXPixel, absoluteYPixel, originX, originY, absoluteWidthPixels, absoluteHeightPixels, params.scaleX, params.scaleY, params.rotation)
        batch.color = color
    }

    /**
     * Renders the toString() of the given object at the x and y pixel
     * @param ignoreLines whether to ignore the '\n' character or not
     */
    fun renderText(text: Any?, xPixel: Int, yPixel: Int, ignoreLines: Boolean = false) {
        val f = TextManager.getFont()
        val font = f.font
        font.setColor(0f, 0f, 0f, 1f)
        val s = text.toString()
        if (!ignoreLines && s.contains("\n")) {
            s.split("\n").forEachIndexed { index, string ->
                font.draw(batch, string, ((xPixel + xPixelOffset) * Game.SCALE).toFloat(), ((yPixel + yPixelOffset + f.charHeight * index) * Game.SCALE).toFloat())
            }
        } else {
            font.draw(batch, s, ((xPixel + xPixelOffset) * Game.SCALE).toFloat(), ((yPixel + yPixelOffset + f.charHeight) * Game.SCALE).toFloat())
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
        val color = font.color
        font.color = params.color
        val s = text.toString()
        if (!ignoreLines && s.contains("\n")) {
            val lines = s.split("\n")
            lines.forEachIndexed { index, string ->
                font.draw(batch, string, ((xPixel + xPixelOffset) * Game.SCALE).toFloat(), ((yPixel + f.charHeight + yPixelOffset + (f.charHeight * (lines.lastIndex - index))) * Game.SCALE).toFloat())
            }
        } else {
            font.draw(batch, s, ((xPixel + xPixelOffset) * Game.SCALE).toFloat(), ((yPixel + f.charHeight + yPixelOffset) * Game.SCALE).toFloat())
        }
        font.color = color
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
            tag.forEach { it.type.execute(context, it.argument) }
            lastTagIndex = thisTagIndex
        }
        renderText(taggedText.text.substring(lastTagIndex), context.currentBounds.width + context.currentBounds.x, context.currentBounds.y, context.currentRenderParams)
        params.color = original.color
        params.size = original.size
        params.style = original.style
    }
}