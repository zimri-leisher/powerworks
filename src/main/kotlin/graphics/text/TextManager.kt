package graphics.text

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import main.Game
import misc.Numbers
import java.awt.FontFormatException
import java.awt.Rectangle
import java.io.IOException
import java.util.regex.Pattern

data class FontInfo(val font: BitmapFont, val charWidth: Float, val charHeight: Float)

object TextManager {

    private val fonts = mutableMapOf<Pair<Int, FontStyle>, FontInfo>()
    private val fontFile = Gdx.files.internal("font/Graph-35-pix.ttf")
    private var fontGenerator = FreeTypeFontGenerator(fontFile)
    lateinit var defaultFont: BitmapFont
    val glyphLayout = GlyphLayout()
    const val DEFAULT_SIZE = 20
    private const val DEFAULT_CHARS = "1234567890qwertyuiopasdfghjklzxcvbnm`=[]\\;',./QWERTYUIOPASDFGHJLZXCVBNM{}|:\"<>?"

    init {
        try {
            val defaultParams = FreeTypeFontGenerator.FreeTypeFontParameter()
            with(defaultParams) {
                size = DEFAULT_SIZE
                characters = DEFAULT_CHARS
            }
            defaultFont = fontGenerator.generateFont(defaultParams)
            glyphLayout.setText(defaultFont, DEFAULT_CHARS)
            fonts.put(Pair(DEFAULT_SIZE, FontStyle.PLAIN), FontInfo(defaultFont, Numbers.ceil((glyphLayout.width / DEFAULT_CHARS.length) / Game.SCALE).toFloat(), Math.ceil((glyphLayout.height.toDouble()) / Game.SCALE).toFloat()))
        } catch (ex: FontFormatException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    /**
     * Goes through a string containing text tags (see the TextTagType enum) and returns a pre-generated TaggedText
     * object that can be passed into the Renderer.renderTaggedText method
     */
    fun parseTags(text: String): TaggedText {
        val tags = mutableMapOf<Int, MutableList<TextTag>>()
        val untaggedString = StringBuilder()
        var i = 0
        var offset = 0
        while (i <= text.lastIndex) {
            val char = text[i]
            if (char == TaggedText.TAG_BEGIN_CHAR) {
                val stringAfter = text.substring(i + 1)
                // if there is no closer
                if (!stringAfter.contains(TaggedText.TAG_END_CHAR)) {
                    untaggedString.append(char)
                    i++
                    continue
                }
                val endOfTag = text.indexOf(TaggedText.TAG_END_CHAR, i)
                val stringInside = stringAfter.substringBefore(TaggedText.TAG_END_CHAR)
                // if there are spaces inside the brackets (excluding inside of a string argument)
                if (stringInside.replace(Pattern.compile("\".*\"", Pattern.DOTALL).toRegex(), "").contains(' ')) {
                    untaggedString.append(char)
                    i++
                    continue
                }
                val tagName = stringInside.substringBefore(TaggedText.TAG_ARG_CHAR).toLowerCase()
                val tagType = TextTagType.values().firstOrNull { it.identifier == tagName || it.aliases.any { it == tagName } }
                if (tagType == null) {
                    untaggedString.append(char)
                    i++
                    continue
                }
                val tagArgument = if (stringInside.contains(TaggedText.TAG_ARG_CHAR)) stringInside.substringAfter(TaggedText.TAG_ARG_CHAR).trim('"') else ""
                if ((i - offset) in tags) {
                    tags[i - offset]!!.add(TextTag(tagArgument, tagType))
                } else {
                    tags[i - offset] = mutableListOf(TextTag(tagArgument, tagType))
                }
                offset += stringInside.length + 2
                i = endOfTag + 1
            } else {
                i++
                untaggedString.append(char)
            }
        }
        return TaggedText(untaggedString.toString(), tags)
    }

    /**
     * @return a FontInfo object with information pertinent to the dimensions of each character in the font,
     * along with the Font object itself
     */
    fun getFont(size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): FontInfo {
        var font = fonts.get(Pair(size, style))
        if (font != null)
            return font
        font = genFont(size, style)
        fonts.put(Pair(size, style), font)
        return font
    }

    // TODO style doesn't work right now. Could create a default java font, then load it to bitmap?
    private fun genFont(size: Int, style: FontStyle): FontInfo {
        println("Generating new font")
        val param = FreeTypeFontGenerator.FreeTypeFontParameter()
        with(param) {
            this.size = size
            this.characters = DEFAULT_CHARS
        }
        val f = fontGenerator.generateFont(param)
        glyphLayout.setText(f, DEFAULT_CHARS)
        println("${(glyphLayout.width.toDouble() / DEFAULT_CHARS.length)}, ${(glyphLayout.width.toDouble() / DEFAULT_CHARS.length) / Game.SCALE}")
        return FontInfo(f, ((glyphLayout.width / DEFAULT_CHARS.length) / Game.SCALE), ((glyphLayout.height / Game.SCALE)))
    }

    /**
     * @return the width and height of the given string at the given size, including newlines and assuming monospacing
     */
    fun getStringBounds(s: String, size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): Rectangle {
        if (s.isEmpty())
            return Rectangle(0, 0)
        val lines = s.split("\n")
        val f = getFont(size, style)
        return Rectangle((lines.maxBy { it.length }!!.length * f.charWidth).toInt(), (lines.size * f.charHeight).toInt())
    }

    fun getStringBounds(t: TaggedText, size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): Rectangle {
        val context = TextRenderContext(Rectangle(0, 0, 0, 0), TextRenderParams(size = size, style = style))
        var lastIndex = 0
        for((index, tags) in t.tags) {
            val previousText = t.text.substring(lastIndex, index)
            val substringBounds = getStringBounds(previousText, context.currentRenderParams.size, context.currentRenderParams.style)
            context.currentBounds.width += substringBounds.width
            context.currentBounds.height = Math.max(context.currentBounds.height, substringBounds.height)
            tags.forEach { it.type.execute(context, it.argument) }
            lastIndex = index
        }
        val lastSubstringBounds = getStringBounds(t.text.substring(lastIndex), context.currentRenderParams.size, context.currentRenderParams.style)
        context.currentBounds.width += lastSubstringBounds.width
        context.currentBounds.height = Math.max(context.currentBounds.height, lastSubstringBounds.height)
        return context.currentBounds
    }

    fun dispose() {
        fontGenerator.dispose()
        fonts.forEach { _, u -> u.font.dispose() }
    }
}