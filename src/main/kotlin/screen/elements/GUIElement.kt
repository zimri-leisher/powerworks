package screen.elements

import data.WeakMutableList
import graphics.TextureRenderParams

import io.PressType
import main.Game
import screen.mouse.Mouse
import screen.ScreenManager
import screen.WindowGroup

typealias Alignment = () -> Int

private var nextId = 0

sealed class RootGUIElement(var name: String, xAlignment: Alignment, yAlignment: Alignment, widthAlignment: Alignment, heightAlignment: Alignment, open: Boolean, var layer: Int) {

    val id = nextId++

    var open: Boolean = open
        set(value) {
            if (!value && field) {
                field = false
                mouseOn = false
                if (this is GUIElement) {
                    parentWindow.openChildren.remove(this)
                    if (ScreenManager.selectedElement == this) {
                        ScreenManager.selectedElement = ScreenManager.getHighestElement(Mouse.xPixel, Mouse.yPixel)
                    }
                } else if (this is GUIWindow) {
                    ScreenManager.openWindows.remove(this)
                    if (ScreenManager.selectedWindow == this) {
                        ScreenManager.selectedWindow = ScreenManager.getHighestWindow(Mouse.xPixel, Mouse.yPixel)
                    }
                }
                onClose()
                children.forEach { if (it.matchParentClosing) it.open = false }
            } else if (value && !field) {
                field = true
                mouseOn = ScreenManager.isMouseOn(this)
                if (this is GUIElement)
                    parentWindow.openChildren.add(this)
                else if (this is GUIWindow) {
                    ScreenManager.openWindows.add(this)
                    if (openAtMouse) {
                        var x = Mouse.xPixel
                        if (x + widthPixels > Game.WIDTH)
                            x = Game.WIDTH - widthPixels
                        else if (x < 0)
                            x = 0
                        var y = Mouse.yPixel
                        if (y + heightPixels > Game.HEIGHT)
                            y = Game.HEIGHT - heightPixels
                        else if (y < 0)
                            y = 0
                        alignments.x = { x }
                        alignments.y = { y }
                    }
                }
                onOpen()
                children.forEach {
                    if (it.matchParentOpening) {
                        it.open = true
                    }
                }
            }
        }
    var alignments = ElementAlignments(this, xAlignment, yAlignment, widthAlignment, heightAlignment)
        set(value) {
            if (field != value) {
                field = value
                value.update()
            }
        }

    open var xPixel: Int = alignments.x()
        protected set(value) {
            if (field != value) {
                val old = field
                field = value
                onPositionChange(old, yPixel)
                children.forEach {
                    it.alignments.updatePosition()
                    it.onParentPositionChange(old, yPixel)
                }
            }
        }

    open var yPixel: Int = alignments.y()
        protected set(value) {
            if (field != value) {
                val old = field
                field = value
                onPositionChange(xPixel, old)
                children.forEach {
                    it.alignments.updatePosition()
                    it.onParentPositionChange(xPixel, old)
                }
            }
        }
    /**
     * This will be calculated and assigned to the width pixels every time a dimension or position of this or a parent changes,
     * or the updateAlignment() function is called
     */
    var widthPixels = alignments.width()
        protected set(value) {
            if (field != value) {
                val old = field
                field = value
                onDimensionChange(old, heightPixels)
                children.forEach {
                    it.alignments.updateDimension()
                    it.onParentDimensionChange(old, heightPixels)
                }
            }
        }

    /**
     * This will be calculated and assigned to the height pixels every time a dimension or position of this or a parent changes,
     * or the updateAlignment() function is called
     */
    var heightPixels = alignments.height()
        protected set(value) {
            if (field != value) {
                val old = field
                field = value
                onDimensionChange(widthPixels, old)
                children.forEach {
                    it.alignments.updateDimension()
                    it.onParentDimensionChange(widthPixels, old)
                }
            }
        }

    abstract val parentWindow: GUIWindow

    private val _children = mutableListOf<GUIElement>()

    val children = object : MutableList<GUIElement> by _children {
        override fun add(element: GUIElement): Boolean {
            if (_children.any { it.id == element.id })
                return false
            val result = _children.add(element)
            if (result) {
                if (element.parent != this@RootGUIElement) {
                    if (element.matchParentLayer)
                        element.layer = element.parent.layer + 1
                    element.parent = this@RootGUIElement
                }
                if (element.open)
                    parentWindow.openChildren.add(element)
                element.autoRender = autoRender
                this@RootGUIElement.onAddChild(element)
            }
            return result
        }

        override fun remove(element: GUIElement): Boolean {
            val result = _children.remove(element)
            if (result) {
                if (element.open) {
                    parentWindow.openChildren.remove(element)
                }
                this@RootGUIElement.onRemoveChild(element)
            }
            return result
        }

        override fun clear() {
            val i = _children.iterator()
            for (child in i) {
                i.remove()
                if (child.open)
                    parentWindow.openChildren.remove(child)
                this@RootGUIElement.onRemoveChild(child)
            }
        }
    }

