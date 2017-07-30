package screen

import graphics.Images
import graphics.RenderParams
import io.PressType
import main.Game
import java.util.stream.Collectors
import kotlin.properties.Delegates

object RootGUIElementObject : RootGUIElement()

/* Constants */
val GUI_BUTTON_DEFAULT_WIDTH = Images.GUI_BUTTON.widthPixels
val GUI_BUTTON_DEFAULT_HEIGHT = Images.GUI_BUTTON.heightPixels

private var nextID = 0

abstract class RootGUIElement(val name: String = "Root GUI element object", xPixel: Int = 0, yPixel: Int = 0, var widthPixels: Int = Game.WIDTH, var heightPixels: Int = Game.HEIGHT, var layer: Int = 0, var adjustPosition: Boolean = true, var adjustDimensions: Boolean = false) {

    var xPixel: Int by Delegates.observable(xPixel) { _, o, _ -> children.forEach { it.updatePosition(o, yPixel) } }
    var yPixel: Int by Delegates.observable(yPixel) { _, o, _ -> children.forEach { it.updatePosition(xPixel, o) } }
    private var xRatio: Double = xPixel.toDouble() / Game.WIDTH
    private var yRatio: Double = yPixel.toDouble() / Game.HEIGHT
    private var widthRatio: Double = widthPixels.toDouble() / Game.WIDTH
    private var heightRatio: Double = heightPixels.toDouble() / Game.HEIGHT
    val params = RenderParams()
    var id = nextID++
    var open: Boolean = false
        set(value) {
            if (!value && field) {
                field = false
                ScreenManager.openGuiElements.remove(this)
                onClose()
                children.forEach { it.open = false }
            } else if (value && !field) {
                field = true
                ScreenManager.openGuiElements.add(this)
                onOpen()
                children.forEach { it.open = true }
            }
        }
    var mouseOn: Boolean = false
        set(value) {
            if (value && !field) {
                onMouseEnter()
                field = value
            } else if (!value && field) {
                onMouseLeave()
                field = value
            }
        }

    private val _children: MutableSet<GUIElement> = mutableSetOf()
    val children = object : MutableSet<GUIElement> by _children {

        override fun add(element: GUIElement): Boolean {
            val result = _children.add(element)
            if(result) {
                if(element.parent != this@RootGUIElement) {
                    element.layer = element.parent.layer + 1
                    element.parent = this@RootGUIElement
                }
                this@RootGUIElement.onAddChild(element)
            }
            return result
        }
    }

    /** Gets a GUI element by its name from this element's children */
    fun MutableSet<GUIElement>.get(name: String): GUIElement? {
        return _children.firstOrNull { it.name == name }
    }

    open fun onOpen() {

    }

    open fun onClose() {

    }

    fun toggle() {
        open = !open
    }

    open fun onAddChild(child: GUIElement) {
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

    /* Take into account params! */
    open fun render() {
    }

    open fun update() {

    }

    override fun toString(): String {
        return "Root GUI element object"
    }
}

abstract class GUIElement(parent: RootGUIElement? = RootGUIElementObject,
                               name: String,
                               relXPixel: Int = 0, relYPixel: Int = 0,
                               widthPixels: Int, heightPixels: Int,
                               layer: Int = if (parent == null) 1 else parent.layer + 1,
                               adjustPosition: Boolean = true, adjustDimensions: Boolean = false) :
        RootGUIElement(name, (parent?.xPixel ?: 0) + relXPixel, (parent?.yPixel ?: 0) + relYPixel, widthPixels, heightPixels, layer, adjustPosition, adjustDimensions) {

    var parent: RootGUIElement = parent ?: RootGUIElementObject
        set(value) {
            if (field != value) {
                field.children.remove(this)
                value.children.add(this)
                val v = field
                field = value
                onParentChange(v)
            }
        }

    var relXPixel: Int by Delegates.observable(relXPixel) { _, o, n -> xPixel = n + this.parent.xPixel; this.children.forEach { it.updatePosition(o, yPixel) } }
    var relYPixel: Int by Delegates.observable(relYPixel) { _, o, n -> yPixel = n + this.parent.yPixel; this.children.forEach { it.updatePosition(xPixel, o) } }

    init {
        this.parent.children.add(this)
        ScreenManager.guiElements.add(this)
    }

    open fun onParentChange(oldParent: RootGUIElement) {
        updatePosition(oldParent.xPixel, oldParent.yPixel)
    }

    open fun updatePosition(oldX: Int, oldY: Int) {
        val thisOldX = xPixel
        val thisOldY = yPixel
        xPixel = parent.xPixel + relXPixel
        yPixel = parent.yPixel + relYPixel
        children.forEach { it.updatePosition(thisOldX, thisOldY) }
    }

    override fun toString(): String {
        return javaClass.simpleName + ": $name at $xPixel, $yPixel absolute, $relXPixel, $relYPixel relative, width: $widthPixels, height: $heightPixels, layer: $layer, parent: ${parent.name}"
    }

}