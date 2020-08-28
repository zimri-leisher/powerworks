package screen.gui2

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import fluid.FluidTank
import graphics.*
import graphics.text.TaggedText
import graphics.text.TextManager
import graphics.text.TextRenderParams
import io.ControlEventType
import item.Inventory
import level.LevelObject
import main.PowerworksDelegates
import main.heightPixels
import main.toColor
import main.widthPixels
import misc.Geometry
import resource.ResourceContainer
import resource.ResourceType

private var nextId = 0

open class Gui(val layer: ScreenLayer, block: GuiElement.Context.() -> Unit = {}) {

    val parentElement = object : GuiElement(null) {
        override val absoluteXPixel get() = xPixel
        override val absoluteYPixel get() = yPixel
        override val gui: Gui
            get() = this@Gui

        init {
            placement = Placement.Origin
            dimensions = Dimensions.FitChildren
        }
    }

    private val internalContext = GuiElement.Context(parentElement)

    var renderParams: TextureRenderParams? = null

    val layout = Layout(this)

    val placement get() = layout.getExactPlacement(parentElement)
    val dimensions get() = layout.getExactDimensions(parentElement)

    var open
        get() = parentElement.open
        set(value) {
            parentElement.open = value
        }

    init {
        block(internalContext)
        layout.set()
        layer.guis.add(this)
    }

    fun define(block: GuiElement.Context.() -> Unit) {
        block(internalContext)
        layout.set()
    }

    open fun render() {
        if (parentElement.open) {
            parentElement.render(renderParams)
        }
    }

    open fun update() {
        parentElement.update()
    }

    fun getChildrenAt(xPixel: Int, yPixel: Int) = parentElement.getChildrenAt(xPixel - placement.xPixel, yPixel - placement.yPixel)

    class Layout(private val gui: Gui) {
        private val dimensions = mutableMapOf<GuiElement, Dimensions.Exact>()
        private val placements = mutableMapOf<GuiElement, Placement.Exact>()

        fun recalculateExactDimensions(element: GuiElement): Dimensions.Exact {
            val dimension = element.dimensions.get(element, gui)
            val oldDimensions = getExactDimensions(element)
            dimensions[element] = if (dimension is Dimensions.Unknown) Dimensions.Exact(dimension.widthPixels, dimension.heightPixels) else dimension
            element.onDimensionChange(oldDimensions)
            return dimension
        }

        fun recalculateExactPlacement(element: GuiElement): Placement.Exact {
            val placement = element.placement.get(element, gui)
            val oldPlacement = getExactPlacement(element)
            placements[element] = if (placement is Placement.Unknown) Placement.Exact(placement.xPixel, placement.yPixel) else placement
            element.onPlacementChange(oldPlacement)
            return placement
        }

        fun getExactDimensions(element: GuiElement): Dimensions.Exact {
            return dimensions[element] ?: return Dimensions.Unknown(0, 0)
        }

        fun getExactPlacement(element: GuiElement): Placement.Exact {
            return placements[element] ?: return Placement.Unknown(0, 0)
        }

        fun set() {
            fun recursivelySetDimensions(element: GuiElement) {
                element.children.forEach { recursivelySetDimensions(it) }
                val dimension = element.dimensions.get(element, gui)
                val oldDimensions = getExactDimensions(element)
                dimensions[element] = if (dimension is Dimensions.Unknown) Dimensions.Exact(dimension.widthPixels, dimension.heightPixels) else dimension
                element.onDimensionChange(oldDimensions)
            }

            fun recursivelySetPlacement(element: GuiElement) {
                val placement = element.placement.get(element, gui)
                val oldPlacement = getExactPlacement(element)
                placements[element] = if (placement is Placement.Unknown) Placement.Exact(placement.xPixel, placement.yPixel) else placement
                element.onPlacementChange(oldPlacement)
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

            println("for $gui")
            fun checkHasDefined(element: GuiElement) {
                println("$element dimensions known: ${getExactDimensions(element) !is Dimensions.Unknown}")
                println("$element placement known: ${getExactPlacement(element) !is Placement.Unknown}")
                element.children.forEach {
                    checkHasDefined(it)
                }
            }

            checkHasDefined(gui.parentElement)
        }
    }
}

abstract class GuiElement(parent: GuiElement?) {

