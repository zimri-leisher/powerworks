package screen.gui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import crafting.Recipe
import fluid.FluidTank
import graphics.*
import graphics.text.TaggedText
import graphics.text.TextManager
import graphics.text.TextRenderParams
import io.ControlBind
import item.Inventory
import level.LevelInfo
import level.LevelObject
import main.PowerworksDelegates
import main.height
import main.toColor
import main.width
import misc.Geometry
import resource.ResourceContainer
import resource.ResourceList
import resource.ResourceType
import screen.Interaction
import screen.ScreenLayer
import screen.attribute.*
import screen.element.*
import java.util.*

private var nextId = 0

/**
 * A GUI window on the given [layer]. Use [block] as context for defining the layout of this gui, or use [define] anywhere
 * later. Guis are automatically stored and rendered on the appropriate layer by the [ScreenManager].
 */
open class Gui(val layer: ScreenLayer, block: GuiElement.Context.() -> Unit = {}) {

    /**
     * The parent [GuiElement] of this GUI. The [Gui] itself has almost no state of its own, except for the [layout]. Everything
     * else, [open], [dimensions], [placement], and all of its children, are stored in this element.
     */
    val parentElement = object : GuiElement(null) {
        override val absoluteX get() = x
        override val absoluteY get() = y
        override val gui: Gui
            get() = this@Gui

        override val isRoot get() = true

        init {
            placement = Placement.Origin
            dimensions = Dimensions.FitChildren
        }
    }

    private val internalContext = GuiElement.Context(parentElement)

    /**
     * The current [TextureRenderParams] of this GUI. Changing this changes the [TextureRenderParams] for all children.
     */
    var renderParams: TextureRenderParams? = null

    /**
     * The layout of this GUI. This stores all the exact [Placement]s and [Dimensions] of its children elements.
     */
    val layout = Layout(this)

    /**
     * The exact [Placement] of the parent element of this GUI
     */
    val placement get() = layout.getExactPlacement(parentElement)

    /**
     * The exact [Dimensions] of the parent element of this GUI
     */
    val dimensions get() = layout.getExactDimensions(parentElement)

    /**
     * Whether or not the parent element is open. Changing this just changes it for the parent element.
     */
    var open
        get() = parentElement.open
        set(value) {
            parentElement.open = value
        }

    init {
        open = false
        block(internalContext)
        layout.set()
        layer.guis.add(this)
    }

    /**
     * Lets you use [GuiElement.Context] to define the layout of this GUI. Used in alternative to passing in a block
     * in the constructor, when you dont know it at construction time.
     */
    protected fun define(block: GuiElement.Context.() -> Unit) {
        block(internalContext)
        layout.set()
    }

    /**
     * Renders this GUI (if the parent element is open) and all its children. Uses the current [renderParams].
     */
    open fun render() {
        if (parentElement.open) {
            parentElement.render(renderParams)
        }
    }

    /**
     * Updates this GUI and all its children.
     */
    open fun update() {
        parentElement.update()
    }

    /**
     * A class for calculating and storing the exact [Placement]s and [Dimensions] of [GuiElement]s.
     */
    class Layout(private val gui: Gui) {
        private val dimensions = mutableMapOf<GuiElement, Dimensions.Exact>()
        private val placements = mutableMapOf<GuiElement, Placement.Exact>()

        /**
         * Recalculates the cached exact dimensions of the given element, stores them, and returns them.
         */
        fun recalculateExactDimensions(element: GuiElement): Dimensions.Exact {
            val dimension = element.dimensions.get(element, gui)
            dimensions[element] =
                if (dimension is Dimensions.Unknown) Dimensions.Exact(dimension.width, dimension.height) else dimension
            element.onChangeDimensions()
            return dimension
        }

        /**
         * Recalculates the cached exact placement of the given element, stores them, and returns them.
         */
        fun recalculateExactPlacement(element: GuiElement): Placement.Exact {
            val placement = element.placement.get(element, gui)
            placements[element] =
                if (placement is Placement.Unknown) Placement.Exact(placement.x, placement.y) else placement
            element.onChangePlacement()
            return placement
        }

