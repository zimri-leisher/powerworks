package screen.elements

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import main.toColor
import misc.Numbers.sign
import misc.PixelCoord
import java.lang.Integer.min
import kotlin.math.absoluteValue
import kotlin.math.ceil

class GUIProgressArrow(parent: RootGUIElement, name: String,
                       val from: RootGUIElement, val fromDir: Int,
                       val to: RootGUIElement, val toDir: Int,
                       var maxProgress: Int,
                       var backgroundColor: Color = toColor(0xFFFFFF),
                       var progressColor: Color = toColor(0x00BC06),
                       open: Boolean = false,
                       layer: Int = parent.layer + 1) :
        GUIElement(parent, name, 0, 0, 0, 0, open, layer) {

    private val start = PixelCoord(0, 0)
    private val end = PixelCoord(0, 0)

    var currentProgress = 0

    val progress get() = currentProgress.toFloat() / maxProgress

    init {
        updateAlignments()
    }

    private fun updateAlignments() {
        start.xPixel = from.xPixel + getX(from, fromDir)
        start.yPixel = from.yPixel + getY(from, fromDir)
        end.xPixel = to.xPixel + getX(to, toDir)
        end.yPixel = to.yPixel + getY(to, toDir)
    }

    override fun render() {
        if (from.open && to.open) {
            updateAlignments()
            val changeX = end.xPixel - start.xPixel
            val changeY = end.yPixel - start.yPixel
            var pixelsToColor = ceil((changeX.absoluteValue + changeY.absoluteValue + 2) * progress).toInt()
            if (fromDir == 1 || fromDir == 3) {
                Renderer.renderFilledRectangle(start.xPixel, start.yPixel, changeX + if (changeX > 0) 2 else 0, 2, localRenderParams.combine(TextureRenderParams(color = backgroundColor)))
                if (pixelsToColor > 0) {
                    Renderer.renderFilledRectangle(start.xPixel, start.yPixel, min((changeX + if (changeX > 0) 2 else 0).absoluteValue, pixelsToColor) * sign(changeX), 2, localRenderParams.combine(TextureRenderParams(color = progressColor)))
                    pixelsToColor -= (changeX + if (changeX > 0) 2 else 0).absoluteValue
                }
                Renderer.renderFilledRectangle(end.xPixel, start.yPixel, 2, changeY, localRenderParams.combine(TextureRenderParams(color = backgroundColor)))
                if (pixelsToColor > 0) {
                    Renderer.renderFilledRectangle(end.xPixel, start.yPixel , 2, min(changeY.absoluteValue, pixelsToColor) * sign(changeY), localRenderParams.combine(TextureRenderParams(color = progressColor)))
                }
            } else {
                Renderer.renderFilledRectangle(start.xPixel, start.yPixel, 2, changeY + if (changeY > 0) 2 else 0, params = localRenderParams.combine(TextureRenderParams(color = backgroundColor)))
                if(pixelsToColor > 0) {
                    Renderer.renderFilledRectangle(start.xPixel, start.yPixel, 2, min((changeY + if(changeY > 0) 2 else 0).absoluteValue, pixelsToColor) * sign(changeY), params = localRenderParams.combine(TextureRenderParams(color = progressColor)))
                    pixelsToColor -= (changeY + if(changeY > 0) 2 else 0).absoluteValue
                }
                Renderer.renderFilledRectangle(start.xPixel, end.yPixel, changeX, 2, params = localRenderParams.combine(TextureRenderParams(color = backgroundColor)))
                if(pixelsToColor > 0) {
                    Renderer.renderFilledRectangle(start.xPixel, end.yPixel, min(changeX.absoluteValue, pixelsToColor) * sign(changeX), 2, params = localRenderParams.combine(TextureRenderParams(color = progressColor)))
                }
            }
        }
    }

    private fun getX(element: RootGUIElement, dir: Int) = when (dir % 4) {
        0, 2 -> element.alignments.width() / 2
        1 -> element.alignments.width()
        3 -> 0
        else -> 0
    }

    private fun getY(element: RootGUIElement, dir: Int) = when (dir % 4) {
        0 -> element.alignments.height()
        1, 3 -> element.alignments.height() / 2
        2 -> 0
        else -> 0
    }


}