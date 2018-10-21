package graphics.text

import data.ResourceManager
import kotlin.math.roundToInt

data class TaggedText(val text: String, val tags: Map<Int, List<TextTag>>) {
    companion object {
        val TAG_BEGIN_CHAR = '<'
        val TAG_END_CHAR = '>'
        val TAG_ARG_CHAR = '='
    }
}

class TagParseException(message: String) : Exception(message)

enum class TextTagType(val identifier: String,
                       val execute: (context: TextRenderContext,
                                     arg: String,
                                     /**
                                      * Whether or not to actually draw stuff to the screen. This is used when just
                                      * checking bounds
                                      */
                                     dontRender: Boolean) -> Unit,
                       val aliases: List<String> = listOf()) {
    DEFAULT("default", { context, _, _ ->
        context.currentRenderParams.color.set(0xFFFFFF)
        context.currentRenderParams.size = TextManager.DEFAULT_SIZE
        context.currentRenderParams.style = FontStyle.PLAIN
    }, listOf("d")),
    SIZE("size", { context, arg, _ ->
        context.currentRenderParams.size = arg.toInt()
    }),
    COLOR("color", { context, arg, _ ->
        context.currentRenderParams.color.set(try {
            java.awt.Color.decode(arg).rgb
        } catch (e: NumberFormatException) {
            misc.Color.toColor(arg)?.rgb ?: throw TagParseException("Invalid color")
        })
    }),
    BOLD("bold", { context, _, _ ->
        context.currentRenderParams.style = FontStyle.BOLD
    }, listOf("b")),
    ITALIC("italic", { context, _, _ ->
        context.currentRenderParams.style = FontStyle.ITALIC
    }, listOf("i", "italics")),
    PLAIN("plain", { context, _, _ ->
        context.currentRenderParams.style = FontStyle.PLAIN
    }, listOf("p")),
    STYLE("style", { context, arg, _ ->
        context.currentRenderParams.style = FontStyle.valueOf(arg.toUpperCase().replace(' ', '_'))
    }),
    IMAGE("image", { context, arg, dontRender ->
        val image = ResourceManager.getAtlasTexture(arg)
        val info = graphics.text.TextManager.getFont(context.currentRenderParams.size, context.currentRenderParams.style)
        val size = (Math.max(info.charHeight, info.charWidth)).roundToInt()
        // we're rendering this at the end of the string, thus we use the width plus the x for the x of the render, and same for the height
        // the reason for the isDrawing here is that this could in theory be called outside of the main render loop (usually when checking
        // the bounds of a tagged text object)
        if (!dontRender)
            graphics.Renderer.renderTextureKeepAspect(image, context.currentBounds.width + context.currentBounds.x, context.currentBounds.y, size, size)
        context.currentBounds.width += size
        context.currentBounds.height = Math.max(context.currentBounds.height, size)
    }, listOf("img"));
}