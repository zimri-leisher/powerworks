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
import main.height
import main.width

/**
 * Custom SpriteBatch draw method that takes in a texture render parameter object and uses its values appropriately
 */
fun SpriteBatch.draw(texture: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, params: TextureRenderParams) {
    draw(texture, x, y, originX, originY, width, height, params.scaleX, params.scaleY, params.rotation)
}

/**
 * The main render helper.
 * This should be called directly in the render() methods of appropriate level objects/screen elements.
 * If the object is being rendered inside of a GUILevelView, the coordinates are automatically converted from level coordinates
 * to screen coordinates. If it is a separate gui element, it will remain unconverted.
 * This means there is no need to specify at any time whether you mean level or screen coordinates, as long as you're keeping
 * render methods in their appropriate places
 */
object Renderer {

    /**
     * The x that is added to render calls
     */
    var xOffset = 0

    /**
     * The y that is added to render calls
     */
    var yOffset = 0

    // 6000 is a good number, a bit more than the requirements for 1 batch to hold max tiles at max zoom on a fairly large monitor
    val batch = SpriteBatch(6000)

    /**
     * Will not render outside of this clip
     */
    fun pushClip(x: Int, y: Int, width: Int, height: Int) {
        batch.flush()
        ScissorStack.pushScissors(Rectangle((x * Game.SCALE).toFloat(), (y * Game.SCALE).toFloat(), (width * Game.SCALE).toFloat(), (height * Game.SCALE).toFloat()))
    }

    /**
     * Removes the clip
     */
    fun popClip() {
        try {
            batch.flush()
            ScissorStack.popScissors()
        } catch (e: IllegalStateException) {
            // there is apparently no way to check if there are no scissors on the stack. feelsbad
            if (e.message != "Array is empty.") {
                throw e
            }
        }
    }

    /**
     * Draws a 'default' rectangle, i.e. one using the Image.GUI.DEFAULT_[...] components of a rectangle. This is useful for
     * making more visually interesting backgrounds of sizes determined at runtime
     */
    fun renderDefaultRectangle(x: Int, y: Int, width: Int, height: Int, params: TextureRenderParams) {
        val absoluteX = ((x + xOffset) * Game.SCALE).toFloat()
        val absoluteY = ((y + yOffset) * Game.SCALE).toFloat()
        val absoluteWidth = (width * Game.SCALE).toFloat()
        val absoluteHeight = (height * Game.SCALE).toFloat()
        val oldColor = batch.color
        batch.color = params.color
        val originX = absoluteWidth / 2
        val originY = absoluteHeight / 2
        batch.draw(Image.Gui.DEFAULT_BACKGROUND, absoluteX, absoluteY, originX, originY,
                absoluteWidth, absoluteHeight, params)
        with(Image.Gui.DEFAULT_EDGE_BOTTOM) {
            batch.draw(this, absoluteX, absoluteY, originX, originY,
                    absoluteWidth, (this.height * Game.SCALE).toFloat(), params)
        }
        with(Image.Gui.DEFAULT_EDGE_LEFT) {
            batch.draw(this, absoluteX, absoluteY, originX, originY,
                    (this.width * Game.SCALE).toFloat(), absoluteHeight, params)
        }
        // origins need adjusting to keep them relative to the center of the rectangle not the texture
        with(Image.Gui.DEFAULT_EDGE_RIGHT) {
            val imgWidth = (this.width * Game.SCALE).toFloat()
            batch.draw(this, absoluteX + absoluteWidth - imgWidth, absoluteY, -(absoluteWidth / 2) + imgWidth, originY,
                    imgWidth, absoluteHeight, params)
        }
        with(Image.Gui.DEFAULT_EDGE_TOP) {
            val imgHeight = (this.height * Game.SCALE).toFloat()
            batch.draw(this, absoluteX, absoluteY + absoluteHeight - imgHeight, originX, -(absoluteHeight / 2) + imgHeight,
                    absoluteWidth, imgHeight, params)
        }
        batch.color = oldColor
        if (params.brightness > 1f) {
            brightPatch(absoluteX, absoluteY, originX, originY, absoluteWidth, absoluteHeight, params)
        } else if (params.brightness < 1f) {
            darkPatch(absoluteX, absoluteY, originX, originY, absoluteWidth, absoluteHeight, params)
        }
    }

