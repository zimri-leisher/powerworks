package graphics.text

import main.Game
import main.ResourceManager
import java.awt.Font
import java.awt.FontFormatException
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.font.FontRenderContext
import java.io.IOException
import java.util.regex.Pattern

data class FontInfo(val font: Font, val charWidth: Int, val charHeight: Int)

object TextManager {

    private val defaultFontRenderContext = FontRenderContext(null, false, false)
    private val fonts = mutableMapOf<Pair<Int, FontStyle>, FontInfo>()
    private lateinit var defaultFont: Font
    const val DEFAULT_SIZE = 20
    private const val TESTING_STRING = "1234567890qwertyuiopasdfghjklzxcvbnm`=[]\\;',./QWERTYUIOPASDFGHJLZXCVBNM{}|:\"<>?"

    init {
        try {
            val font = Font.createFont(Font.TRUETYPE_FONT, ResourceManager.getRawResourceAsStream("/font/Graph-35-pix.ttf")).deriveFont(FontStyle.PLAIN.style, DEFAULT_SIZE.toFloat())
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            ge.registerFont(font)
            defaultFont = font
            val testBounds = font.getStringBounds(TESTING_STRING, defaultFontRenderContext).bounds.apply {
                setBounds(0, 0, width / Game.SCALE, height / Game.SCALE)
            }
            fonts.put(Pair(DEFAULT_SIZE, FontStyle.PLAIN), FontInfo(font, Math.ceil(testBounds.width.toDouble() / TESTING_STRING.length).toInt(), testBounds.height))
        } catch (ex: FontFormatException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

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

    private fun genFont(size: Int, style: FontStyle): FontInfo {
        println("genning new font")
        val f = defaultFont.deriveFont(style.style, size.toFloat())
        val testBounds = f.getStringBounds(TESTING_STRING, defaultFontRenderContext).bounds.apply {
            setBounds(0, 0, width / Game.SCALE, height / Game.SCALE)
        }
        return FontInfo(f, Math.ceil(testBounds.width.toDouble() / TESTING_STRING.length).toInt(), testBounds.height)
    }

    /**
     * @return the width and height of the given string at the given size, including newlines and assuming monospacing
     */
    fun getStringBounds(s: String, size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): Rectangle {
        if (s.isEmpty())
            return Rectangle(0, 0)
        val lines = s.split("\n")
        val f = getFont(size, style)
        return Rectangle(lines.maxBy { it.length }!!.length * f.charWidth, lines.size * f.charHeight)
    }

    fun getStringBounds(t: TaggedText, size: Int = DEFAULT_SIZE, style: FontStyle = FontStyle.PLAIN): Rectangle {
        val bounds = Rectangle(0, 0)
        val context = TextRenderContext(0, 0, TextRenderParams(size = size, style = style))
        var lastIndex = 0
        for((index, tags) in t.tags) {
            println("iterating through tags")
            val previousText = t.text.substring(lastIndex, index)
            val substringBounds = getStringBounds(previousText, context.currentRenderParams.size, context.currentRenderParams.style)
            bounds.add(Rectangle(context.currentXPixel, context.currentYPixel, substringBounds.width, substringBounds.height))
            tags.forEach { it.type.execute(context, it.argument) }
            lastIndex = index
        }
        val lastSubstringBounds = getStringBounds(t.text.substring(lastIndex), context.currentRenderParams.size, context.currentRenderParams.style)
        println("bounds of last rectangle: $lastSubstringBounds")
        bounds.add(Rectangle(context.currentXPixel, context.currentYPixel, lastSubstringBounds.width, lastSubstringBounds.height))
        println("text: $t, bounds: $bounds")
        return bounds
    }
}