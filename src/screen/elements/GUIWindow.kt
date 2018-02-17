package screen.elements

import screen.ScreenManager
import screen.WindowGroup

open class GUIWindow(val name: String, xAlignment: () -> Int, yAlignment: () -> Int, widthAlignment: () -> Int, heightAlignment: () -> Int,
                     open: Boolean = false,
                     /** Subject to change if this is not a fixed window */
                     var layer: Int = 0,
                     windowGroup: WindowGroup) {

    constructor(name: String, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int,
                open: Boolean = false,
                /** Subject to change if this is not a fixed window */
                layer: Int = 0,
                windowGroup: WindowGroup) : this(name, { xPixel }, { yPixel }, { widthPixels }, { heightPixels }, open, layer, windowGroup)


    var xAlignment = xAlignment
        set(value) {
            field = value
            xPixel = value()
        }
    var yAlignment = yAlignment
        set(value) {
            field = value
            yPixel = value()
        }
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
    var windowGroup: WindowGroup = windowGroup
        set(value) {
            field.windows.remove(this)
            value.windows.add(this)
            field = value
        }

    var open = open
        set(value) {
            if (value && !field) {
                field = value
                ScreenManager.openWindows.add(this)
                rootChild.open = true
                onOpen()
            } else if (!value && field) {
                field = value
                ScreenManager.openWindows.remove(this)
                rootChild.open = false
                onClose()
            }
        }

    private val _openChildren = mutableListOf<RootGUIElement>()

    /**
     * Ordered constantly based on layer
     */
    val openChildren = object : MutableList<RootGUIElement> by _openChildren {
        override fun add(el: RootGUIElement): Boolean {
            val b = _openChildren.add(el)
            _openChildren.sortBy { it.layer }
            return b
        }
    }

    /** The child for which all children get added to */
    var rootChild = RootGUIElement(this, { this.widthPixels }, { this.heightPixels })
    val children
        get() = rootChild.children
    var widthPixels = widthAlignment()
        set(value) {
            if (field != value) {
                val old = field
                field = value
                onDimensionChange(old, heightPixels)
                rootChild.widthPixels = value
            }
        }

    var heightPixels = heightAlignment()
        set(value) {
            if (field != value) {
                val old = field
                field = value
                onDimensionChange(widthPixels, old)
                rootChild.heightPixels = value
            }
        }
    var xPixel = xAlignment()
        set(value) {
            if (field != value) {
                val old = field
                field = value
                rootChild.children.forEach {
                    it.xPixel = value + it.xAlignment()
                    it.onParentPositionChange(old, xPixel)
                }
            }
        }
    var yPixel = yAlignment()
        set(value) {
            if (field != value) {
                val old = field
                field = value
                rootChild.children.forEach {
                    it.yPixel = value + it.yAlignment()
                    it.onParentPositionChange(xPixel, old)
                }
            }
        }

    var topLeftGroup = AutoFormatGUIGroup(rootChild, name + " top left group", { 1 }, { 1 }, open = open, xPixelSeparation = 5)
    var topRightGroup = AutoFormatGUIGroup(rootChild, name + " top right group", { this.widthPixels - 5 }, { 1 }, open = open, xPixelSeparation = -5)
    var bottomRightGroup = AutoFormatGUIGroup(rootChild, name + " bottom right group", { this.widthPixels - 5 }, { this.heightPixels - 5 }, open = open, xPixelSeparation = -5)
    var bottomLeftGroup = AutoFormatGUIGroup(rootChild, name + " bottom left group", { this.widthPixels - 5 }, { 1 }, open = open, xPixelSeparation = 5)

    init {
        windowGroup.windows.add(this)
        ScreenManager.windows.add(this)
        if (open) {
            ScreenManager.openWindows.add(this)
        }
    }

    /* Settings */
    /** Automatically scale when the screen size changes */
    var adjustDimensions = false
    /** Send interactions to the window behind it */
    var transparentToInteraction = false
    /** If this should not interfere with sending controls to the level when selected */
    var partOfLevel = false

    /* Util */
    fun generateCloseButton(layer: Int = this.layer + 1, pos: Int = 1): GUICloseButton {
        return GUICloseButton(getGroup(pos), name + " close button", { 0 }, { 0 }, open, layer, this)
    }

    fun generateDragGrip(layer: Int = this.layer + 1, pos: Int = 1): GUIDragGrip {
        return GUIDragGrip(getGroup(pos), name + " drag grip", { 0 }, { 0 }, open, layer, this)
    }

    fun generateDimensionDragGrip(layer: Int = this.layer + 1, pos: Int = 1): GUIDimensionDragGrip {
        return GUIDimensionDragGrip(getGroup(pos), name + " dimension drag grip", { 0 }, { 0 }, open, layer, this)
    }

    private fun getGroup(pos: Int): AutoFormatGUIGroup {
        when (pos) {
            0 -> return topLeftGroup
            1 -> return topRightGroup
            2 -> return bottomRightGroup
            3 -> return bottomLeftGroup
        }
        return topRightGroup
    }

    /* Gets the specified element by name. If checkChildren is true (default), it checks recursively */
    fun getChild(name: String, checkChildren: Boolean = true): RootGUIElement? {
        var r = if (rootChild.name == name) rootChild else null
        if (checkChildren) {
            val i = children.iterator()
            while (r == null && i.hasNext()) {
                r = i.next().getChild(name)
            }
        }
        return r
    }

    /* Gets the specified element by id (unique for each element). If checkChildren is true (default), it checks recursively */
    fun getChild(id: Int, checkChildren: Boolean = true): RootGUIElement? {
        var r = if (rootChild.id == id) rootChild else null
        if (checkChildren) {
            val i = children.iterator()
            while (r == null && i.hasNext()) {
                r = i.next().getChild(id)
            }
        }
        return r
    }

    fun toggle() {
        open = !open
    }

    fun updateAlignment() {
        xPixel = xAlignment()
        yPixel = yAlignment()
        widthPixels = widthAlignment()
        heightPixels = heightAlignment()
    }

    /* Events */
    /** When this is opened after being closed */
    open fun onOpen() {
    }

    /** When this is closed after being open */
    open fun onClose() {
    }

    open fun update() {
        updateChild(rootChild)
    }

    /** When the user resizes the screen */
    open fun onScreenSizeChange(oldWidth: Int, oldHeight: Int) {
    }

    /** When the width or height of this changes */
    open fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
    }

    fun updateChild(c: RootGUIElement) {
        c.update()
        c.children.forEach { updateChild(it) }
    }

    override fun toString(): String {
        return "GUI window: $name at $xPixel, $yPixel, width: $widthPixels, height: $heightPixels, layer: $layer"
    }
}