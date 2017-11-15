package screen

open class GUIWindow(val name: String, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int,
                     open: Boolean = false,
                     /** Subject to change if this is not a fixed window */
                     var layer: Int = 0,
                     windowGroup: WindowGroup) {
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
    var xPixel = xPixel
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
    var yPixel = yPixel
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
    var widthPixels = widthPixels
        set(value) {
            if (field != value) {
                val old = field
                field = value
                onDimensionChange(old, heightPixels)
                rootChild.widthPixels = value
            }
        }

    var heightPixels = heightPixels
        set(value) {
            if (field != value) {
                val old = field
                field = value
                onDimensionChange(widthPixels, old)
                rootChild.heightPixels = value
            }
        }

    var topRightControlsGroup = AutoFormatGUIGroup(rootChild, name + " top right controls group", { this.widthPixels - 5 }, { 1 }, open, xPixelSeparation = -5)

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

    /* Util */
    fun generateCloseButton(layer: Int = this.layer + 1): GUICloseButton {
        return GUICloseButton(topRightControlsGroup, name + " close button", { 0 }, { 0 }, open, layer, this).run {
            topRightControlsGroup.children.add(this)
            this
        }
    }

    fun generateDragGrip(layer: Int = this.layer + 1): GUIDragGrip {
        return GUIDragGrip(topRightControlsGroup, name + " drag grip", { 0 }, { 0 }, open, layer, this).run {
            topRightControlsGroup.children.add(this)
            this
        }
    }

    fun generateDimensionDragGrip(layer: Int = this.layer + 1): GUIDimensionDragGrip {
        return GUIDimensionDragGrip(topRightControlsGroup, name + " dimension drag grip", { 0 }, { 0 }, open, layer, this).run {
            topRightControlsGroup.children.add(this)
            this
        }
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