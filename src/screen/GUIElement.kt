package screen

import io.PressType

private var nextID = 0

open class RootGUIElement(val parentWindow: GUIWindow,
                          /**
                           * Should return the requested dimension whenever necessary
                           * Will be used to update said dimension if it changes
                           * (think of it like: alignment is the calculator,
                           * the actual pixel value is where it gets stored for performance reasons)
                           */
                          widthAlignment: () -> Int, heightAlignment: () -> Int,
                          open: Boolean = parentWindow.open, var layer: Int = 0) {

    constructor(parentWindow: GUIWindow,
                widthPixels: Int, heightPixels: Int,
                open: Boolean = parentWindow.open,
                layer: Int = 0) :
            this(parentWindow, { widthPixels }, { heightPixels }, open, layer)

    var widthAlignment = widthAlignment
        set(value) {
            field = value
            widthPixels = value()
        }
    var heightAlignment = heightAlignment
        set(value) {
            field = value
            heightPixels = value()
        }
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
                if (element.open)
                    parentWindow.openChildren.add(element)
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
                parentWindow.openChildren.remove(this)
                onClose()
                children.forEach { if (it.matchParentClosing) it.open = false }
            } else if (value && !field) {
                field = true
                mouseOn = ScreenManager.isMouseOn(this)
                parentWindow.openChildren.add(this)
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
    open var xPixel
        get() = parentWindow.xPixel
        set(value) {
            parentWindow.xPixel = value
        }
    open var yPixel
        get() = parentWindow.yPixel
        set(value) {
            parentWindow.yPixel = value
        }
    var widthPixels = widthAlignment()
        set(value) {
            if (field != value) {
                val old = field
                field = value
                onDimensionChange(old, heightPixels)
                children.forEach {
                    if (updateDimension)
                        it.widthPixels = it.widthAlignment()
                    if (updatePosition)
                        it.xPixel = xPixel + it.xAlignment()
                    it.onParentDimensionChange(old, heightPixels)
                }
            }
        }

    var heightPixels = heightAlignment()
        set(value) {
            if (field != value) {
                val old = field
                field = value
                onDimensionChange(widthPixels, old)
                children.forEach {
                    if (updateDimension)
                        it.heightPixels = it.heightAlignment()
                    if (updatePosition)
                        it.yPixel = yPixel + it.yAlignment()
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
    var updatePosition = true
    var updateDimension = true
    /** Open when the parent opens */
    var matchParentOpening = true
    /** Close when the parent closes */
    var matchParentClosing = true
    /** Send interactions to the parent */
    var transparentToInteraction = false
    /** Modify dimensions automatically when the parent's dimensions change */
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
                          override val name: String,
                          xAlignment: () -> Int, yAlignment: () -> Int,
                          widthAlignment: () -> Int, heightAlignment: () -> Int,
                          open: Boolean = false,
                          layer: Int = parent.layer + 1) :
        RootGUIElement(parent.parentWindow, widthAlignment, heightAlignment, open, layer) {

    constructor(parent: RootGUIElement,
                name: String,
                relXPixel: Int, relYPixel: Int,
                widthPixels: Int, heightPixels: Int,
                open: Boolean = false,
                layer: Int = parent.layer + 1) :
            this(parent, name, { relXPixel }, { relYPixel }, { widthPixels }, { heightPixels }, open, layer)

    constructor(parentWindow: GUIWindow,
                name: String,
                xAlignment: () -> Int, yAlignment: () -> Int,
                widthAlignment: () -> Int, heightAlignment: () -> Int,
                open: Boolean = false,
                layer: Int = parentWindow.rootChild.layer + 1) :
            this(parentWindow.rootChild, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer)

    var xAlignment = xAlignment
        set(value) {
            field = value
            xPixel = parent.xPixel + value()
        }
    var yAlignment = yAlignment
        set(value) {
            field = value
            yPixel = parent.yPixel + value()
        }
    var parent: RootGUIElement = parent
        set(value) {
            if (field != value) {
                field.children.remove(this)
                if (open)
                    field.parentWindow.openChildren.remove(this)
                value.children.add(this)
                val v = field
                field = value
                onParentChange(v)
            }
        }

    /**
     * THIS SETTER IS MEANT FOR PRIVATE USE ONLY PLS NO USE
     */
    final override var xPixel = parent.xPixel + xAlignment()
        set(value) {
            if (field != value) {
                val old = field
                field = value
                onPositionChange(old, yPixel)
                children.forEach {
                    if (updateDimension)
                        it.widthPixels = it.widthAlignment()
                    if (updatePosition)
                        it.xPixel = it.xAlignment() + xPixel
                    it.onParentPositionChange(old, yPixel)
                }
            }
        }

    /**
     * THIS SETTER IS MEANT FOR PRIVATE USE ONLY PLS NO USE
     */
    final override var yPixel = parent.yPixel + yAlignment()
        set(value) {
            if (field != value) {
                val old = field
                field = value
                onPositionChange(xPixel, old)
                children.forEach {
                    if (updateDimension)
                        it.heightPixels = it.heightAlignment()
                    if (updatePosition)
                        it.yPixel = it.yAlignment() + yPixel
                    it.onParentPositionChange(xPixel, old)
                }
            }
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

    override fun equals(other: Any?): Boolean {
        return other is GUIElement && other.id == id
    }

    override fun toString(): String {
        return javaClass.simpleName + ": $name at $xPixel, $yPixel absolute, ${xAlignment()}, ${yAlignment()} relative, width: $widthPixels, height: $heightPixels, layer: $layer, parent: ${parent.name}"
    }
}