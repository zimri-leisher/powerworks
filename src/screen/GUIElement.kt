package screen

import io.PressType
import main.Game
import kotlin.properties.Delegates

object RootGUIElementObject : RootGUIElement()

private var nextID = 0

open abstract class RootGUIElement(xPixel: Int = 0, yPixel: Int = 0, var widthPixels: Int = Game.WIDTH, var heightPixels: Int = Game.HEIGHT, var layer: Int = 0, var adjustPosition: Boolean = true, var adjustDimensions: Boolean = false) {

    var xPixel: Int by Delegates.observable(xPixel) { _, o, _ -> children.forEach { it.onParentPositionChange(o, yPixel) } }
    var yPixel: Int by Delegates.observable(yPixel) { _, o, _ -> children.forEach { it.onParentPositionChange(xPixel, o) } }
    private var xRatio: Double = xPixel.toDouble() / Game.WIDTH
    private var yRatio: Double = yPixel.toDouble() / Game.HEIGHT
    private var widthRatio: Double = widthPixels.toDouble() / Game.WIDTH
    private var heightRatio: Double = heightPixels.toDouble() / Game.HEIGHT
    var id = nextID++
    var open: Boolean = false
        set(value) {
            if (!value && field) {
                field = false
                onClose()
                children.forEach { it.open = false }
            } else if (value && !field) {
                open = true
                onOpen()
                children.forEach { it.open = true }
            }
        }
    var mouseOn: Boolean = false
        set(value) {
            if(value && !field) {
                onMouseEnter()
            } else if(!value && field) {
                onMouseLeave()
            }
        }

    private val _children: MutableList<GUIElement> = mutableListOf()
    val children: MutableList<GUIElement> = object : MutableList<GUIElement> by _children {
        override fun add(element: GUIElement): Boolean {
            //If its parent is already this than it must be in the children
            if (element.parent != this@RootGUIElement) {
                val result = _children.add(element)
                //Assume parent will be changed in the parent.set method
                return result
            }
            return false
        }
    }

    open fun onOpen() {

    }

    open fun onClose() {

    }

    fun toggle() {
        open = !open
    }

    open fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int) {

    }

    open fun onMouseActionOff(type: PressType, xPixel: Int, yPixel: Int) {

    }

    open fun onMouseEnter() {

    }

    open fun onMouseLeave() {

    }

    open fun onParentDimensionChange(oldWidth: Int, oldHeight: Int) {
        if (adjustPosition) {

        }
    }

    open fun render() {
        children.stream().sorted { o1, o2 -> o1.layer.compareTo(o2.layer) }.forEach { it.render() }
    }

    open fun update() {

    }
}

open abstract class GUIElement(parent: RootGUIElement = RootGUIElementObject, relXPixel: Int = 0, relYPixel: Int = 0, widthPixels: Int, heightPixels: Int, layer: Int = parent.layer + 1, adjustPosition: Boolean = true, adjustDimensions: Boolean = false) : RootGUIElement(parent.xPixel + relXPixel, parent.yPixel + relYPixel, widthPixels, heightPixels, layer, adjustPosition, adjustDimensions) {

    var relXPixel: Int by Delegates.observable(relXPixel) { _, o, n -> xPixel = n + parent.xPixel; children.forEach { it.onParentPositionChange(o, yPixel) } }
    var relYPixel: Int by Delegates.observable(relYPixel) { _, o, n -> yPixel = n + parent.yPixel; children.forEach { it.onParentPositionChange(xPixel, o) } }

    var parent: RootGUIElement = parent
        set(value) {
            if (field != value) {
                field.children.remove(this)
                value.children.add(this)
                val oldParent = field
                field = value
                onParentPositionChange(oldParent.xPixel, oldParent.yPixel)
            }
        }

    init {
        /* Because the position has already been updated */
        parent.children.add(this)
        ScreenManager.guiElements.add(this)
    }

    open fun onParentPositionChange(oldX: Int, oldY: Int) {
        val thisOldX = xPixel
        val thisOldY = yPixel
        xPixel = parent.xPixel + relXPixel
        yPixel = parent.yPixel + relYPixel
        children.forEach { it.onParentPositionChange(thisOldX, thisOldY) }
    }
}