package screen

import io.*
import level.LevelObject
import level.moving.MovingObject

class ViewWindow(name: String,
                 xPixel: Int, yPixel: Int,
                 widthPixels: Int, heightPixels: Int,
                 var camera: LevelObject,
                 zoomLevel: Int = 10,
                 open: Boolean = false,
                 layer: Int = 0,
                 windowGroup: WindowGroup) :
        GUIWindow(name, xPixel, yPixel, widthPixels, heightPixels, open, layer, windowGroup),
        ControlPressHandler {

    val view = GUIView(rootChild, name,
            { 0 }, { 0 },
            { this.widthPixels }, { this.heightPixels },
            camera, zoomLevel, open)
    val outline = GUIOutline(view, name + " outline", open = open)
    val nameText = GUIText(view, name, 1, 4, name, layer = 1)
    val controls = mutableListOf<GUIElement>()

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.SCREEN, Control.UP, Control.DOWN, Control.LEFT, Control.RIGHT)
        nameText.transparentToInteraction = true
        controls.add(generateDimensionDragGrip(2, 2))
        controls.add(generateDragGrip(2))
        controls.add(generateCloseButton(2))
        controls.add(nameText)
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.RELEASED || camera !is MovingObject)
            return
        val c = p.control
        val m = camera as MovingObject
        if (c == Control.UP) {
            m.yVel--
        } else if (c == Control.DOWN) {
            m.yVel++
        } else if (c == Control.RIGHT) {
            m.xVel++
        } else if (c == Control.LEFT) {
            m.xVel--
        }
    }
}