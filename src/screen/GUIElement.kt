package screen

import main.Game
import kotlin.properties.Delegates

object RootGUIElementObject : RootGUIElement()

private var nextID = 0

open class RootGUIElement(xPixel: Int = 0, yPixel: Int = 0, relXPixel: Int = 0, relYPixel: Int = 0, var widthPixels: Int = Game.WIDTH, var heightPixels: Int = Game.HEIGHT, var layer: Int = 0) {

    var xPixel: Int by Delegates.observable(xPixel) { _, _, _ -> children.forEach { it.onParentMove() } }
    var yPixel: Int by Delegates.observable(yPixel) { _, _, _ -> children.forEach { it.onParentMove() } }
    var relXPixel: Int by Delegates.observable(relXPixel) { _, _, _ -> children.forEach { it.onParentMove() } }
    var relYPixel: Int by Delegates.observable(relYPixel) { _, _, _ -> children.forEach { it.onParentMove() } }
    var id = nextID++
    var open: Boolean = false

    private val _children: MutableList<GUIElement> = mutableListOf()
    val children: MutableList<GUIElement> = object : MutableList<GUIElement> by _children {
        override fun add(element: GUIElement): Boolean {
            val result = _children.add(element)
            element.onParentMove()
            return result
        }
    }

    fun open() {
        if (!open) {
            open = true
            onOpen()
            children.forEach { it.open() }
        }
    }

    fun onOpen() {

    }

    fun close() {
        if (open) {
            open = false
            onClose()
            children.forEach { it.close() }
        }
    }

    fun onClose() {

    }

    fun toggle() {
        if (open)
            close()
        else
            open()
    }

    fun render() {
        children.stream().sorted { o1, o2 -> o1.layer.compareTo(o2.layer) }.forEach { it.render() }
    }


}

open class GUIElement(relXPixel: Int = 0, relYPixel: Int = 0, widthPixels: Int, heightPixels: Int, parent: RootGUIElement = RootGUIElementObject, layer: Int = parent.layer + 1) : RootGUIElement(parent.xPixel + relXPixel, parent.yPixel + relYPixel, relXPixel, relYPixel, widthPixels, heightPixels, layer) {

    var parent: RootGUIElement = parent
        set(value) {
            if (field.children.contains(this))
                field.children.remove(this)
            value.children.add(this)
        }

    init {
        /* Because the position has already been updated */
        parent.children.add(this)
    }

    fun onParentMove() {
        xPixel = parent.xPixel + relXPixel
        yPixel = parent.yPixel + relYPixel
        children.forEach { it.onParentMove() }
    }
}