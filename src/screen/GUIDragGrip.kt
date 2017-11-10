package screen

import graphics.Image
import graphics.Renderer
import io.Mouse
import io.PressType
import main.Game
import misc.GeometryHelper


class GUIDragGrip(parent: RootGUIElement,
                  name: String,
                  xPixel: Int, yPixel: Int,
                  open: Boolean = false,
                  layer: Int = parent.layer + 1,
                  var keepInsideWindowBounds: Boolean = true) : GUIElement(parent, name, xPixel, yPixel, WIDTH, HEIGHT, open, layer) {

    var dragging = false
    var mXPixel = 0
    var mYPixel = 0

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
        when (type) {
            PressType.PRESSED -> {
                dragging = true
                mXPixel = Mouse.xPixel
                mYPixel = Mouse.yPixel
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
            var nX = 0
            var nY = 0
            var dX = Mouse.xPixel - mXPixel
            var dY = Mouse.yPixel - mYPixel
            if (parent is GUIElement) {
                val p = parent as GUIElement
                nX = p.relXPixel + dX
                nY = p.relYPixel + dY
            } else {
                nX = parentWindow.xPixel + dX
                nY = parentWindow.yPixel + dY
            }
            if (parent is GUIElement) {
                if (!keepInsideWindowBounds || GeometryHelper.contains(parentWindow.xPixel, parentWindow.yPixel, parentWindow.widthPixels, parentWindow.heightPixels,
                        nX, nY, parent.widthPixels, parent.heightPixels)) {
                    parent.xPixel = nX
                    parent.yPixel = nY
                }
            } else {
                if (!keepInsideWindowBounds || GeometryHelper.contains(0, 0, Game.WIDTH, Game.HEIGHT,
                        nX, nY, parentWindow.widthPixels, parentWindow.heightPixels)) {
                    parentWindow.xPixel = nX
                    parentWindow.yPixel = nY
                }
            }
            mXPixel = Mouse.xPixel
            mYPixel = Mouse.yPixel
        }
    }

    companion object {
        const val WIDTH = 4
        const val HEIGHT = 4
    }
}