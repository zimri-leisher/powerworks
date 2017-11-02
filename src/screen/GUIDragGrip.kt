package screen

import graphics.Image
import graphics.Renderer
import io.Mouse
import io.PressType


class GUIDragGrip(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, open: Boolean = false, layer: Int = parent.layer + 1) : GUIElement(parent, name, xPixel, yPixel, WIDTH, HEIGHT, open, layer) {

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

    override fun onMouseActionOff(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
        if (type == PressType.RELEASED)
            dragging = false
    }

    override fun render() {
        Renderer.renderTexture(Image.GUI.DRAG_GRIP, xPixel, yPixel)
    }

    override fun update() {
        if (dragging) {
            if(parent is GUIElement) {
                val p = parent as GUIElement
                val mXPixel1 = Mouse.xPixel
                val mYPixel1 = Mouse.yPixel
                if (mXPixel1 != mXPixel) {
                    p.relXPixel = p.relXPixel + (mXPixel1 - mXPixel)
                    mXPixel = mXPixel1
                }
                if (mYPixel1 != mYPixel) {
                    p.relYPixel = p.relYPixel + (mYPixel1 - mYPixel)
                    mYPixel = mYPixel1
                }
            } else {
                val mXPixel1 = Mouse.xPixel
                val mYPixel1 = Mouse.yPixel
                if (mXPixel1 != mXPixel) {
                    parent.parentWindow.xPixel = parent.xPixel + (mXPixel1 - mXPixel)
                    mXPixel = mXPixel1
                }
                if (mYPixel1 != mYPixel) {
                    parent.parentWindow.yPixel = parent.yPixel + (mYPixel1 - mYPixel)
                    mYPixel = mYPixel1
                }
            }
        }
    }

    companion object {
        const val WIDTH = 4
        const val HEIGHT = 4
    }
}