    /**
     * Draws a 'default' rectangle, i.e. one using the Image.GUI.DEFAULT_[...] components of a rectangle. This is useful for
     * making more visually interesting backgrounds of sizes determined at runtime
     */
    fun renderDefaultRectangle(x: Int, y: Int, width: Int, height: Int) {
        val absoluteX = ((x + xOffset) * Game.SCALE).toFloat()
        val absoluteY = ((y + yOffset) * Game.SCALE).toFloat()
        val absoluteWidth = (width * Game.SCALE).toFloat()
        val absoluteHeight = (height * Game.SCALE).toFloat()
        // background
        batch.draw(Image.Gui.DEFAULT_BACKGROUND, absoluteX, absoluteY, absoluteWidth, absoluteHeight)
        // edges
        with(Image.Gui.DEFAULT_EDGE_RIGHT) {
            batch.draw(this, absoluteX + absoluteWidth - (this.width * Game.SCALE), absoluteY,
                    (this.width * Game.SCALE).toFloat(), absoluteHeight)
        }
        with(Image.Gui.DEFAULT_EDGE_BOTTOM) {
            batch.draw(this, absoluteX, absoluteY,
                    absoluteWidth, (this.height * Game.SCALE).toFloat())
        }
        with(Image.Gui.DEFAULT_EDGE_LEFT) {
            batch.draw(this, absoluteX, absoluteY,
                    (this.width * Game.SCALE).toFloat(), absoluteHeight)
        }
        with(Image.Gui.DEFAULT_EDGE_TOP) {
            batch.draw(this, absoluteX, absoluteY + absoluteHeight - (this.height * Game.SCALE),
                    absoluteWidth, (this.height * Game.SCALE).toFloat())
        }
    }

    fun renderFilledRectangle(x: Int, y: Int, width: Int, height: Int, params: TextureRenderParams) {
        val absoluteX = ((x + xOffset) * Game.SCALE).toFloat()
        val absoluteY = ((y + yOffset) * Game.SCALE).toFloat()
        val absoluteWidth = (width * Game.SCALE).toFloat()
        val absoluteHeight = (height * Game.SCALE).toFloat()
        val originX = absoluteWidth / 2
        val originY = absoluteHeight / 2
        val oldColor = batch.color
        batch.color = params.color
        batch.draw(Image.Gui.WHITE_FILLER, absoluteX, absoluteY, originX, originY, absoluteWidth, absoluteHeight, params)
        batch.color = oldColor
    }

    fun renderFilledRectangle(x: Int, y: Int, width: Int, height: Int) {
        val absoluteX = ((x + xOffset) * Game.SCALE).toFloat()
        val absoluteY = ((y + yOffset) * Game.SCALE).toFloat()
        val absoluteWidth = (width * Game.SCALE).toFloat()
        val absoluteHeight = (height * Game.SCALE).toFloat()
        batch.draw(Image.Gui.WHITE_FILLER, absoluteX, absoluteY, absoluteWidth, absoluteHeight)
    }

    private fun brightPatch(absoluteX: Float, absoluteY: Float, originX: Float, originY: Float, absoluteWidth: Float, absoluteHeight: Float, params: TextureRenderParams) {
        val oldColor = batch.color
        batch.setColor(1f, 1f, 1f, params.brightness - 1f)
        batch.draw(Image.Gui.WHITE_FILLER, absoluteX, absoluteY, originX, originY, absoluteWidth, absoluteHeight, params)
        batch.color = oldColor
    }

    private fun darkPatch(absoluteX: Float, absoluteY: Float, originX: Float, originY: Float, absoluteWidth: Float, absoluteHeight: Float, params: TextureRenderParams) {
        val oldColor = batch.color
        batch.setColor(1f, 1f, 1f, 1f - params.brightness)
        batch.draw(Image.Gui.BLACK_FILLER, absoluteX, absoluteY, originX, originY, absoluteWidth, absoluteHeight, params)
        batch.color = oldColor
    }

