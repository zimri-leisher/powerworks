package screen.gui

import graphics.text.TaggedText
import graphics.text.TextManager
import graphics.text.TextRenderParams
import main.Game
import main.PowerworksDelegates
import java.lang.Integer.max

sealed class Dimensions {

    abstract fun get(element: GuiElement, gui: Gui): Exact

    fun pad(width: Int = 0, height: Int = 0) = Padded(this, width, height)

    open class Exact(val width: Int, val height: Int) : Dimensions() {
        override fun get(element: GuiElement, gui: Gui) = this

        override fun toString(): String {
            return "Exact($width, $height)"
        }
    }

    object None : Exact(0, 0)

    class Unknown(width: Int, height: Int) : Exact(width, height)

    class Padded(val base: Dimensions, val widthPad: Int, val heightPad: Int) : Dimensions() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            val baseDimensions = base.get(element, gui)
            if(baseDimensions is Unknown) {
                return Unknown(baseDimensions.width + widthPad, baseDimensions.height + heightPad)
            }
            return Exact(baseDimensions.width + widthPad, baseDimensions.height + heightPad)
        }
    }

    object FitChildren : Dimensions() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            var xRange = 0 until 0
            var yRange = 0 until 0
            var anyUnknown = false
            for ((placement, dimensions) in element.children.map { gui.layout.getExactPlacement(it) to gui.layout.getExactDimensions(it) }) {
                if (placement is Placement.Unknown || dimensions is Unknown) {
                    anyUnknown = true
                }
                if (placement.x < xRange.first) {
                    xRange = placement.x..xRange.last
                }
                if (placement.x + dimensions.width > xRange.last) {
                    xRange = xRange.first..(placement.x + dimensions.width)
                }
                if (placement.y < yRange.first) {
                    yRange = placement.y..yRange.last
                }
                if (placement.y + dimensions.height > yRange.last) {
                    yRange = yRange.first..(placement.y + dimensions.height)
                }
            }
            return if (anyUnknown) Unknown(max(0, xRange.last) - max(0, xRange.first), max(0, yRange.last) - max(0, yRange.first))
            else Exact(max(0, xRange.last) - max(0, xRange.first), max(0, yRange.last) - max(0, yRange.first))
        }
    }

    class VerticalList(val padding: Int) : Dimensions() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            val dimensions = element.children.filter { it.placement is Placement.VerticalList && it.open }.map { gui.layout.getExactDimensions(it) }
            return if (dimensions.any { it is Unknown }) Unknown(
                dimensions.maxByOrNull { it.width }?.width
                    ?: 0, dimensions.sumBy { it.height + padding } - padding)
            else Exact(dimensions.maxByOrNull { it.width }?.width ?: 0, dimensions.sumBy { it.height + padding } - padding)
        }
    }

    class HorizontalList(val padding: Int) : Dimensions() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            val dimensions = element.children.filter { it.placement is Placement.HorizontalList && it.open }.map { gui.layout.getExactDimensions(it) }
            return if (dimensions.any { it is Unknown }) Unknown(dimensions.sumBy { it.width + padding } - padding,
                    dimensions.maxByOrNull { it.height }?.height ?: 0)
            else Exact(dimensions.sumBy { it.width + padding } - padding, dimensions.maxByOrNull { it.height }?.height ?: 0)
        }
    }

    object MatchParent : Dimensions() {
        override fun get(element: GuiElement, gui: Gui) = gui.layout.getExactDimensions(element.parent)
    }

    object Fullscreen : Dimensions() {
        override fun get(element: GuiElement, gui: Gui) = Exact(Game.WIDTH, Game.HEIGHT)
    }

    class Text(text: String, var allowTags: Boolean = false, var ignoreLines: Boolean = false, var params: TextRenderParams) : Dimensions() {

        private var taggedText: TaggedText by PowerworksDelegates.lateinitVal()

        var text = text
            set(value) {
                if (allowTags) {
                    taggedText = TextManager.parseTags(value)
                }
                field = value
            }

        override fun get(element: GuiElement, gui: Gui): Exact {
            val bounds = if (allowTags) {
                TextManager.getStringBounds(taggedText, params.size, params.style)
            } else {
                TextManager.getStringBounds(text, params.size, params.style)
            }
            return Exact(bounds.width, bounds.height)
        }

    }

    class Dynamic(val width: () -> Int, val height: () -> Int) : Dimensions() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            return Exact(width(), height())
        }
    }
}