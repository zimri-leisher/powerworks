package graphics.text

import java.awt.Rectangle

data class TextRenderContext(var currentBounds: Rectangle, val currentRenderParams: TextRenderParams)