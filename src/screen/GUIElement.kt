package screen

import data.ScreenLocation

object NoParent : GUIElement(0, 0) {

}

open class GUIElement protected constructor(var loc: ScreenLocation, var parent: GUIElement = NoParent) {
    constructor(xPixel: Int, yPixel: Int) : this(ScreenLocation(xPixel, yPixel)) {}
}