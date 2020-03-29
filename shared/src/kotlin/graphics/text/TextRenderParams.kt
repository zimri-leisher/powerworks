package graphics.text

import com.badlogic.gdx.graphics.Color

data class TextRenderParams(
        var color: Color = Color(1f, 1f, 1f, 1f),
        var size: Int = TextManager.DEFAULT_SIZE,
        var style: FontStyle = FontStyle.PLAIN)