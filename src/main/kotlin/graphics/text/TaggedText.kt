package graphics.text

import java.awt.Color
import main.ResourceManager
import graphics.Renderer

data class TaggedText(val text: String, val tags: Map<Int, List<TextTag>>) {
    companion object {
        val TAG_BEGIN_CHAR = '<'
        val TAG_END_CHAR = '>'
        val TAG_ARG_CHAR = '='
    }
}

class TagParseException(message: String) : Exception(message)

enum class TextTagType(val identifier: String, val execute: (context: TextRenderContext, arg: String) -> Unit, val aliases: List<String> = listOf()) {
    DEFAULT("default", { context, _ ->
        context.currentRenderParams.color = 0xFFFFFF
        context.currentRenderParams.size = TextManager.DEFAULT_SIZE
        context.currentRenderParams.style = FontStyle.PLAIN
    }, listOf("d")),
    SIZE("size", { context, arg ->
        context.currentRenderParams.size = arg.toInt()
    }),
    COLOR("color", { context, arg ->
        context.currentRenderParams.color = try {
            java.awt.Color.decode(arg).rgb
        } catch (e: NumberFormatException) {
            misc.Color.toColor(arg)?.rgb ?: throw TagParseException("Invalid color")
        }
    }),
    BOLD("bold", { context, _ ->
        context.currentRenderParams.style = FontStyle.BOLD
    }, listOf("b")),
    ITALIC("italic", { context, _ ->
        context.currentRenderParams.style = FontStyle.ITALIC
    }, listOf("i", "italics")),
    PLAIN("plain", { context, _ ->
        context.currentRenderParams.style = FontStyle.PLAIN
    }, listOf("p")),
    STYLE("style", { context, arg ->
        context.currentRenderParams.style = FontStyle.valueOf(arg.toUpperCase().replace(' ', '_'))
    }),
    IMAGE("image", { context, arg ->
        val image = main.ResourceManager.getImage(arg)
        val info = graphics.text.TextManager.getFont(context.currentRenderParams.size, context.currentRenderParams.style)
        val size = Math.max(info.charHeight, info.charWidth)
        graphics.Renderer.renderTextureKeepAspect(image, context.currentXPixel, context.currentYPixel, size, size)
        context.currentXPixel += size
    }, listOf("img"));
}