    val id = nextId++

    private var mutableParent: GuiElement? = parent
    val parent: GuiElement
        get() = mutableParent!!
    val isRoot get() = mutableParent == null

    open val gui: Gui get() = parent.gui

    private val mutableChildren = mutableListOf<GuiElement>()
    val children: List<GuiElement> get() = mutableChildren

    var open = true
        set(value) {
            if (field != value) {
                if (value) {
                    onOpen()
                    onOpen?.invoke(this)
                } else {
                    onClose()
                    onClose?.invoke(this)
                }
                field = value
            }
        }

    var mouseOn: Boolean = false
        set(value) {
            if (value && !field) {
                onMouseEnter()
                onMouseEnter?.invoke(this)
                field = value
            } else if (!value && field) {
                onMouseLeave()
                onMouseLeave?.invoke(this)
                field = value
            }
        }

    var placement: Placement = Placement.Origin

    var dimensions: Dimensions = Dimensions.None

    val widthPixels get() = gui.layout.getExactDimensions(this).widthPixels

    val heightPixels get() = gui.layout.getExactDimensions(this).heightPixels

    open val xPixel: Int
        get() = gui.layout.getExactPlacement(this).xPixel
    open val absoluteXPixel: Int get() = parent.absoluteXPixel + xPixel

    open val yPixel: Int
        get() = gui.layout.getExactPlacement(this).yPixel
    open val absoluteYPixel: Int get() = parent.absoluteYPixel + yPixel

    var onOpen: (GuiElement.() -> Unit)? = null
    var onClose: (GuiElement.() -> Unit)? = null
    var onMouseEnter: (GuiElement.() -> Unit)? = null
    var onMouseLeave: (GuiElement.() -> Unit)? = null
    var onInteractOn: (GuiElement.(interaction: Interaction) -> Unit)? = null
    var onDeselect: (GuiElement.() -> Unit)? = null

    open fun render(params: TextureRenderParams?) {
        children.forEach { if (it.open) it.render(params) }
    }

    open fun update() {
        children.forEach { it.update() }
    }

    fun getChildrenAt(xPixel: Int, yPixel: Int) = children.asSequence().filter {
        val placement = gui.layout.getExactPlacement(it)
        val dimensions = gui.layout.getExactDimensions(it)
        Geometry.intersects(placement.xPixel, placement.yPixel, dimensions.widthPixels, dimensions.heightPixels, xPixel, yPixel, 1, 1)
    }

    open fun onOpen() {}
    open fun onClose() {}
    open fun onMouseEnter() {}
    open fun onMouseLeave() {}
    open fun onInteractOn(interaction: Interaction) {}
    open fun onDeselect() {}
    open fun onDimensionChange(oldDimensions: Dimensions.Exact) {}
    open fun onPlacementChange(oldPlacement: Placement.Exact) {}


    class Context(val inElement: GuiElement) {

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
            inElement.onInteractOn = block
        }

        fun onDeselect(block: GuiElement.() -> Unit) {
            inElement.onDeselect = block
        }

        fun onMouseEnter(block: GuiElement.() -> Unit) {
            inElement.onMouseEnter = block
        }

        fun onMouseLeave(block: GuiElement.() -> Unit) {
            inElement.onMouseLeave = block
        }

        fun onOpen(block: GuiElement.() -> Unit) {
            inElement.onOpen = block
        }

        fun onClose(block: GuiElement.() -> Unit) {
            inElement.onClose = block
        }

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

        fun align(horizontalAlignment: HorizontalAlign = HorizontalAlign.CENTER, verticalAlignment: VerticalAlign = VerticalAlign.CENTER, lock: Boolean = false, block: Context.() -> Unit) =
                at(Placement.Align(horizontalAlignment, verticalAlignment), lock, block)

        fun background(block: Context.() -> Unit = {}) =
                ElementDefaultRectangle(inElement).apply {
                    dimensions = Dimensions.FitChildren
                    addChild(this, block, Placement.Origin)
                }

