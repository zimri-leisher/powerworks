package graphics.text

data class TextRenderParams(
        var color: Int = 0xFFFFFF,
        var size: Int = TextManager.DEFAULT_SIZE,
        var style: FontStyle = FontStyle.PLAIN)