package screen.elements

import io.PressType

class GUIClickableRegion(parent: RootGUIElement, name: String, xAlignment: () -> Int, yAlignment: () -> Int, widthAlignment: () -> Int, heightAlignment: () -> Int,
                         val func: (PressType, Int, Int, Int, Boolean, Boolean, Boolean) -> Unit, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {
    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        func(type, xPixel, yPixel, button, shift, ctrl, alt)
    }
}