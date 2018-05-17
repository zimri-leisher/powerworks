package graphics

import main.Game
import main.ResourceManager
import java.awt.Font
import java.awt.FontFormatException
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.font.FontRenderContext
import java.io.IOException

data class FontInfo(val font: Font, val charWidth: Int, val charHeight: Int)

object TextManager {

    private val defaultFontRenderContext = FontRenderContext(null, false, false)
    private val fonts = mutableMapOf<Int, FontInfo>()
    private lateinit var defaultFont: Font
    const val DEFAULT_SIZE = 20
    private const val TESTING_STRING = "1234567890qwertyuiopasdfghjklzxcvbnm`=[]\\;',./QWERTYUIOPASDFGHJLZXCVBNM{}|:\"<>?"

    init {
        try {
            val font = Font.createFont(Font.TRUETYPE_FONT, ResourceManager.getResourceAsStream("/font/Graph-35-pix.ttf")).deriveFont(Font.PLAIN, DEFAULT_SIZE.toFloat())
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            ge.registerFont(font)
            defaultFont = font
            val testBounds = font.getStringBounds(TESTING_STRING, defaultFontRenderContext).bounds.apply {
                setBounds(0, 0, width / Game.SCALE, height / Game.SCALE)
            }
            fonts.put(DEFAULT_SIZE, FontInfo(font, Math.ceil(testBounds.width.toDouble() / TESTING_STRING.length).toInt(), testBounds.height))
        } catch (ex: FontFormatException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    /**
     * @return a FontInfo object with information pertinent to the dimensions of each character in the font,
     * along with the Font object itself
     */
    fun getFont(size: Int = DEFAULT_SIZE): FontInfo {
        var font = fonts.get(size)
        if (font != null)
            return font
        font = genFont(size)
        fonts.put(size, font)
        return font
    }

    private fun genFont(size: Int): FontInfo {
        val f = defaultFont.deriveFont(size.toFloat())
        val testBounds = f.getStringBounds(TESTING_STRING, defaultFontRenderContext).bounds.apply {
            setBounds(0, 0, width / Game.SCALE, height / Game.SCALE)
        }
        return FontInfo(f, Math.ceil(testBounds.width.toDouble() / TESTING_STRING.length).toInt(), testBounds.height)
    }

    /**
     * @return the width and height of the given string at the given size, including newlines and assuming monospacing
     */
    fun getStringBounds(s: String): Rectangle {
        if(s.isEmpty())
            return Rectangle(0, 0)
        val lines = s.split("\n")
        val f = getFont()
        return Rectangle(lines.maxBy { it.length }!!.length * f.charWidth, lines.size * f.charHeight)
    }
}