    /**
     * @param borderThickness how thicc to make the lines
     */
    fun renderEmptyRectangle(x: Int, y: Int, width: Int, height: Int, borderThickness: Float = 1f, params: TextureRenderParams) {
        val absoluteX = ((x + xOffset) * Game.SCALE).toFloat()
        val absoluteY = ((y + yOffset) * Game.SCALE).toFloat()
        val absoluteWidth = (width * Game.SCALE).toFloat()
        val absoluteHeight = (height * Game.SCALE).toFloat()
        val absBorderThickness = borderThickness * Game.SCALE
        val oldColor = batch.color
        batch.color = params.color
        val originX = absoluteWidth / 2
        val originY = absoluteHeight / 2
        // bottom
        batch.draw(Image.Gui.WHITE_FILLER, absoluteX, absoluteY, originX, originY, absoluteWidth, absBorderThickness, params)
        // left
        batch.draw(Image.Gui.WHITE_FILLER, absoluteX, absoluteY, originX, originY, absBorderThickness, absoluteHeight, params)
        // right
        batch.draw(Image.Gui.WHITE_FILLER, absoluteX + absoluteWidth - absBorderThickness, absoluteY, originX, originY, absBorderThickness, absoluteHeight, params)
        // top
        batch.draw(Image.Gui.WHITE_FILLER, absoluteX, absoluteY + absoluteHeight - absBorderThickness, originX, originY, absoluteWidth, absBorderThickness, params)
        batch.color = oldColor
    }

    /**
     * @param borderThickness how thicc to make the lines
     */
    fun renderEmptyRectangle(x: Int, y: Int, width: Int, height: Int, borderThickness: Float = 1f) {
        val absoluteX = ((x + xOffset) * Game.SCALE + if (width < 0) (width * Game.SCALE) else 0).toFloat()
        val absoluteY = ((y + yOffset) * Game.SCALE + if (height < 0) (height * Game.SCALE) else 0).toFloat()
        val absoluteWidth = (Math.abs(width) * Game.SCALE).toFloat()
        val absoluteHeight = (Math.abs(height) * Game.SCALE).toFloat()
        val absBorderThickness = borderThickness * Game.SCALE
        // bottom
        batch.draw(Image.Gui.WHITE_FILLER, absoluteX, absoluteY, absoluteWidth, absBorderThickness)
        // left
        batch.draw(Image.Gui.WHITE_FILLER, absoluteX, absoluteY, absBorderThickness, absoluteHeight)
        // right
        batch.draw(Image.Gui.WHITE_FILLER, absoluteX + absoluteWidth - absBorderThickness, absoluteY, absBorderThickness, absoluteHeight)
        // top
        batch.draw(Image.Gui.WHITE_FILLER, absoluteX, absoluteY + absoluteHeight - absBorderThickness, absoluteWidth, absBorderThickness)
    }

    /**
     * Quicker method for rendering a texture that skips all parameter calculations. Renders a texture at the x and
     * y  with the given parameters
     */
    fun renderTexture(t: TextureRegion, x: Int, y: Int) {
        batch.draw(t, ((x + xOffset) * Game.SCALE).toFloat(), ((y + yOffset) * Game.SCALE).toFloat(), (t.width * Game.SCALE).toFloat(), (t.height * Game.SCALE).toFloat())
    }

    /**
     * Renders a texture at the x and y  with the given parameters
     */
    fun renderTexture(t: TextureRegion, x: Int, y: Int, params: TextureRenderParams) {
        val absoluteX = ((x + xOffset) * Game.SCALE).toFloat()
        val absoluteY = ((y + yOffset) * Game.SCALE).toFloat()
        val absoluteWidth = (t.width * Game.SCALE).toFloat()
        val absoluteHeight = (t.height * Game.SCALE).toFloat()
        val originX = absoluteWidth / 2
        val originY = absoluteHeight / 2
        val oldColor = batch.color
        batch.color = params.color
        batch.draw(t, absoluteX, absoluteY, originX, originY, absoluteWidth, absoluteHeight, params)
        batch.color = oldColor
    }

    /**
     * Renders a texture at the x and y , keeping the texture at its original aspect ratio but also fitting it inside
     * of the width and height s
     */
    fun renderTextureKeepAspect(t: TextureRegion, x: Int, y: Int, width: Int, height: Int) {
        var w = width
        var h = height
        if (t.width > t.height) {
            if (t.width > width) {
                w = width
                val ratio = width.toFloat() / t.width
                h = (t.height * ratio).toInt()
            }
        }
        if (t.height > t.width) {
            if (t.height > height) {
                h = height
                val ratio = height.toFloat() / t.height
                w = (t.width * ratio).toInt()
            }
        }
        Renderer.renderTexture(t, x + (width - w) / 2, y + (height - h) / 2, w, h)
    }