        fun animation(animation: Animation, placement: Placement = currentDefaultPlacement, widthPixels: Int = animation.widthPixels, heightPixels: Int = animation.heightPixels, keepAspect: Boolean = false, params: TextureRenderParams? = null, block: Context.() -> Unit = {}) =
                renderable(animation, placement, widthPixels, heightPixels, keepAspect, params, block)

        fun texture(texture: TextureRegion, placement: Placement = currentDefaultPlacement, widthPixels: Int = texture.widthPixels, heightPixels: Int = texture.heightPixels, keepAspect: Boolean = false, params: TextureRenderParams? = null, block: Context.() -> Unit = {}) =
                renderable(Texture(texture), placement, widthPixels, heightPixels, keepAspect, params, block)

        fun texture(texture: Texture, placement: Placement = currentDefaultPlacement, widthPixels: Int = texture.widthPixels, heightPixels: Int = texture.heightPixels, keepAspect: Boolean = false, params: TextureRenderParams? = null, block: Context.() -> Unit = {}) =
                renderable(texture, placement, widthPixels, heightPixels, keepAspect, params, block)

        fun renderable(renderable: Renderable, placement: Placement = currentDefaultPlacement, widthPixels: Int = renderable.widthPixels, heightPixels: Int = renderable.heightPixels, keepAspect: Boolean = false, params: TextureRenderParams? = null, block: Context.() -> Unit = {}) =
                ElementRenderable(inElement, renderable, keepAspect).apply {
                    dimensions = Dimensions.Exact(widthPixels, heightPixels)
                    addChild(this, block, placement)
                }

        fun render(placement: Placement = currentDefaultPlacement, render: ElementDynamicRender.(xPixel: Int, yPixel: Int) -> Unit) =
                ElementDynamicRender(inElement, render).apply {
                    addChild(this, {}, placement)
                }

        fun group(placement: Placement = currentDefaultPlacement, block: Context.() -> Unit = {}) =
                GroupElement(inElement).apply {
                    dimensions = Dimensions.FitChildren
                    addChild(this, block, placement)
                }

        fun list(placement: Placement = currentDefaultPlacement, horizontalAlign: HorizontalAlign = HorizontalAlign.CENTER, padding: Int = 0, block: Context.() -> Unit = {}) =
                group(placement) {
                    dimensions = Dimensions.VerticalList(padding)
                    at(Placement.VerticalList(padding, horizontalAlign), true) {
                        block()
                    }
                }

        fun horizontalList(placement: Placement = currentDefaultPlacement, verticalAlign: VerticalAlign = VerticalAlign.CENTER, padding: Int = 0, block: Context.() -> Unit = {}) =
                group(placement) {
                    dimensions = Dimensions.HorizontalList(padding)
                    at(Placement.HorizontalList(padding, verticalAlign), true) {
                        block()
                    }
                }

        fun text(text: String, placement: Placement = currentDefaultPlacement, allowTags: Boolean = false, ignoreLines: Boolean = false, params: TextRenderParams = TextRenderParams(), block: Context.() -> Unit = {}) =
                ElementText(inElement, text, allowTags, ignoreLines, params).apply {
                    dimensions = Dimensions.Text(text, allowTags, ignoreLines, params)
                    addChild(this, block, placement)
                }

        fun button(placement: Placement = currentDefaultPlacement, onPress: GuiElement.(Interaction) -> Unit = {}, onRelease: GuiElement.(Interaction) -> Unit = {}, block: Context.() -> Unit = {}) =
                GroupElement(inElement).apply {
                    dimensions = Dimensions.FitChildren
                    onInteractOn = {
                        if (it.event.type == ControlEventType.PRESS) {
                            onPress(it)
                        } else if (it.event.type == ControlEventType.RELEASE) {
                            onRelease(it)
                        }
                    }
                    addChild(this, block, placement)
                }

        fun button(placement: Placement = currentDefaultPlacement, text: String, onPress: GuiElement.(Interaction) -> Unit = {}, onRelease: GuiElement.(Interaction) -> Unit = {}, block: Context.() -> Unit = {}) =
                button(placement, onPress, onRelease, {
                    text(text, Placement.Align.Center)
                    block()
                })

        fun levelView(placement: Placement = currentDefaultPlacement, camera: LevelObject, block: Context.() -> Unit = {}) =
                ElementLevelView(inElement, camera).apply {
                    addChild(this, block, placement)
                }