    /**
     * TODO this should be the combination of the parent's totalRenderParams and this's localRenderParams
     */
    var totalRenderParams = TextureRenderParams()
    /**
     * The parameters used for all rendering done by this element.
     */
    var localRenderParams = TextureRenderParams()

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

    fun anyChild(predicate: (RootGUIElement) -> Boolean): Boolean {

        fun recursivelyFind(predicate: (RootGUIElement) -> Boolean, e: RootGUIElement): Boolean {
            e.children.forEach {
                if (predicate(it))
                    return true
                if (recursivelyFind(predicate, it))
                    return true
            }
            return false
        }

        return recursivelyFind(predicate, this)
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
    /* Settings */
    /** Open when the parent opens */
    var matchParentOpening = true
    /** Close when the parent closes */
    var matchParentClosing = true
    /** Send interactions to the parent */
    var transparentToInteraction = false
    /** Modify dimensions automatically when the parent's dimensions change */
    var matchParentLayer = true

    /**
     * Have the ScreenManager's render() method call this classes render method. Only useful as false if this is is
     * being rendered by some other container, for instance, GUIElementList
     */
    var autoRender = true
        set(value) {
            if (field != value) {
                field = value
                children.forEach { it.autoRender = value }
            }
        }

    init {
        if (open)
            mouseOn = ScreenManager.isMouseOn(this)
    }

    /** Opens if closed, closes if opened */
    fun toggle() {
        open = !open
    }

    open fun render() {}

    open fun update() {}

    /* Events */
    open fun onOpen() {
    }

    open fun onClose() {
    }

    open fun onAddChild(child: GUIElement) {
    }

    open fun onRemoveChild(child: GUIElement) {
    }

    open fun onChildDimensionChange(child: GUIElement) {
    }

    /** When the mouse is clicked on this and it is on the highest layer, unless transparentToInteraction is true */
    open fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
    }

    open fun onInteractOff(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {

    }

    /** When the mouse is scrolled and the mouse is on this element */
    open fun onScroll(dir: Int) {
    }

    /** When the mouse enters the rectangle defined by xPixel, yPixel, widthPixels, heightPixels. Called even if it's on the bottom */
    open fun onMouseEnter() {
    }

    /** When the mouse leaves the rectangle defined by xPixel, yPixel, widthPixels, heightPixels. Called even if it's on the bottom layer */
    open fun onMouseLeave() {
    }

    /** When either the width or height of this changes */
    open fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
    }

    /** When the user resizes the screen */
    open fun onScreenSizeChange(oldWidth: Int, oldHeight: Int) {
    }

    /** When either the x or y pixel of this changes */
    open fun onPositionChange(pXPixel: Int, pYPixel: Int) {
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RootGUIElement
        if (name != other.name) return false
        if (layer != other.layer) return false
        if (open != other.open) return false
        if (alignments != other.alignments) return false
        if (xPixel != other.xPixel) return false
        if (yPixel != other.yPixel) return false
        if (widthPixels != other.widthPixels) return false
        if (heightPixels != other.heightPixels) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + id
        result = 31 * result + layer
        result = 31 * result + open.hashCode()
        result = 31 * result + alignments.hashCode()
        result = 31 * result + xPixel
        result = 31 * result + yPixel
        result = 31 * result + widthPixels
        result = 31 * result + heightPixels
        return result
    }

    class ElementAlignments(parent: RootGUIElement, x: Alignment, y: Alignment, width: Alignment, height: Alignment) {
        var parent = parent
            set(value) {
                if(field != value) {
                    field = value
                    update()
                }
            }

        var x = x
            set(value) {
                if (field !== value) {
                    field = value
                    parent.xPixel = value() + ((parent as? GUIElement)?.parent?.xPixel ?: 0)
                }
            }
        var y = y
            set(value) {
                if (field !== value) {
                    field = value
                    parent.yPixel = value() + ((parent as? GUIElement)?.parent?.yPixel ?: 0)
                }
            }
        var width = width
            set(value) {
                if (field !== value) {
                    field = value
                    parent.widthPixels = value()
                }
            }
        var height = height
            set(value) {
                if (field !== value) {
                    field = value
                    parent.heightPixels = value()
                }
            }

        fun updatePosition() {
            if (parent is GUIElement) {
                parent.xPixel = x() + (parent as GUIElement).parent.xPixel
                parent.yPixel = y() + (parent as GUIElement).parent.yPixel
            } else {
                parent.xPixel = x()
                parent.yPixel = y()
            }
        }

        fun updateDimension() {
            parent.widthPixels = width()
            parent.heightPixels = height()
        }

        fun update() {
            updatePosition()
            updateDimension()
        }

        fun copy(): ElementAlignments {
            return ElementAlignments(parent, x, y, width, height)
        }

        override fun equals(other: Any?): Boolean {
            return this === other
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            result = 31 * result + width.hashCode()
            result = 31 * result + height.hashCode()
            return result
        }
    }

}