    /**
     * Renders a texture at the x and y , keeping the texture at its original aspect ratio but also fitting it inside
     * of the width and height s. The result of that has the render params applied to it
     */
    fun renderTextureKeepAspect(t: TextureRegion, x: Int, y: Int, width: Int, height: Int, params: TextureRenderParams) {
        var w = width
        var h = height
        if (t.width > t.height) {
            if (t.width > width) {
                w = width
                val ratio = width.toFloat() / t.width
                h = (t.height * ratio).toInt()
            }
        }
        if (t.height > t.width) {
            if (t.height > height) {
                h = height
                val ratio = height.toFloat() / t.height
                w = (t.width * ratio).toInt()
            }
        }
        Renderer.renderTexture(t, x + (width - w) / 2, y + (height - h) / 2, w, h, params)
    }

    /**
     * Renders a texture at the x and y , stretching it to fit the width and height s
     */
    fun renderTexture(t: TextureRegion, x: Int, y: Int, width: Int, height: Int) {
        batch.draw(t, ((x + xOffset) * Game.SCALE).toFloat(), ((y + yOffset) * Game.SCALE).toFloat(), (width * Game.SCALE).toFloat(), (height * Game.SCALE).toFloat())
    }

    /**
     * Renders a texture at the x and y  with the given params, stretching it to fit the width and height s
     */
    fun renderTexture(t: TextureRegion, x: Int, y: Int, width: Int, height: Int, params: TextureRenderParams) {
        val absoluteX = ((x + xOffset) * Game.SCALE).toFloat()
        val absoluteY = ((y + yOffset) * Game.SCALE).toFloat()
        val absoluteWidth = (width * Game.SCALE).toFloat()
        val absoluteHeight = (height * Game.SCALE).toFloat()
        val originX = absoluteWidth / 2
        val originY = absoluteHeight / 2
        val oldColor = batch.color
        batch.color = params.color
        batch.draw(t, absoluteX, absoluteY, originX, originY, absoluteWidth, absoluteHeight, params)
        batch.color = oldColor
    }

    /**
     * Renders the toString() of the given object at the x and y 
     * @param ignoreLines whether to ignore the '\n' character or not
     */
    fun renderText(text: Any?, x: Int, y: Int, ignoreLines: Boolean = false) {
        val f = TextManager.getFont()
        val font = f.font
        val s = text.toString()
        if (!ignoreLines && s.contains("\n")) {
            val lines = s.split("\n")
            lines.forEachIndexed { index, string ->
                font.draw(batch, string, ((x + xOffset) * Game.SCALE).toFloat(), ((y + f.charHeight + yOffset + ((f.charHeight + 1) * (lines.lastIndex - index))) * Game.SCALE))
            }
        } else {
            font.draw(batch, s, ((x + xOffset) * Game.SCALE).toFloat(), ((y + f.charHeight + yOffset) * Game.SCALE))
        }
    }

    /**
     * Renders the toString() of the given object at the x and y , accounting for newlines
     * @param params the rendering parameters to use for the text. Intended to be used by text tags
     * @param ignoreLines whether to ignore the '\n' character or not
     */
    fun renderText(text: Any?, x: Int, y: Int, params: TextRenderParams, ignoreLines: Boolean = false) {
        val f = TextManager.getFont(params.size, params.style)
        val font = f.font
        val oldColor = font.color
        font.color = params.color
        val s = text.toString()
        if (!ignoreLines && s.contains("\n")) {
            val lines = s.split("\n")
            lines.forEachIndexed { index, string ->
                font.draw(batch, string, ((x + xOffset) * Game.SCALE).toFloat(), ((y + f.charHeight + yOffset + ((f.charHeight + 1) * (lines.lastIndex - index))) * Game.SCALE))
            }
        } else {
            font.draw(batch, s, ((x + xOffset) * Game.SCALE).toFloat(), ((y + f.charHeight + yOffset) * Game.SCALE))
        }
        font.color = oldColor
    }

    /**
     * Renders a tagged text object
     * @params the parameters to use initially, may be changed by tags later
     */
    fun renderTaggedText(taggedText: TaggedText, x: Int, y: Int, params: TextRenderParams = TextRenderParams()) {
        val original = params.copy()
        val context = TextRenderContext(java.awt.Rectangle(x, y, 0, 0), params)
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