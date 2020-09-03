package screen.gui

import main.Game

enum class HorizontalAlign {
    LEFT, CENTER, RIGHT
}

enum class VerticalAlign {
    TOP, CENTER, BOTTOM
}

sealed class Placement {

    abstract fun get(element: GuiElement, gui: Gui): Exact

    fun offset(xOffset: Int, yOffset: Int) = Offset(this, xOffset, yOffset)

    class Offset(val base: Placement, val xOffset: Int, val yOffset: Int) : Placement() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            val basePlacement = base.get(element, gui)
            if (basePlacement is Unknown) {
                return Unknown(xOffset + basePlacement.xPixel, yOffset + basePlacement.yPixel)
            }
            return Exact(xOffset + basePlacement.xPixel, yOffset + basePlacement.yPixel)
        }
    }

    open class Exact(val xPixel: Int, val yPixel: Int) : Placement() {

        override fun get(element: GuiElement, gui: Gui) = this

        override fun toString(): String {
            return "Exact($xPixel, $yPixel)"
        }
    }

    class Unknown(xPixel: Int, yPixel: Int) : Exact(xPixel, yPixel)

    object Origin : Exact(0, 0)

    open class Align(val horizontalAlign: HorizontalAlign = HorizontalAlign.CENTER, val verticalAlign: VerticalAlign = VerticalAlign.CENTER) : Placement() {

        override fun get(element: GuiElement, gui: Gui): Exact {
            val parentDimensions = if (element.isRoot) Dimensions.Exact(Game.WIDTH, Game.HEIGHT) else gui.layout.getExactDimensions(element.parent)
            if (parentDimensions is Dimensions.None || (horizontalAlign == HorizontalAlign.LEFT && verticalAlign == VerticalAlign.BOTTOM)) {
                return Origin
            }
            val elementDimensions = gui.layout.getExactDimensions(element)
            val xPixel = when (horizontalAlign) {
                HorizontalAlign.LEFT -> 0
                HorizontalAlign.CENTER -> parentDimensions.widthPixels / 2 - elementDimensions.widthPixels / 2
                HorizontalAlign.RIGHT -> parentDimensions.widthPixels - elementDimensions.widthPixels
            }
            val yPixel = when (verticalAlign) {
                VerticalAlign.BOTTOM -> 0
                VerticalAlign.CENTER -> parentDimensions.heightPixels / 2 - elementDimensions.heightPixels / 2
                VerticalAlign.TOP -> parentDimensions.heightPixels - elementDimensions.heightPixels
            }
            return if (parentDimensions is Dimensions.Unknown || elementDimensions is Dimensions.Unknown) Unknown(xPixel, yPixel) else Exact(xPixel, yPixel)
        }

        object Center : Align(HorizontalAlign.CENTER, VerticalAlign.CENTER)
    }

    class VerticalList(val padding: Int, val align: HorizontalAlign = HorizontalAlign.CENTER) : Placement() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            val siblingDimensions = element.parent.children.map { it to gui.layout.getExactDimensions(it) }.toMap()
            val siblingsInList = siblingDimensions.filterKeys { it.placement is VerticalList }
            var heightSoFar = 0
            var anyUnknown = false
            for ((sibling, dimensions) in siblingsInList) {
                if (dimensions is Dimensions.Unknown) {
                    anyUnknown = true
                }
                heightSoFar += dimensions.heightPixels + padding
                if (sibling == element) {
                    break
                }
            }
            var xPixel = 0
            if (align != HorizontalAlign.LEFT) {
                val elementDimensions = gui.layout.getExactDimensions(element)
                val parentDimensions = gui.layout.getExactDimensions(element.parent)
                anyUnknown = anyUnknown || elementDimensions is Dimensions.Unknown || parentDimensions is Dimensions.Unknown
                if (align == HorizontalAlign.CENTER) {
                    xPixel = parentDimensions.widthPixels / 2 - elementDimensions.widthPixels / 2
                } else {
                    xPixel = parentDimensions.widthPixels - elementDimensions.widthPixels
                }
            }
            return if (anyUnknown) Unknown(xPixel, gui.layout.getExactDimensions(element.parent).heightPixels - heightSoFar)
            else Exact(xPixel, gui.layout.getExactDimensions(element.parent).heightPixels - heightSoFar)
        }
    }

    class HorizontalList(val padding: Int, val align: VerticalAlign = VerticalAlign.CENTER) : Placement() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            val siblingDimensions = element.parent.children.map { it to gui.layout.getExactDimensions(it) }.toMap()
            val siblingsInList = siblingDimensions.filterKeys { it.placement is HorizontalList }
            var widthSoFar = 0
            var anyUnknown = false
            for ((sibling, dimensions) in siblingsInList) {
                if (dimensions is Dimensions.Unknown) {
                    anyUnknown = true
                }
                if (sibling == element) {
                    break
                }
                widthSoFar += dimensions.widthPixels + padding
            }
            var yPixel = 0
            if (align != VerticalAlign.BOTTOM) {
                val elementDimensions = gui.layout.getExactDimensions(element)
                val parentDimensions = gui.layout.getExactDimensions(element.parent)
                anyUnknown = anyUnknown || elementDimensions is Dimensions.Unknown || parentDimensions is Dimensions.Unknown
                if (align == VerticalAlign.CENTER) {
                    yPixel = parentDimensions.heightPixels / 2 - elementDimensions.heightPixels / 2
                } else {
                    yPixel = parentDimensions.heightPixels - elementDimensions.heightPixels
                }
            }
            return if (anyUnknown) Unknown(widthSoFar, yPixel)
            else Exact(widthSoFar, yPixel)
        }
    }

    class Dynamic(val x: () -> Int, val y: () -> Int) : Placement() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            return Exact(x(), y())
        }
    }
}