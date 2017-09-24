package screen

import graphics.Images
import graphics.Renderer
import io.InputManager
import io.PressType


class GUIDragGrip(parent: GUIElement, name: String, xPixel: Int, yPixel: Int) : GUIElement(parent, name, xPixel, yPixel, WIDTH, HEIGHT) {

    var dragging = false
    var mXPixel = 0
    var mYPixel = 0

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int) {
        when (type) {
            PressType.PRESSED -> {
                dragging = true
                mXPixel = InputManager.mouseXPixel
                mYPixel = InputManager.mouseYPixel
            }
            PressType.RELEASED -> dragging = false
        }
    }

    override fun onMouseActionOff(type: PressType, xPixel: Int, yPixel: Int) {
        if (type == PressType.RELEASED)
            dragging = false
    }

    override fun render() {
        Renderer.renderTexture(Images.GUI_DRAG_GRIP, xPixel, yPixel)
    }

    override fun update() {
        if (dragging) {
            val p = parent as GUIElement
            val mXPixel1 = InputManager.mouseXPixel
            val mYPixel1 = InputManager.mouseYPixel
            if (mXPixel1 != mXPixel) {
                p.relXPixel = p.relXPixel + (mXPixel1 - mXPixel)
                mXPixel = mXPixel1
            }
            if (mYPixel1 != mYPixel) {
                p.relYPixel = p.relYPixel + (mYPixel1 - mYPixel)
                mYPixel = mYPixel1
            }
        }
    }

    companion object {
        const val WIDTH = 8
        const val HEIGHT = 8
    }
}