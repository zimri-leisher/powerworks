package screen.element

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import main.toColor
import misc.Numbers
import misc.Coord
import screen.gui.GuiElement
import kotlin.math.absoluteValue
import kotlin.math.ceil

class ElementProgressArrow(parent: GuiElement, var from: GuiElement, var to: GuiElement,
                           var maxProgress: Int = 0, var getProgress: () -> Int = { 0 },
                           var fromDir: Int = 2,
                           var toDir: Int = 3,
                           var backgroundColor: Color = toColor(0xFFFFFF),
                           var progressColor: Color = toColor(0x00BC06)) :
        GuiElement(parent) {
    private var start = Coord(0, 0)
    private var end = Coord(0, 0)

    var currentProgress = 0

    val progress get() = currentProgress.toFloat() / maxProgress

    init {
        updateAlignments()
    }

    private fun updateAlignments() {
        start = Coord(from.absoluteX + getX(from, fromDir), from.absoluteY + getY(from, fromDir))
        end = Coord(to.absoluteX + getX(to, toDir), to.absoluteY + getY(to, toDir))
    }

    override fun render(params: TextureRenderParams?) {
        val actualParams = params ?: TextureRenderParams.DEFAULT
        if (from.open && to.open) {
            updateAlignments()
            val changeX = end.x - start.x
            val changeY = end.y - start.y
            var toColor = ceil((changeX.absoluteValue + changeY.absoluteValue + 2) * progress).toInt()
            if (fromDir == 1 || fromDir == 3) {
                Renderer.renderFilledRectangle(start.x, start.y, changeX + if (changeX > 0) 2 else 0, 2, actualParams.combine(TextureRenderParams(color = backgroundColor)))
                if (toColor > 0) {
                    Renderer.renderFilledRectangle(start.x, start.y, Integer.min((changeX + if (changeX > 0) 2 else 0).absoluteValue, toColor) * Numbers.sign(changeX), 2, actualParams.combine(TextureRenderParams(color = progressColor)))
                    toColor -= (changeX + if (changeX > 0) 2 else 0).absoluteValue
                }
                Renderer.renderFilledRectangle(end.x, start.y, 2, changeY, actualParams.combine(TextureRenderParams(color = backgroundColor)))
                if (toColor > 0) {
                    Renderer.renderFilledRectangle(end.x, start.y, 2, Integer.min(changeY.absoluteValue, toColor) * Numbers.sign(changeY), actualParams.combine(TextureRenderParams(color = progressColor)))
                }
            } else {
                Renderer.renderFilledRectangle(start.x, start.y, 2, changeY + if (changeY > 0) 2 else 0, params = actualParams.combine(TextureRenderParams(color = backgroundColor)))
                if (toColor > 0) {
                    Renderer.renderFilledRectangle(start.x, start.y, 2, Integer.min((changeY + if (changeY > 0) 2 else 0).absoluteValue, toColor) * Numbers.sign(changeY), params = actualParams.combine(TextureRenderParams(color = progressColor)))
                    toColor -= (changeY + if (changeY > 0) 2 else 0).absoluteValue
                }
                Renderer.renderFilledRectangle(start.x, end.y, changeX, 2, params = actualParams.combine(TextureRenderParams(color = backgroundColor)))
                if (toColor > 0) {
                    Renderer.renderFilledRectangle(start.x, end.y, Integer.min(changeX.absoluteValue, toColor) * Numbers.sign(changeX), 2, params = actualParams.combine(TextureRenderParams(color = progressColor)))
                }
            }
        }
        super.render(params)
    }

    private fun getX(element: GuiElement, dir: Int) = when (dir % 4) {
        0, 2 -> element.width / 2
        1 -> element.width
        3 -> 0
        else -> 0
    }

    private fun getY(element: GuiElement, dir: Int) = when (dir % 4) {
        0 -> element.height
        1, 3 -> element.height / 2
        2 -> 0
        else -> 0
    }


}