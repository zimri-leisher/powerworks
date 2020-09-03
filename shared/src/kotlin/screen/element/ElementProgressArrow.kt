package screen.element

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import main.toColor
import misc.Numbers
import misc.PixelCoord
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
    private var start = PixelCoord(0, 0)
    private var end = PixelCoord(0, 0)

    var currentProgress = 0

    val progress get() = currentProgress.toFloat() / maxProgress

    init {
        updateAlignments()
    }

    private fun updateAlignments() {
        start = PixelCoord(from.absoluteXPixel + getX(from, fromDir), from.absoluteYPixel + getY(from, fromDir))
        end = PixelCoord(to.absoluteXPixel + getX(to, toDir), to.absoluteYPixel + getY(to, toDir))
    }

    override fun render(params: TextureRenderParams?) {
        val actualParams = params ?: TextureRenderParams.DEFAULT
        if (from.open && to.open) {
            updateAlignments()
            val changeX = end.xPixel - start.xPixel
            val changeY = end.yPixel - start.yPixel
            var pixelsToColor = ceil((changeX.absoluteValue + changeY.absoluteValue + 2) * progress).toInt()
            if (fromDir == 1 || fromDir == 3) {
                Renderer.renderFilledRectangle(start.xPixel, start.yPixel, changeX + if (changeX > 0) 2 else 0, 2, actualParams.combine(TextureRenderParams(color = backgroundColor)))
                if (pixelsToColor > 0) {
                    Renderer.renderFilledRectangle(start.xPixel, start.yPixel, Integer.min((changeX + if (changeX > 0) 2 else 0).absoluteValue, pixelsToColor) * Numbers.sign(changeX), 2, actualParams.combine(TextureRenderParams(color = progressColor)))
                    pixelsToColor -= (changeX + if (changeX > 0) 2 else 0).absoluteValue
                }
                Renderer.renderFilledRectangle(end.xPixel, start.yPixel, 2, changeY, actualParams.combine(TextureRenderParams(color = backgroundColor)))
                if (pixelsToColor > 0) {
                    Renderer.renderFilledRectangle(end.xPixel, start.yPixel, 2, Integer.min(changeY.absoluteValue, pixelsToColor) * Numbers.sign(changeY), actualParams.combine(TextureRenderParams(color = progressColor)))
                }
            } else {
                Renderer.renderFilledRectangle(start.xPixel, start.yPixel, 2, changeY + if (changeY > 0) 2 else 0, params = actualParams.combine(TextureRenderParams(color = backgroundColor)))
                if (pixelsToColor > 0) {
                    Renderer.renderFilledRectangle(start.xPixel, start.yPixel, 2, Integer.min((changeY + if (changeY > 0) 2 else 0).absoluteValue, pixelsToColor) * Numbers.sign(changeY), params = actualParams.combine(TextureRenderParams(color = progressColor)))
                    pixelsToColor -= (changeY + if (changeY > 0) 2 else 0).absoluteValue
                }
                Renderer.renderFilledRectangle(start.xPixel, end.yPixel, changeX, 2, params = actualParams.combine(TextureRenderParams(color = backgroundColor)))
                if (pixelsToColor > 0) {
                    Renderer.renderFilledRectangle(start.xPixel, end.yPixel, Integer.min(changeX.absoluteValue, pixelsToColor) * Numbers.sign(changeX), 2, params = actualParams.combine(TextureRenderParams(color = progressColor)))
                }
            }
        }
        super.render(params)
    }

    private fun getX(element: GuiElement, dir: Int) = when (dir % 4) {
        0, 2 -> element.widthPixels / 2
        1 -> element.widthPixels
        3 -> 0
        else -> 0
    }

    private fun getY(element: GuiElement, dir: Int) = when (dir % 4) {
        0 -> element.heightPixels
        1, 3 -> element.heightPixels / 2
        2 -> 0
        else -> 0
    }


}