        /**
         * Gets the current cached exact dimensions of the given [element].
         */
        fun getExactDimensions(element: GuiElement): Dimensions.Exact {
            return dimensions[element] ?: return Dimensions.Unknown(0, 0)
        }

        /**
         * Gets the current cached exact placement of the given [element].
        .         */
        fun getExactPlacement(element: GuiElement): Placement.Exact {
            return placements[element] ?: return Placement.Unknown(0, 0)
        }

        /**
         * Recalculates the layout caches from scratch based on the dimensions and placements of this gui's children. Must
         * call this before rendering. This is called automatically at initialize and after calling [defineIn]
         */
        fun set() {
            dimensions.clear()
            placements.clear()

            fun recursivelySetDimensions(element: GuiElement) {
                element.children.forEach { recursivelySetDimensions(it) }
                val dimension = element.dimensions.get(element, gui)
                dimensions[element] = if (dimension is Dimensions.Unknown) Dimensions.Exact(
                    dimension.width,
                    dimension.height
                ) else dimension
                element.onChangeDimensions()
            }

            fun recursivelySetPlacement(element: GuiElement) {
                val placement = element.placement.get(element, gui)
                placements[element] =
                    if (placement is Placement.Unknown) Placement.Exact(placement.x, placement.y) else placement
                element.onChangePlacement()
                element.children.forEach { recursivelySetPlacement(it) }
            }

            fun getAllChildren(element: GuiElement): List<GuiElement> {
                return element.children + element.children.flatMap { getAllChildren(it) }
            }

            var calculationAttempts = 0
            val allElements = getAllChildren(gui.parentElement) + gui.parentElement
            while (allElements.any {
                    getExactDimensions(it) is Dimensions.Unknown ||
                            getExactPlacement(it) is Placement.Unknown
                } && calculationAttempts < 10) {
                recursivelySetDimensions(gui.parentElement)
                recursivelySetPlacement(gui.parentElement)
                calculationAttempts++
            }
        }
    }
}

open class GroupElement(parent: GuiElement) : GuiElement(parent)

class MutableGroupElement(parent: GuiElement) : GroupElement(parent) {

    private val mutableChildren get() = children as MutableList<GuiElement>

    fun add(element: GuiElement, index: Int = mutableChildren.size) {
        mutableChildren.add(index, element)
        gui.layout.set()
    }

    fun remove(element: GuiElement) {
        mutableChildren.remove(element)
        gui.layout.set()
    }
}

/**
 * A class for defining parts of GUIs. GuiElements may have children, parents, a [Placement], [Dimensions] and [eventListeners].
 * They can be opened and closed, rendered and update.
 * Don't manually instantiate them to create GUIs. Instead, use the builder functions in [GuiElement.Context] to idiomatically
 * create GUIs. You can obtain closure inside of a [GuiElement.Context] by using [Gui.define].
 *
 * Subclasses should make sure to call super methods in any overriden functions, as super methods push events out to [eventListeners]
 */
abstract class GuiElement(parent: GuiElement?) {

    /**
     * The unique ID given to each instance of every GuiElement.
     */
    val id = nextId++

    private var mutableParent: GuiElement? = parent

    /**
     * The parent element of this GuiElement. If this is a root element (i.e. if this is the parent element of a [Gui]), this
     * method throws a null pointer exception. Use [isRoot] to check if you are unsure.
     */
    val parent: GuiElement
        get() = mutableParent!!

    /**
     * @return true if this is the parent element of a [Gui].
     */
    open val isRoot get() = false

    /**
     * The [Gui] this element is in.
     */
    open val gui: Gui get() = parent.gui

    private val mutableChildren = mutableListOf<GuiElement>()

    /**
     * The children of this element. Rendering and updating the children are handled by this class and not [ScreenManager].
     * Thus, in order for children to be rendered and updated properly, make sure to call `super` when overriding those
     * functions.
     */
    val children: List<GuiElement> get() = mutableChildren

    /**
     * Whether or not this element should render its children and be possible to interact with. Closed elements are still
     * updated.
     */
    var open = true
        set(value) {
            if (field != value) {
                if (value) {
                    onOpen()
                } else {
                    onClose()
                }
                field = value
            }
        }