        fun resourceContainerView(container: ResourceContainer, width: Int, height: Int, placement: Placement = currentDefaultPlacement, allowSelection: Boolean = false, onSelect: (type: ResourceType, quantity: Int, interaction: Interaction) -> Unit = { _, _, _ -> }, block: Context.() -> Unit = {}) =
                ElementResourceContainer(inElement, width, height, container, allowSelection, onSelect).apply {
                    addChild(this, block, placement)
                }

        fun closeButton(placement: Placement = currentDefaultPlacement, block: Context.() -> Unit = {}) =
                ElementCloseButton(inElement).apply {
                    addChild(this, block, placement)
                }

        fun progressBar(maxProgress: Int, getProgress: () -> Int = { 0 }, dimensions: Dimensions = Dimensions.Exact(48, 6), placement: Placement = currentDefaultPlacement, block: Context.() -> Unit = {}) =
                ElementProgressBar(inElement, maxProgress, getProgress).apply {
                    this.dimensions = dimensions
                    addChild(this, block, placement)
                }

        fun inventory(inventory: Inventory, placement: Placement = currentDefaultPlacement, block: Context.() -> Unit = {}) =
                ElementInventory(inElement, inventory).apply {
                    addChild(this, block, placement)
                }

        fun fluidTank(fluidTank: FluidTank, dimensions: Dimensions = Dimensions.Exact(60, 40), placement: Placement = currentDefaultPlacement, block: Context.() -> Unit = {}) =
                ElementFluidTank(inElement, fluidTank).apply {
                    this.dimensions = dimensions
                    addChild(this, block, placement)
                }

        fun progressArrow(from: GuiElement, fromDir: Int, to: GuiElement, toDir: Int,
                          maxProgress: Int, getProgress: () -> Int = { 0 },
                          backgroundColor: Color = toColor(0xFFFFFF), progressColor: Color = toColor(0x00BC06), block: Context.() -> Unit = {}) =
                ElementProgressArrow(inElement, from, to, maxProgress, getProgress, fromDir, toDir, backgroundColor, progressColor).apply {
                    addChild(this, block, Placement.Origin)
                }
    }
}

class GroupElement(parent: GuiElement) : GuiElement(parent)

class ElementDynamicRender(parent: GuiElement, val render: ElementDynamicRender.(xPixel: Int, yPixel: Int) -> Unit = { _, _ -> }) : GuiElement(parent) {
    override fun render(params: TextureRenderParams?) {
        render(absoluteXPixel, absoluteYPixel)
        super.render(params)
    }
}

class ElementRenderable(parent: GuiElement, var renderable: Renderable, var keepAspect: Boolean = false, var params: TextureRenderParams? = null) : GuiElement(parent) {
    override fun render(params: TextureRenderParams?) {
        if (params == null && this.params == null) {
            renderable.render(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels, keepAspect)
        } else if (params == null) {
            renderable.render(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels, keepAspect, this.params!!)
        } else if (this.params == null) {
            renderable.render(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels, keepAspect, params)
        } else {
            renderable.render(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels, keepAspect, this.params!!.combine(params))
        }
        super.render(params)
    }
}

class ElementDefaultRectangle(parent: GuiElement) : GuiElement(parent) {
    override fun render(params: TextureRenderParams?) {
        if (params != null) {
            Renderer.renderDefaultRectangle(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels, params)
        } else {
            Renderer.renderDefaultRectangle(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels)
        }
        super.render(params)
    }
}

class ElementText(parent: GuiElement, text: String, val allowTags: Boolean = false, var ignoreLines: Boolean, var params: TextRenderParams = TextRenderParams()) : GuiElement(parent) {

    private var taggedText: TaggedText by PowerworksDelegates.lateinitVal()

    var text = text
        set(value) {
            if (allowTags) {
                taggedText = TextManager.parseTags(value)
            }
            field = value
        }

    override fun render(params: TextureRenderParams?) {
        if (allowTags) {
            Renderer.renderTaggedText(taggedText, absoluteXPixel, absoluteYPixel, this.params)
        } else {
            Renderer.renderText(text, absoluteXPixel, absoluteYPixel, this.params, ignoreLines)
        }
        super.render(params)
    }
}