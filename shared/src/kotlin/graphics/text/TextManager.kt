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
import java.util.*
import java.util.regex.Pattern
import kotlin.math.ceil

data class FontInfo(val font: BitmapFont, val charWidth: Float, val charHeight: Float)

/**
 * An object with various utility methods related to text. This is where you can create [TaggedText] objects and get
 * the dimensions of strings
 */
object TextManager {

    private val fonts = mutableMapOf<Pair<Int, FontStyle>, FontInfo>()
    private val fontFile = Gdx.files.internal("font/Graph-35-pix.ttf")
    private var fontGenerator = FreeTypeFontGenerator(fontFile)
    lateinit var defaultFont: BitmapFont
    val glyphLayout = GlyphLayout()
    const val DEFAULT_SIZE = 20
    const val DEFAULT_CHARS = """1234567890qwertyuiopasdfghjklzxcvbnm`[]\;',./QWERTYUIOPASDFGHJKLZXCVBNM{}|:"<>?!@#$%^&*()_-+="""

    init {
        try {
            val defaultParams = FreeTypeFontGenerator.FreeTypeFontParameter()
            with(defaultParams) {
                size = DEFAULT_SIZE
                characters = DEFAULT_CHARS
            }
            defaultFont = fontGenerator.generateFont(defaultParams)
            glyphLayout.setText(defaultFont, DEFAULT_CHARS)
            fonts.put(Pair(DEFAULT_SIZE, FontStyle.PLAIN), FontInfo(defaultFont, (glyphLayout.width / DEFAULT_CHARS.length) / Game.SCALE, glyphLayout.height / Game.SCALE))
        } catch (ex: FontFormatException) {
            ex.printStackTrace(System.err)
        } catch (ex: IOException) {
            ex.printStackTrace(System.err)
        }
    }

    /**
     * Goes through a string containing [text tags][TextTagType] and returns a pre-generated [TaggedText]
     * object that can be passed into the [graphics.Renderer.renderTaggedText] method
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
                val tagName = stringInside.substringBefore(TaggedText.TAG_ARG_CHAR).lowercase(Locale.getDefault())
                val tagType =
                    TextTagType.values().firstOrNull { it.identifier == tagName || it.aliases.any { it == tagName } }
                if (tagType == null) {
                    untaggedString.append(char)
                    i++
                    continue
                }
                val tagArgument =
                    if (stringInside.contains(TaggedText.TAG_ARG_CHAR)) stringInside.substringAfter(TaggedText.TAG_ARG_CHAR)
                        .trim('"') else ""
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
     * @return a [FontInfo] object with information about the dimensions of characters in the font,
     * along with the [BitmapFont] object itself
     */
    fun getFont(size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): FontInfo {
        var font = fonts.get(Pair(size, style))
        if (font != null)
            return font
        font = genFont(size, style)
        fonts.put(Pair(size, style), font)
        return font
    }

    private fun genFont(size: Int, style: FontStyle): FontInfo {
        println("Generating new font")
        val param = FreeTypeFontGenerator.FreeTypeFontParameter()
        with(param) {
            this.size = size
            this.characters = DEFAULT_CHARS
        }
        val f = fontGenerator.generateFont(param)
        glyphLayout.setText(f, DEFAULT_CHARS)
        return FontInfo(f, ((glyphLayout.width / DEFAULT_CHARS.length) / Game.SCALE), ((glyphLayout.height / Game.SCALE)))
    }

    /**
     * @return the width and height of the given string at the given size, including newlines
     */
    fun getStringBounds(s: String, size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): Rectangle {
        if (s.isEmpty())
            return Rectangle(0, 0)
        val lines = s.split("\n")
        val f = getFont(size, style)
        return Rectangle(ceil(lines.maxBy { it.length }!!.length * f.charWidth).toInt(), ceil(lines.size * f.charHeight).toInt())
    }

    /**
     * @return the width of the string. If there are newlines, this will be the length of the longest line
     */
    fun getStringWidth(s: String, size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): Int {
        if (s.isEmpty())
            return 0
        val lines = s.split("\n")
        val f = getFont(size, style)
        return ceil((lines.maxBy { it.length }!!.length * f.charWidth)).toInt()
    }

    /**
     * @return the height of the string. This is essentially the character height times the number of lines
     */
    fun getStringHeight(s: String, size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): Int {
        if (s.isEmpty())
            return 0
        val lines = s.split("\n")
        val f = getFont(size, style)
        return ceil((lines.size * f.charHeight)).toInt()
    }

    /**
     * @return the width and height of the given tagged text at the given size, including newlines and tags
     */
    fun getStringBounds(t: TaggedText, size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): Rectangle {
        val context = TextRenderContext(Rectangle(0, 0, 0, 0), TextRenderParams(size = size, style = style))
        var lastIndex = 0
        for((index, tags) in t.tags) {
            val previousText = t.text.substring(lastIndex, index)
            val substringBounds = getStringBounds(previousText, context.currentRenderParams.size, context.currentRenderParams.style)
            context.currentBounds.width += substringBounds.width
            context.currentBounds.height = Math.max(context.currentBounds.height, substringBounds.height)
            tags.forEach { it.type.execute(context, it.argument, true) }
            lastIndex = index
        }
        val lastSubstringBounds = getStringBounds(t.text.substring(lastIndex), context.currentRenderParams.size, context.currentRenderParams.style)
        context.currentBounds.width += lastSubstringBounds.width
        context.currentBounds.height = Math.max(context.currentBounds.height, lastSubstringBounds.height)
        return context.currentBounds
    }

    /**
     * @return the width of the given tagged text at the given size, including newlines and tags
     */
    fun getStringWidth(t: TaggedText, size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): Int {
        return getStringBounds(t, size, style).width // TODO this (and the height) could probably be optimized
    }

    /**
     * @return the height of the given tagged text at the given size, including newlines and tags
     */
    fun getStringHeight(t: TaggedText, size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): Int {
        return getStringBounds(t, size, style).height
    }

    /**
     * Disposes of resources loaded through this
     */
    fun dispose() {
        fontGenerator.dispose()
        fonts.forEach { _, u -> u.font.dispose() }
    }
}