    /**
     * Whether or not the mouse is on this element. Updating of this is handled by the [ScreenManager], it should not be set
     * manually.
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

    /**
     * Where this element is placed relative to its parent. See [Placement] for more details.
     */
    var placement: Placement = Placement.Origin

    /**
     * The dimensions (width/height) of this element. See [Dimensions] for more details.
     */
    var dimensions: Dimensions = Dimensions.None

    /**
     * The exact width of this element as determined by the cached values in the parent [Gui]'s [Gui.Layout].
     */
    val width get() = gui.layout.getExactDimensions(this).width

    /**
     * The exact height of this element as determined by the cached values in the parent [Gui]'s [Gui.Layout].
     */
    val height get() = gui.layout.getExactDimensions(this).height

    /**
     * The exact x of this element relative to its parent as determined by the cached values in the parent [Gui]'s [Gui.Layout].
     */
    open val x: Int
        get() = gui.layout.getExactPlacement(this).x

    /**
     * The x of this element relative to the origin of the screen.
     */
    open val absoluteX: Int get() = parent.absoluteX + x

    /**
     * The exact y of this element relative to its parent as determined by the cached values in the parent [Gui]'s [Gui.Layout].
     */
    open val y: Int
        get() = gui.layout.getExactPlacement(this).y

    /**
     * The y of this element relative to the origin of the screen.
     */
    open val absoluteY: Int get() = parent.absoluteY + y

    /**
     * A list of [GuiEventListener]s to send events to. [GuiEventListener]s are simple classes that hold a handler function
     * for events they want to receive. See [GuiEvent] for a list of types.
     */
    val eventListeners = mutableListOf<GuiEventListener>()

    /**
     * A list of [Attribute]s this [GuiElement] has. [Attribute]s are essentially added features to a [GuiElement]. They
     * work by adding event listeners and hooking into events of this element.
     */
    val attributes = mutableListOf<Attribute>()

    /**
     * Renders this element to the screen at the [absoluteX], [absoluteY] with dimensions [width] and [height] and with
     * [params], and then renders all of its open children elements. If overriden, make sure to call super so that the render
     * event gets pushed out.
     */
    open fun render(params: TextureRenderParams?) {
        eventListeners.filterIsInstance<GuiRenderListener>().forEach { it.handle(this, absoluteX, absoluteY, params) }
        children.forEach { if (it.open) it.render(params) }
    }

    /**
     * Updates this element, performing miscellaneous things to keep the state of this up-to-date. This method also updates
     * all children. If overriden, make sure to call to super so that the update event gets pushed out.
     */
    open fun update() {
        eventListeners.filterIsInstance<GuiUpdateListener>().forEach { it.handle(this) }
        children.forEach { it.update() }
    }

    /**
     * @return a sequence of all children of this element that intersect the given point.
     */
    fun getChildrenAt(x: Int, y: Int) = children.asSequence().filter {
        val placement = gui.layout.getExactPlacement(it)
        val dimensions = gui.layout.getExactDimensions(it)
        Geometry.intersects(placement.x, placement.y, dimensions.width, dimensions.height, x, y, 1, 1)
    }

    open fun onOpen() {
        eventListeners.filterIsInstance<GuiOpenListener>().forEach { it.handle(this) }
    }

    open fun onClose() {
        eventListeners.filterIsInstance<GuiCloseListener>().forEach { it.handle(this) }
    }

    open fun onMouseEnter() {
        eventListeners.filterIsInstance<GuiMouseEnterListener>().forEach { it.handle(this) }
    }

    open fun onMouseLeave() {
        eventListeners.filterIsInstance<GuiMouseLeaveListener>().forEach { it.handle(this) }
    }

    open fun onInteractOn(interaction: Interaction) {
        eventListeners.filterIsInstance<GuiInteractOnListener>().forEach { it.handle(this, interaction) }
    }

    open fun onDeselect() {
        eventListeners.filterIsInstance<GuiDeselectListener>().forEach { it.handle(this) }
    }

    open fun onChangeDimensions() {
        eventListeners.filterIsInstance<GuiChangeDimensionListener>().forEach { it.handle(this) }
    }

