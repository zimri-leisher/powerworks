package screen

import graphics.RenderParams
import io.PressType
import main.Game

object RootGUIElementObject : RootGUIElement()

private var nextID = 0

abstract class RootGUIElement(val name: String = "Root GUI element object", open val xPixel: Int = 0, open val yPixel: Int = 0, open val widthPixels: Int = Game.WIDTH, open val heightPixels: Int = Game.HEIGHT, var layer: Int = 0,
                              /** Whether or not to modify dimensions based on the ratio of the parent's previous dimension versus the new one */
                              var adjustDimensions: Boolean = false) {

    val params = RenderParams()
    var id = nextID++
    /** Whether or not the ScreenManager should render this */
    var autoRender = true
    var open: Boolean = false
        set(value) {
            if (!value && field) {
                field = false
                ScreenManager.openGuiElements.remove(this)
                mouseOn = false
                onClose()
                children.forEach { it.open = false }
            } else if (value && !field) {
                field = true
                ScreenManager.openGuiElements.add(this)
                ScreenManager.updateMouseOn(this)
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
            if (result) {
                if (element.parent != this@RootGUIElement) {
                    element.layer = element.parent.layer + 1
                    element.parent = this@RootGUIElement
                }
                this@RootGUIElement.onAddChild(element)
            }
            return result
        }
    }

    fun get(name: String, checkChildren: Boolean = true): GUIElement? {
        var r = children.firstOrNull { it.name == name }
        if (checkChildren) {
            val i = children.iterator()
            while (r == null && i.hasNext()) {
                r = i.next().get(name)
            }
        }
        return r
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

    open fun onMouseScroll(dir: Int) {
    }

    open fun onParentDimensionChange(oldWidth: Int, oldHeight: Int) {
    }

    open fun onParentPositionChange(pXPixel: Int, pYPixel: Int) {
    }

    open fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
    }

    open fun onPositionChange(pXPixel: Int, pYPixel: Int) {
    }

    open fun onParentChange(oldParent: RootGUIElement) {
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
                          adjustDimensions: Boolean = false) :
        RootGUIElement(name, (parent?.xPixel ?: 0) + relXPixel, (parent?.yPixel ?: 0) + relYPixel, widthPixels, heightPixels, layer, adjustDimensions) {

    open var parent: RootGUIElement = parent ?: RootGUIElementObject
        set(value) {
            if (field != value) {
                field.children.remove(this)
                value.children.add(this)
                val v = field
                field = value
                onParentChange(v)
            }
        }

    final override var widthPixels = widthPixels
        set(value) {
            val old = field
            field = value
            onDimensionChange(old, heightPixels)
            children.forEach {
                if (it.adjustDimensions) {
                    val xRatio = widthPixels / old
                    it.widthPixels *= xRatio
                }
                it.onParentDimensionChange(old, heightPixels)
            }
        }
    final override var heightPixels = heightPixels
        set(value) {
            val old = field
            field = value
            onDimensionChange(widthPixels, old)
            children.forEach {
                if (it.adjustDimensions) {
                    val yRatio = heightPixels / old
                    it.heightPixels *= yRatio
                }
                it.onParentDimensionChange(widthPixels, old)
            }
        }

    final override var xPixel = relXPixel + this.parent.xPixel
        private set
    final override var yPixel = relYPixel + this.parent.yPixel
        private set

    var relXPixel = relXPixel
        set(value) {
            field = value
            updatePosition()
        }
    var relYPixel = relYPixel
        set(value) {
            field = value
            updatePosition()
        }

    private fun updatePosition() {
        val oldX = xPixel
        val oldY = yPixel
        xPixel = parent.xPixel + relXPixel
        yPixel = parent.yPixel + relYPixel
        onPositionChange(oldX, oldY)
        children.forEach {
            it.updatePosition()
            it.onParentPositionChange(oldX, oldY)
        }
    }

    init {
        this.parent.children.add(this)
        open = this.parent.open
        ScreenManager.guiElements.add(this)
    }

    override fun toString(): String {
        return javaClass.simpleName + ": $name at $xPixel, $yPixel absolute, $relXPixel, $relYPixel relative, width: $widthPixels, height: $heightPixels, layer: $layer, parent: ${parent.name}"
    }

}