open class GUIElement(parent: RootGUIElement, name: String, xAlignment: Alignment, yAlignment: Alignment, widthAlignment: Alignment, heightAlignment: Alignment, open: Boolean = false, layer: Int = parent.layer + 1) :
        RootGUIElement(name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    constructor(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, open: Boolean = false, layer: Int = parent.layer + 1) :
            this(parent, name, { xPixel }, { yPixel }, { widthPixels }, { heightPixels }, open, layer)

    var parent: RootGUIElement = parent
        set(value) {
            if (field != value) {
                parentWindow = field.parentWindow
                field.children.remove(this)
                if (open)
                    field.parentWindow.openChildren.remove(this)
                val v = field
                field = value
                value.children.add(this)
                onParentChange(v)
            }
        }

    final override var parentWindow = if (parent is GUIWindow) parent else parent.parentWindow
        private set

    init {
        alignments.update()
        parent.children.add(this)
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

    override fun toString() = "${javaClass.simpleName}: $name at $xPixel, $yPixel absolute, ${alignments.x()}, ${alignments.y()} relative, w: $widthPixels, h: $heightPixels"
}

open class GUIWindow(name: String, xAlignment: Alignment, yAlignment: Alignment, widthAlignment: Alignment, heightAlignment: Alignment, windowGroup: WindowGroup, open: Boolean = false, layer: Int = 0) :
        RootGUIElement(name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    constructor(name: String, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, windowGroup: WindowGroup, open: Boolean = false, layer: Int = 0) :
            this(name, { xPixel }, { yPixel }, { widthPixels }, { heightPixels }, windowGroup, open, layer)

    /**
     * Ordered constantly based on layer
     */
    val openChildren = WeakMutableList<GUIElement>().apply {
        onAdd = {
            this.sortBy { it.layer }
        }
    }

    override val parentWindow = this

    var windowGroup: WindowGroup = windowGroup
        set(value) {
            field.windows.remove(this)
            value.windows.add(this)
            field = value
        }

    var topLeftGroup = AutoFormatGUIGroup(this, name + " top left group", { 1 }, { this.heightPixels - 5 }, open = open, xPixelSeparation = 5)
    var topRightGroup = AutoFormatGUIGroup(this, name + " top right group", { this.widthPixels - 5 }, { this.heightPixels - 5 }, open = open, xPixelSeparation = -5)
    var bottomRightGroup = AutoFormatGUIGroup(this, name + " bottom right group", { this.widthPixels - 5 }, { 1 }, open = open, xPixelSeparation = -5)
    var bottomLeftGroup = AutoFormatGUIGroup(this, name + " bottom left group", { 1 }, { 1 }, open = open, xPixelSeparation = 5)

    /* Settings */

    /** If this should not interfere with sending controls to the level when selected */
    var partOfLevel = false
    /** If this should, when opened, move as near to the mouse as possible (but not beyond the screen) */
    var openAtMouse = false

    init {
        windowGroup.windows.add(this)
        ScreenManager.windows.add(this)
        if (open) {
            ScreenManager.openWindows.add(this)
        }
    }

    /* Util */
    /**
     * @param pos 0 - top left, 1 - top right, 2 - bottom right, 3 - bottom left
     */
    fun generateCloseButton(layer: Int = this.layer + 1, pos: Int = 1): GUICloseButton {
        return GUICloseButton(getGroup(pos), name + " close button", { 0 }, { 0 }, open, layer, this)
    }

    /**
     * @param pos 0 - top left, 1 - top right, 2 - bottom right, 3 - bottom left
     */
    fun generateDragGrip(layer: Int = this.layer + 1, pos: Int = 1): GUIDragGrip {
        return GUIDragGrip(getGroup(pos), name + " drag grip", { 0 }, { 0 }, open, layer, this)
    }

    /**
     * @param pos 0 - top left, 1 - top right, 2 - bottom right, 3 - bottom left
     */
    fun generateDimensionDragGrip(layer: Int = this.layer + 1, pos: Int = 1): GUIDimensionDragGrip {
        return GUIDimensionDragGrip(getGroup(pos), name + " dimension drag grip", { 0 }, { 0 }, open, layer, this)
    }

    /**
     * @param pos 0 - top left, 1 - top right, 2 - bottom right, 3 - bottom left
     */
    private fun getGroup(pos: Int): AutoFormatGUIGroup {
        when (pos) {
            0 -> return topLeftGroup
            1 -> return topRightGroup
            2 -> return bottomRightGroup
            3 -> return bottomLeftGroup
        }
        return topRightGroup
    }

    override fun update() {

        fun updateChild(c: RootGUIElement) {
            c.children.forEach {
                it.update()
                updateChild(it)
            }
        }

        updateChild(this)
    }

    override fun toString() = "$name window at $xPixel, $yPixel absolute, ${alignments.x()}, ${alignments.y()} relative"
}