    open fun onChangePlacement() {
        eventListeners.filterIsInstance<GuiChangePlacementListener>().forEach { it.handle(this) }
    }

    open class Context(val inElement: GuiElement) {

        private var placementLocked = false
        var currentDefaultPlacement: Placement = Placement.Origin
            set(value) {
                if (!placementLocked) {
                    field = value
                }
            }

        var dimensions
            get() = inElement.dimensions
            set(value) {
                inElement.dimensions = value
            }
        var placement
            get() = inElement.placement
            set(value) {
                if (!placementLocked) {
                    inElement.placement = value
                }
            }
        var open
            get() = inElement.open
            set(value) {
                inElement.open = value
            }

        private fun addChild(child: GuiElement, block: Context.() -> Unit, placement: Placement) {
            child.placement = placement
            inElement.mutableChildren.add(child)
            block(Context(child))
        }

        fun onInteractOn(block: GuiElement.(interaction: Interaction) -> Unit) {
            inElement.eventListeners.add(GuiInteractOnListener(block))
        }

        fun onDeselect(block: GuiElement.() -> Unit) {
            inElement.eventListeners.add(GuiDeselectListener(block))
        }

        fun onMouseEnter(block: GuiElement.() -> Unit) {
            inElement.eventListeners.add(GuiMouseEnterListener(block))
        }

        fun onMouseLeave(block: GuiElement.() -> Unit) {
            inElement.eventListeners.add(GuiMouseLeaveListener(block))
        }

        fun onOpen(block: GuiElement.() -> Unit) {
            inElement.eventListeners.add(GuiOpenListener(block))
        }

        fun onClose(block: GuiElement.() -> Unit) {
            inElement.eventListeners.add(GuiCloseListener(block))
        }

        fun onUpdate(block: GuiElement.() -> Unit) {
            inElement.eventListeners.add(GuiUpdateListener(block))
        }

        fun onRender(block: GuiElement.(x: Int, y: Int, TextureRenderParams?) -> Unit) {
            inElement.eventListeners.add(GuiRenderListener(block))
        }

        private fun addAttribute(attribute: Attribute) {
            inElement.attributes.add(attribute)
        }

        fun openWithBrainInventory() = AttributeOpenWithBrainInventory(inElement).apply { addAttribute(this) }

        fun makeDraggable() = AttributeDraggable(inElement).apply { addAttribute(this) }

        fun makeResizable() = AttributeResizable(inElement).apply { addAttribute(this) }

        fun openAtMouse() = AttributeOpenAtMouse(inElement).apply { addAttribute(this) }

        fun keepInsideScreen() = AttributeKeepInScreen(inElement).apply { addAttribute(this) }

        fun linkToContainer(container: ResourceContainer) =
            AttributeResourceContainerLink(inElement, container).apply { addAttribute(this) }

        fun openAtCenter(order: Int = 0) = AttributeOpenAtCenter(inElement, order = order).apply { addAttribute(this) }

        fun at(placement: Placement, lock: Boolean = false, block: Context.() -> Unit) {
            val oldPlacement = currentDefaultPlacement
            currentDefaultPlacement = placement
            if (lock) {
                placementLocked = true
            }
            block()
            if (lock) {
                placementLocked = false
            }
            currentDefaultPlacement = oldPlacement
        }

        fun align(
            horizontalAlignment: HorizontalAlign = HorizontalAlign.CENTER,
            verticalAlignment: VerticalAlign = VerticalAlign.CENTER,
            lock: Boolean = false,
            block: Context.() -> Unit
        ) =
            at(Placement.Align(horizontalAlignment, verticalAlignment), lock, block)

        fun clip(
            placement: Placement = currentDefaultPlacement,
            dimensions: Dimensions = Dimensions.MatchParent,
            block: Context.() -> Unit = {}
        ) = ElementClip(inElement).apply {
            this.dimensions = dimensions
            addChild(this, block, placement)
        }

        fun background(params: TextureRenderParams? = null, block: Context.() -> Unit = {}) =
            ElementDefaultRectangle(inElement, params).apply {
                dimensions = Dimensions.FitChildren
                addChild(this, block, currentDefaultPlacement)
            }

