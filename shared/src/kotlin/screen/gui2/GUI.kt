package screen.gui2

import graphics.*

abstract class GUI(block: GUIElement.GUIContext.() -> Unit) {

    private val parentElement = object : GUIElement(null) {
        override val xPixel = 0
        override val yPixel = 0
    }

    private val internalContext = GUIElement.GUIContext(parentElement)

    init {
        block(internalContext)
    }

    fun render(params: TextureRenderParams? = null) {
        parentElement.render(params)
    }

    fun update() {
        parentElement.update()
    }
}

enum class HorizontalAlignment {
    CENTER, LEFT, RIGHT
}

enum class VerticalAlignment {
    CENTER, TOP, BOTTOM
}

abstract class GUIElement(parent: GUIElement?) {

    private var mutableParent: GUIElement? = parent
    val parent: GUIElement
        get() = mutableParent!!

    private val mutableChildren = mutableListOf<GUIElement>()
    val children: List<GUIElement> get() = mutableChildren

    var horizontalAlignment = HorizontalAlignment.CENTER
    var verticalAlignment = VerticalAlignment.CENTER

    protected var dimensions: Dimensions = Dimensions.FitChildren

    val widthPixels: Int
        get() {
            // if we depend on each other, just return 0
            if (dimensions is Dimensions.MatchParent && parent.dimensions is Dimensions.FitChildren) {
                return 0
            }
            return dimensions.getWidthPixelsFor(this)
        }

    val heightPixels: Int
        get() {
            // if we depend on each other, just return 0
            if (dimensions is Dimensions.MatchParent && parent.dimensions is Dimensions.FitChildren) {
                return 0
            }
            return dimensions.getHeightPixelsFor(this)
        }

    open val xPixel: Int
        get() {
            return when (horizontalAlignment) {
                HorizontalAlignment.LEFT -> 0
                HorizontalAlignment.RIGHT -> parent.widthPixels - widthPixels
                HorizontalAlignment.CENTER -> parent.widthPixels / 2 - widthPixels / 2
            }
        }
    val absoluteXPixel: Int get() = parent.absoluteXPixel + xPixel

    open val yPixel: Int
        get() =
            when (verticalAlignment) {
                VerticalAlignment.BOTTOM -> 0
                VerticalAlignment.TOP -> parent.heightPixels - heightPixels
                VerticalAlignment.CENTER -> parent.heightPixels / 2 - heightPixels / 2
            }
    val absoluteYPixel: Int get() = parent.absoluteYPixel + yPixel

    open fun render(params: TextureRenderParams?) {

    }

    open fun update() {

    }

    class GUIContext(val inElement: GUIElement) {

        var currentHorizontalAlignment = HorizontalAlignment.CENTER
        var currentVerticalAlignment = VerticalAlignment.CENTER

        private fun addChild(child: GUIElement, block: GUIContext.() -> Unit, horizontalAlignment: HorizontalAlignment = currentHorizontalAlignment, verticalAlignment: VerticalAlignment = currentVerticalAlignment) {
            child.horizontalAlignment = horizontalAlignment
            child.verticalAlignment = verticalAlignment
            inElement.mutableChildren.add(child)
            block(GUIContext(child))
        }

        fun align(horizontalAlignment: HorizontalAlignment = currentHorizontalAlignment, verticalAlignment: VerticalAlignment = currentVerticalAlignment, block: GUIContext.() -> Unit) {
            val oldVerticalAlignment = currentVerticalAlignment
            val oldHorizontalAlignment = currentHorizontalAlignment
            currentVerticalAlignment = verticalAlignment
            currentHorizontalAlignment = horizontalAlignment
            block()
            currentVerticalAlignment = oldVerticalAlignment
            currentHorizontalAlignment = oldHorizontalAlignment
        }

        var dimensions
            get() = inElement.dimensions
            set(value) {
                inElement.dimensions = value
            }

        fun background(block: GUIContext.() -> Unit = {}) =
                DefaultRectangleElement(inElement).apply {
                    dimensions = Dimensions.FitChildren
                    addChild(this, block)
                }

        fun animation(animation: Animation, block: GUIContext.() -> Unit = {}, horizontalAlignment: HorizontalAlignment = currentHorizontalAlignment, verticalAlignment: VerticalAlignment = currentVerticalAlignment) =
                renderable(animation, block, horizontalAlignment, verticalAlignment)

        fun texture(texture: Texture, block: GUIContext.() -> Unit = {}, horizontalAlignment: HorizontalAlignment = currentHorizontalAlignment, verticalAlignment: VerticalAlignment = currentVerticalAlignment) =
                renderable(texture, block, horizontalAlignment, verticalAlignment)

        fun renderable(renderable: Renderable, block: GUIContext.() -> Unit = {}, horizontalAlignment: HorizontalAlignment = currentHorizontalAlignment, verticalAlignment: VerticalAlignment = currentVerticalAlignment) =
                RenderableElement(inElement, renderable).apply {
                    dimensions = Dimensions.Exact(renderable.widthPixels, renderable.heightPixels)
                    addChild(this, block, horizontalAlignment, verticalAlignment)
                }
    }
}

enum class GroupFormatting {
    VERTICAL_LIST, HORIZONTAL_LIST, CLUSTER
}

class GroupElement(parent: GUIElement, val formatting: GroupFormatting) : GUIElement(parent) {

}

class RenderableElement(parent: GUIElement, var renderable: Renderable) : GUIElement(parent) {
    override fun render(params: TextureRenderParams?) {
        if (params != null) {
            renderable.render(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels)
        }
    }
}

class DefaultRectangleElement(parent: GUIElement) : GUIElement(parent) {
    override fun render(params: TextureRenderParams?) {
        if (params != null) {
            Renderer.renderDefaultRectangle(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels, params)
        } else {
            Renderer.renderDefaultRectangle(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels)
        }
    }
}