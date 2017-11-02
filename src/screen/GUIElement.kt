package screen

import io.PressType

private var nextID = 0

open class RootGUIElement(val parentWindow: GUIWindow,
                          widthPixels: Int = parentWindow.widthPixels, heightPixels: Int = parentWindow.heightPixels,
                          open: Boolean = parentWindow.open, var layer: Int = 0) {
    val id = nextID++
    private val _children: MutableSet<GUIElement> = mutableSetOf()
    val children = object : MutableSet<GUIElement> by _children {
        override fun add(element: GUIElement): Boolean {
            val result = _children.add(element)
            if (result) {
                if (element.parent != this@RootGUIElement) {
                    if (element.matchParentLayer)
                        element.layer = element.parent.layer + 1
                    element.parent = this@RootGUIElement
                }
                this@RootGUIElement.onAddChild(element)
            }
            return result
        }
    }
    var open: Boolean = open
        set(value) {
            if (!value && field) {
                field = false
                mouseOn = false
                onClose()
                children.forEach { if (it.matchParentClosing) it.open = false }
            } else if (value && !field) {
                field = true
                mouseOn = ScreenManager.isMouseOn(this)
                onOpen()
                children.forEach {
                    if (it.matchParentOpening) {
                        it.open = true
                    }
                }
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
    open val xPixel
        get() = parentWindow.xPixel
    open val yPixel
        get() = parentWindow.yPixel
    var widthPixels = widthPixels
        set(value) {
            if (field != value) {
                val old = field
                field = value
                onDimensionChange(old, heightPixels)
                children.forEach {
                    if (it.adjustDimensions) {
                        val xRatio = widthPixels.toDouble() / old
                        it.widthPixels = (it.widthPixels * xRatio).toInt()
                    }
                    it.onParentDimensionChange(old, heightPixels)
                }
            }
        }

    var heightPixels = heightPixels
        set(value) {
            if (field != value) {
                val old = field
                field = value
                onDimensionChange(widthPixels, old)
                children.forEach {
                    if (it.adjustDimensions) {
                        val yRatio = heightPixels.toDouble() / old
                        it.heightPixels = (it.heightPixels * yRatio).toInt()
                    }
                    it.onParentDimensionChange(widthPixels, old)
                }
            }
        }
    open val name
        get() = parentWindow.name

    /* Util */
    /* Gets the specified element by name. If checkChildren is true (default), it checks recursively */
    fun getChild(name: String, checkChildren: Boolean = true): GUIElement? {
        var r = children.firstOrNull { it.name == name }
        if (checkChildren) {
            val i = children.iterator()
            while (r == null && i.hasNext()) {
                r = i.next().getChild(name)
            }
        }
        return r
    }

    /* Gets the specified element by id (unique for each element). If checkChildren is true (default), it checks recursively */
    fun getChild(id: Int, checkChildren: Boolean = true): GUIElement? {
        var r = children.firstOrNull { it.id == id }
        if (checkChildren) {
            val i = children.iterator()
            while (r == null && i.hasNext()) {
                r = i.next().getChild(id)
            }
        }
        return r
    }

    /* Settings */
    /** Open when the parent opens */
    var matchParentOpening = true
    /** Close when the parent closes */
    var matchParentClosing = true
    /** Send interactions to the parent */
    var transparentToInteraction = false
    /** Modify dimensions automatically when the parent's dimensions change */
    var adjustDimensions = false
    /** Follow parent's movement (if false, relative X and Y pixels will be adjusted to match */
    var adjustPosition = true
    /** Change the layer to be 1 above the parent's layer automatically */
    var matchParentLayer = true
    /** Have the ScreenManager's render() method call this classes render method. Only useful as false if this is is
     * being rendered by some other container, for instance, GUIGroup
     */
    var autoRender = true

    open fun render() {}

    open fun update() {}

    /* Events */
    /** When this is opened after being closed */
    open fun onOpen() {
    }

    /** When this is closed after being open */
    open fun onClose() {
    }

    /** When this gains a child */
    open fun onAddChild(child: GUIElement) {
    }

    /** When the mouse is clicked on this and it is on the highest layer, unless transparentToInteraction is true */
    open fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
    }

    /** When the mouse clicks off this */
    open fun onMouseActionOff(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
    }

    /** When the mouse enters the rectangle defined by xPixel, yPixel, widthPixels, heightPixels. Called even if it's on the bottom */
    open fun onMouseEnter() {
    }

    /** When the mouse leaves the rectangle defined by xPixel, yPixel, widthPixels, heightPixels. Called even if it's on the bottom layer */
    open fun onMouseLeave() {
    }

    /** When the mouse is scrolled and the mouse is on this element */
    open fun onMouseScroll(dir: Int) {
    }

    /** When either the width or height of this changes */
    open fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
    }

    /** When either the x or y pixel of this changes */
    open fun onPositionChange(pXPixel: Int, pYPixel: Int) {
    }

    override fun toString(): String = "Root child of $parentWindow"

    override fun equals(other: Any?): Boolean {
        return other is RootGUIElement && other.id == id
    }

    override fun hashCode() = id
}

abstract class GUIElement(parent: RootGUIElement,
                          override val name: String, relXPixel: Int, relYPixel: Int,
                          widthPixels: Int, heightPixels: Int,
                          open: Boolean = false,
                          layer: Int = parent.layer + 1) :
        RootGUIElement(parent.parentWindow, widthPixels, heightPixels, open, layer) {

    var parent: RootGUIElement = parent
        set(value) {
            if (field != value) {
                field.children.remove(this)
                value.children.add(this)
                val v = field
                field = value
                onParentChange(v)
            }
        }

    final override var xPixel = parent.xPixel + relXPixel
        private set
    final override var yPixel = parent.yPixel + relYPixel
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

    init {
        parent.children.add(this)
    }

    constructor(parent: GUIWindow,
                name: String, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int,
                open: Boolean = false,
                layer: Int = parent.rootChild.layer + 1) :
            this(parent.rootChild, name, xPixel, yPixel, widthPixels, heightPixels, open, layer)

    /* Util */
    /** Opens if closed, closes if opened */
    fun toggle() {
        open = !open
    }

    /** Updates this elements position based on relative x and y pixel and recurses to children too */
    fun updatePosition() {
        val oldX = xPixel
        val oldY = yPixel
        xPixel = parent.xPixel + relXPixel
        yPixel = parent.yPixel + relYPixel
        onPositionChange(oldX, oldY)
        children.forEach {
            if (it.adjustPosition)
                it.updatePosition()
            it.onParentPositionChange(oldX, oldY)
        }
    }

    /** Makes this and all it's children's layers their respective parent's layer + 1 */
    fun compressLayer() {
        layer = parent.layer + 1
        children.forEach { it.compressLayer() }
    }

    /* Events */
    /** When the parent is changed */
    open fun onParentChange(oldParent: RootGUIElement) {
    }

    /** When a parent's dimension changes (note, no need to call super here, all adjusting stuff is done in separate functions) */
    open fun onParentDimensionChange(oldWidth: Int, oldHeight: Int) {
    }

    /** When a parent's dimension changes (note, no need to call super here, all adjusting stuff is done in separate functions) */
    open fun onParentPositionChange(pXPixel: Int, pYPixel: Int) {
    }

    init {
        this.parent.children.add(this).run { }
    }

    override fun toString(): String {
        return javaClass.simpleName + ": $name at $xPixel, $yPixel absolute, $relXPixel, $relYPixel relative, width: $widthPixels, height: $heightPixels, layer: $layer, parent: ${parent.name}"
    }
}