        fun animation(
            animation: Animation,
            placement: Placement = currentDefaultPlacement,
            width: Int = animation.width,
            height: Int = animation.height,
            keepAspect: Boolean = false,
            params: TextureRenderParams? = null,
            block: Context.() -> Unit = {}
        ) =
            renderable(animation, placement, width, height, keepAspect, params, block)

        fun texture(
            texture: TextureRegion,
            placement: Placement = currentDefaultPlacement,
            width: Int = texture.width,
            height: Int = texture.height,
            keepAspect: Boolean = false,
            params: TextureRenderParams? = null,
            block: Context.() -> Unit = {}
        ) =
            renderable(Texture(texture), placement, width, height, keepAspect, params, block)

        fun texture(
            texture: Texture,
            placement: Placement = currentDefaultPlacement,
            width: Int = texture.width,
            height: Int = texture.height,
            keepAspect: Boolean = false,
            params: TextureRenderParams? = null,
            block: Context.() -> Unit = {}
        ) =
            renderable(texture, placement, width, height, keepAspect, params, block)

        fun renderable(
            renderable: Renderable,
            placement: Placement = currentDefaultPlacement,
            width: Int = renderable.width,
            height: Int = renderable.height,
            keepAspect: Boolean = false,
            params: TextureRenderParams? = null,
            block: Context.() -> Unit = {}
        ) =
            ElementRenderable(inElement, renderable, keepAspect, params).apply {
                dimensions = Dimensions.Exact(width, height)
                addChild(this, block, placement)
            }

        fun mutableGroup(placement: Placement = currentDefaultPlacement, block: Context.() -> Unit = {}) =
            MutableGroupElement(inElement).apply {
                dimensions = Dimensions.FitChildren
                addChild(this, block, placement)
            }

        fun group(placement: Placement = currentDefaultPlacement, block: Context.() -> Unit = {}) =
            GroupElement(inElement).apply {
                dimensions = Dimensions.FitChildren
                addChild(this, block, placement)
            }

        fun mutableList(
            placement: Placement = currentDefaultPlacement,
            horizontalAlign: HorizontalAlign = HorizontalAlign.CENTER,
            padding: Int = 0,
            block: Context.() -> Unit = {}
        ) =
            mutableGroup(placement) {
                dimensions = Dimensions.VerticalList(padding)
                at(Placement.VerticalList(padding, horizontalAlign), true) {
                    block()
                }
            }

        fun list(
            placement: Placement = currentDefaultPlacement,
            horizontalAlign: HorizontalAlign = HorizontalAlign.CENTER,
            padding: Int = 0,
            block: Context.() -> Unit = {}
        ) =
            group(placement) {
                dimensions = Dimensions.VerticalList(padding)
                at(Placement.VerticalList(padding, horizontalAlign), true) {
                    block()
                }
            }

        fun horizontalList(
            placement: Placement = currentDefaultPlacement,
            verticalAlign: VerticalAlign = VerticalAlign.CENTER,
            padding: Int = 0,
            block: Context.() -> Unit = {}
        ) =
            group(placement) {
                dimensions = Dimensions.HorizontalList(padding)
                at(Placement.HorizontalList(padding, verticalAlign), true) {
                    block()
                }
            }

        fun text(
            text: String,
            placement: Placement = currentDefaultPlacement,
            allowTags: Boolean = false,
            ignoreLines: Boolean = false,
            params: TextRenderParams = TextRenderParams(),
            block: Context.() -> Unit = {}
        ) =
            ElementText(inElement, text, allowTags, ignoreLines, params).apply {
                addChild(this, block, placement)
            }

        fun button(
            placement: Placement = currentDefaultPlacement,
            onRelease: GuiElement.(Interaction) -> Unit = {},
            onPress: GuiElement.(Interaction) -> Unit = {},
            toggle: Boolean = false,
            renderDefaultButton: Boolean = false,
            block: Context.() -> Unit = {}
        ) =
            ElementButton(inElement, renderDefaultButton, toggle, onPress, onRelease).apply {
                dimensions = Dimensions.FitChildren
                addChild(this, block, placement)
            }

