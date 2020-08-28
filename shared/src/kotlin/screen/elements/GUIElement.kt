package screen.elements

import data.WeakMutableList
import graphics.TextureRenderParams
import io.ControlEvent
import main.Game
import main.PowerworksDelegates
import screen.WindowGroup
import screen.mouse.Mouse
import java.util.*

typealias Alignment = () -> Int

sealed class RootGUIElement(name: String, xAlignment: Alignment, yAlignment: Alignment, widthAlignment: Alignment, heightAlignment: Alignment, open: Boolean, var layer: Int) {

    val id = UUID.randomUUID()!!
    var name = name

    var open: Boolean = open
        set(value) {
            if (!value && field) {
                field = false
                mouseOn = false
                if (this is GUIElement) {
                    parentWindow.openChildren.remove(this)
                } else if (this is GUIWindow) {
                    //ScreenManager.openWindows.remove(this)
                }
                onClose()
                children.forEach { if (it.matchParentClosing) it.open = false }
            } else if (value && !field) {
                field = true
                //mouseOn = ScreenManager.isMouseOn(this)
                if (this is GUIElement)
                    parentWindow.openChildren.add(this)
                else if (this is GUIWindow) {
                    //ScreenManager.openWindows.add(this)
                    if (openAtMouse) {
                        val x = Mouse.xPixel
                        val y = Mouse.yPixel - heightPixels
                        alignments.x = { x }
                        alignments.y = { y }
                        keepInScreen()
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
                onAlignmentChange(true, true, true, true)
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
     * This is updated every time a dimension or position of this or a parent changes,
     * or the updateAlignment() function is called
     */
    var widthPixels = alignments.width()
        private set(value) {
            if (field != value) {
                val old = field
                field = value
                if (this is GUIElement) {
                    parent.onChildDimensionChange(this)
                }
                onDimensionChange(old, heightPixels)
                children.forEach {
                    it.alignments.update()
                    it.onParentDimensionChange(old, heightPixels)
                }
            }
        }

    /**
     * This is updated every time a dimension or position of this or a parent changes,
     * or the updateAlignment() function is called
     */
    var heightPixels = alignments.height()
        private set(value) {
            if (field != value) {
                val old = field
                field = value
                if (this is GUIElement) {
                    parent.onChildDimensionChange(this)
                }
                onDimensionChange(widthPixels, old)
                children.forEach {
                    it.alignments.update()
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
    fun getChild(uuid: UUID, checkChildren: Boolean = true): GUIElement? {
        var r = children.firstOrNull { it.id == id }
        if (checkChildren) {
            val i = children.iterator()
            while (r == null && i.hasNext()) {
                r = i.next().getChild(id)
            }
        }
        return r
    }

    fun anyChild(predicate: (GUIElement) -> Boolean): Boolean {

        fun recursivelyFind(predicate: (GUIElement) -> Boolean, e: RootGUIElement): Boolean {
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

    /**
     * True the mouse intersects the rectangle starting at [xPixel], [yPixel] with width [widthPixels] and height [heightPixels]
     */
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

    /** When this gets a new parent, change this's layer to one above the parent */
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
        if (open) {}
            //mouseOn = ScreenManager.isMouseOn(this)
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
    open fun onInteractOn(event: ControlEvent, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
    }

    open fun onInteractOff(event: ControlEvent, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {

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

    /** When one of the ElementAlignments changes */
    open fun onAlignmentChange(x: Boolean = false, y: Boolean = false, width: Boolean = false, height: Boolean = false) {
    }

    fun keepInScreen() {
        var x = xPixel
        if (x + widthPixels > Game.WIDTH)
            x = Game.WIDTH - widthPixels
        else if (x < 0)
            x = 0
        var y = yPixel
        if (y + heightPixels > Game.HEIGHT)
            y = Game.HEIGHT - heightPixels
        else if (y < 0)
            y = 0
        alignments.x = { x }
        alignments.y = { y }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RootGUIElement

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    class ElementAlignments(parent: RootGUIElement, x: Alignment, y: Alignment, width: Alignment, height: Alignment) {

        var parent = parent
            set(value) {
                if (field != value) {
                    field = value
                    update()
                }
            }

        var x = x
            set(value) {
                if (field !== value) {
                    field = value
                    parent.xPixel = value() + ((parent as? GUIElement)?.parent?.xPixel ?: 0)
                    parent.onAlignmentChange(x = true)
                }
            }
        var y = y
            set(value) {
                if (field !== value) {
                    field = value
                    parent.yPixel = value() + ((parent as? GUIElement)?.parent?.yPixel ?: 0)
                    parent.onAlignmentChange(y = true)
                }
            }
        var width = width
            set(value) {
                if (field !== value) {
                    field = value
                    parent.widthPixels = value()
                    parent.onAlignmentChange(width = true)
                }
            }
        var height = height
            set(value) {
                if (field !== value) {
                    field = value
                    parent.heightPixels = value()
                    parent.onAlignmentChange(height = true)
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
                alignments.update()
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

    override fun toString() = "${javaClass.simpleName}: $name at $xPixel, $yPixel absolute, ${alignments.x()}, ${alignments.y()} relative, w: $widthPixels, h: $heightPixels, l: $layer"
}

open class GUIWindow(name: String, xAlignment: Alignment, yAlignment: Alignment, widthAlignment: Alignment, heightAlignment: Alignment, open: Boolean = false, layer: Int = 0) :
        RootGUIElement(name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    constructor(name: String, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, open: Boolean = false, layer: Int = 0) :
            this(name, { xPixel }, { yPixel }, { widthPixels }, { heightPixels }, open, layer)

    /**
     * Ordered constantly based on layer
     */
    val openChildren = WeakMutableList<GUIElement>().apply {
        onAdd = {
            this.sortBy { it.layer }
        }
    }

    override val parentWindow = this

    var windowGroup: WindowGroup by PowerworksDelegates.lateinitVal()

    var topLeftGroup = AutoFormatGUIGroup(this, name + " top left group", { 1 }, { this.heightPixels - 6 }, open = open, padding = 1, dir = 1)
    var topRightGroup = AutoFormatGUIGroup(this, name + " top right group", { this.widthPixels - 1 }, { this.heightPixels - 6 }, open = open, padding = 1, dir = 3)
    var bottomRightGroup = AutoFormatGUIGroup(this, name + " bottom right group", { this.widthPixels - 1 }, { 1 }, open = open, padding = 1, dir = 3)
    var bottomLeftGroup = AutoFormatGUIGroup(this, name + " bottom left group", { 1 }, { 1 }, open = open, padding = 1, dir = 1)

    /* Settings */

    /** If this should, when opened, move as near to the mouse as possible (but not beyond the screen) */
    var openAtMouse = false

    var allowEscapeToClose = false

    var clickOffToClose = false

    /* Util */
    /**
     * @param pos 0 - top left, 1 - top right, 2 - bottom right, 3 - bottom left
     * Automatically turns on [allowEscapeToClose] too
     */
    fun generateCloseButton(layer: Int = this.layer + 2, pos: Int = 1): GUICloseButton {
        allowEscapeToClose = true
        return GUICloseButton(getGroup(pos), name + " close button", { 0 }, { 0 }, open, layer, this)
    }

    /**
     * @param pos 0 - top left, 1 - top right, 2 - bottom right, 3 - bottom left
     */
    fun generateDragGrip(layer: Int = this.layer + 2, pos: Int = 1): GUIDragGrip {
        return GUIDragGrip(getGroup(pos), name + " drag grip", { 0 }, { 0 }, open, layer, this)
    }

    /**
     * @param pos 0 - top left, 1 - top right, 2 - bottom right, 3 - bottom left
     */
    fun generateDimensionDragGrip(layer: Int = this.layer + 2, pos: Int = 1): GUIDimensionDragGrip {
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
    }

    override fun toString() = "$name window at $xPixel, $yPixel absolute, ${alignments.x()}, ${alignments.y()} relative, w: ${widthPixels}, h: ${heightPixels}, l: $layer"
}