package screen

import graphics.Image
import graphics.Renderer
import io.Mouse
import io.PressType
import main.Game


class GUIDragGrip(parent: RootGUIElement,
                  name: String,
                  xAlignment: () -> Int, yAlignment: () -> Int,
                  open: Boolean = false,
                  layer: Int = parent.layer + 1,
                  val actOn: GUIWindow,
                  var keepInsideWindowBounds: Boolean = true) :
        GUIElement(parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }, open, layer) {

    var dragging = false
    var sXPixel = 0
    var sYPixel = 0
    var actOnSXPixel = 0
    var actOnSYPixel = 0

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
        when (type) {
            PressType.PRESSED -> {
                dragging = true
                sXPixel = Mouse.xPixel
                sYPixel = Mouse.yPixel
                actOnSXPixel = actOn.xPixel
                actOnSYPixel = actOn.yPixel
            }
            PressType.RELEASED -> dragging = false
        }
    }

    override fun onClose() {
        dragging = false
    }

    override fun render() {
        Renderer.renderTexture(Image.GUI.DRAG_GRIP, xPixel, yPixel)
    }

    override fun update() {
        if (dragging) {
            var nX = Mouse.xPixel - sXPixel + actOnSXPixel
            var nY = Mouse.yPixel - sYPixel + actOnSYPixel
            if (!keepInsideWindowBounds) {
                actOn.xPixel = nX
                actOn.yPixel = nY
            } else {
                actOn.xPixel = Math.max(0,
                        Math.min(
                                Game.WIDTH - actOn.widthPixels,
                                nX
                        ))
                actOn.yPixel = Math.max(0,
                        Math.min(
                                Game.HEIGHT - actOn.heightPixels,
                                nY
                        ))
            }
        }
    }

    companion object {
        const val WIDTH = 4
        const val HEIGHT = 4
    }
}