        fun button(
            text: String,
            onRelease: GuiElement.(Interaction) -> Unit = {},
            onPress: GuiElement.(Interaction) -> Unit = {},
            toggle: Boolean = false,
            padding: Int = 0,
            allowTags: Boolean = false,
            ignoreLines: Boolean = false,
            params: TextRenderParams = TextRenderParams(),
            placement: Placement = currentDefaultPlacement,
            block: Context.() -> Unit = {}
        ) =
            button(placement, onRelease, onPress, toggle, true, {
                dimensions = dimensions.pad(padding, padding)
                text(text, Placement.Align.Center, allowTags, ignoreLines, params)
                block()
            })

        fun levelView(
            placement: Placement = currentDefaultPlacement,
            camera: LevelObject,
            block: Context.() -> Unit = {}
        ) =
            ElementLevelView(inElement, camera).apply {
                addChild(this, block, placement)
            }

        fun resourceContainerView(
            container: ResourceContainer,
            width: Int,
            height: Int,
            placement: Placement = currentDefaultPlacement,
            allowSelection: Boolean = false,
            allowModification: Boolean = false,
            onSelect: (type: ResourceType, quantity: Int, interaction: Interaction) -> Unit = { _, _, _ -> },
            block: Context.() -> Unit = {}
        ) =
            ElementResourceContainer(
                inElement,
                width,
                height,
                container,
                allowSelection,
                allowModification,
                onSelect
            ).apply {
                addChild(this, block, placement)
            }

        fun closeButton(placement: Placement = currentDefaultPlacement, block: Context.() -> Unit = {}) =
            ElementCloseButton(inElement).apply {
                addChild(this, block, placement)
            }

        fun progressBar(
            maxProgress: Int,
            getProgress: () -> Int = { 0 },
            dimensions: Dimensions = Dimensions.Exact(48, 6),
            placement: Placement = currentDefaultPlacement,
            block: Context.() -> Unit = {}
        ) =
            ElementProgressBar(inElement, maxProgress, getProgress).apply {
                this.dimensions = dimensions
                addChild(this, block, placement)
            }

        fun inventory(
            inventory: Inventory,
            placement: Placement = currentDefaultPlacement,
            block: Context.() -> Unit = {}
        ) =
            ElementResourceContainer(inElement, inventory.width, inventory.height, inventory, true, true).apply {
                addChild(this, block, placement)
            }

        fun fluidTank(
            fluidTank: FluidTank,
            dimensions: Dimensions = Dimensions.Exact(60, 40),
            placement: Placement = currentDefaultPlacement,
            block: Context.() -> Unit = {}
        ) =
            ElementFluidTank(inElement, fluidTank).apply {
                this.dimensions = dimensions
                addChild(this, block, placement)
            }

        fun progressArrow(
            from: GuiElement,
            fromDir: Int,
            to: GuiElement,
            toDir: Int,
            maxProgress: Int,
            getProgress: () -> Int = { 0 },
            backgroundColor: Color = toColor(0xFFFFFF),
            progressColor: Color = toColor(0x00BC06),
            block: Context.() -> Unit = {}
        ) =
            ElementProgressArrow(
                inElement,
                from,
                to,
                maxProgress,
                getProgress,
                fromDir,
                toDir,
                backgroundColor,
                progressColor
            ).apply {
                addChild(this, block, Placement.Origin)
            }

        fun resourceList(
            list: ResourceList,
            width: Int = list.size,
            height: Int = 1,
            displayQuantity: Boolean = true,
            placement: Placement = currentDefaultPlacement,
            block: Context.() -> Unit = {}
        ) =
            ElementResourceList(inElement, list, width, height, displayQuantity).apply {
                addChild(this, block, placement)
            }

        fun recipeListDisplay(
            recipes: List<Recipe>,
            width: Int = recipes.size,
            height: Int = 1,
            onSelectRecipe: (Recipe) -> Unit = {},
            placement: Placement = currentDefaultPlacement,
            block: Context.() -> Unit = {}
        ) =
            ElementRecipeList(inElement, recipes, width, height, onSelectRecipe).apply {
                addChild(this, block, placement)
            }

        fun tabs(padding: Int = 0, placement: Placement = currentDefaultPlacement, block: Context.Tab.() -> Unit = {}) =
            ElementTabs(inElement, padding).apply {
                this.placement = placement
                inElement.mutableChildren.add(this)
                block(Context.Tab(this))
            }

        fun recipeSelectButton(
            recipe: Recipe? = null,
            allowedRecipes: (Recipe) -> Boolean = { true },
            onRecipeChange: (Recipe?) -> Unit = {},
            placement: Placement = currentDefaultPlacement,
            block: Context.() -> Unit = {}
        ) =
            ElementRecipeButton(inElement, recipe, onRecipeChange, allowedRecipes).apply {
                dimensions = Dimensions.Exact(16, 16)
                addChild(this, block, placement)
            }

        fun levelSelector(
            levels: Map<UUID, LevelInfo>,
            onSelectLevel: (UUID, LevelInfo) -> Unit = { _, _ -> },
            placement: Placement = currentDefaultPlacement,
            dimensions: Dimensions = Dimensions.Exact(100, 100),
            block: Context.() -> Unit = {}
        ) =
            ElementLevelSelector(inElement, levels, onSelectLevel).apply {
                this.dimensions = dimensions
                addChild(this, block, placement)
            }

        fun controlBind(
            bind: ControlBind,
            placement: Placement = currentDefaultPlacement,
            block: Context.() -> Unit = {}
        ) =
            ElementControlBind(inElement, bind).apply {
                addChild(this, block, placement)
            }

        class Tab(inElement: ElementTabs) : Context(inElement) {
            fun tab(text: String, onClick: (index: Int) -> Unit): ElementTabs.Tab {
                val taggedText = TextManager.parseTags(text)
                val taggedTextBounds = TextManager.getStringBounds(taggedText)
                val tab = ElementTabs.Tab(taggedText, taggedTextBounds.width, taggedTextBounds.height, onClick)
                val elementTabs = inElement as ElementTabs
                elementTabs.tabs = elementTabs.tabs + tab
                return tab
            }
        }
    }
}

class ElementRenderable(
    parent: GuiElement,
    var renderable: Renderable,
    var keepAspect: Boolean = false,
    var params: TextureRenderParams? = null
) : GuiElement(parent) {
    override fun render(params: TextureRenderParams?) {
        if (params == null && this.params == null) {
            renderable.render(absoluteX, absoluteY, width, height, keepAspect)
        } else if (params == null) {
            renderable.render(absoluteX, absoluteY, width, height, keepAspect, this.params!!)
        } else if (this.params == null) {
            renderable.render(absoluteX, absoluteY, width, height, keepAspect, params)
        } else {
            renderable.render(absoluteX, absoluteY, width, height, keepAspect, this.params!!.combine(params))
        }
        super.render(params)
    }
}

class ElementDefaultRectangle(parent: GuiElement, var params: TextureRenderParams? = null) : GuiElement(parent) {
    override fun render(params: TextureRenderParams?) {
        if (params == null && this.params == null) {
            Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height)
        } else if (params == null) {
            Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height, this.params!!)
        } else if (this.params == null) {
            Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height, params)
        } else {
            Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height, this.params!!.combine(params))
        }
        super.render(params)
    }
}

class ElementText(
    parent: GuiElement,
    text: String,
    val allowTags: Boolean = false,
    var ignoreLines: Boolean,
    var params: TextRenderParams = TextRenderParams()
) : GuiElement(parent) {

    private var taggedText: TaggedText by PowerworksDelegates.lateinitVal()

    var text = text
        set(value) {
            field = value
            if (allowTags) {
                taggedText = TextManager.parseTags(value)
            }
            dimensions = Dimensions.Text(value, allowTags, ignoreLines, params)
            gui.layout.recalculateExactDimensions(this)
        }

    init {
        dimensions = Dimensions.Text(text, allowTags, ignoreLines, params)
    }

    override fun render(params: TextureRenderParams?) {
        if (allowTags) {
            Renderer.renderTaggedText(taggedText, absoluteX, absoluteY, this.params)
        } else {
            Renderer.renderText(text, absoluteX, absoluteY, this.params, ignoreLines)
        }
        super.render(params)
    }
}