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

    class Offset(val base: Placement, val x: Int, val y: Int) : Placement() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            val basePlacement = base.get(element, gui)
            if (basePlacement is Unknown) {
                return Unknown(x + basePlacement.x, y + basePlacement.y)
            }
            return Exact(x + basePlacement.x, y + basePlacement.y)
        }
    }

    open class Exact(val x: Int, val y: Int) : Placement() {

        override fun get(element: GuiElement, gui: Gui) = this

        override fun toString(): String {
            return "Exact($x, $y)"
        }
    }

    class Unknown(x: Int, y: Int) : Exact(x, y)

    object Origin : Exact(0, 0)

    class Follow(val element: GuiElement) : Placement() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            return Exact(element.absoluteX, element.absoluteY)
        }
    }

    open class Align(val horizontalAlign: HorizontalAlign = HorizontalAlign.CENTER, val verticalAlign: VerticalAlign = VerticalAlign.CENTER) : Placement() {

        override fun get(element: GuiElement, gui: Gui): Exact {
            val parentDimensions = if (element.isRoot) Dimensions.Exact(Game.WIDTH, Game.HEIGHT) else gui.layout.getExactDimensions(element.parent)
            if (parentDimensions is Dimensions.None || (horizontalAlign == HorizontalAlign.LEFT && verticalAlign == VerticalAlign.BOTTOM)) {
                return Origin
            }
            val elementDimensions = gui.layout.getExactDimensions(element)
            val x = when (horizontalAlign) {
                HorizontalAlign.LEFT -> 0
                HorizontalAlign.CENTER -> parentDimensions.width / 2 - elementDimensions.width / 2
                HorizontalAlign.RIGHT -> parentDimensions.width - elementDimensions.width

            }
            val y = when (verticalAlign) {
                VerticalAlign.BOTTOM -> 0
                VerticalAlign.CENTER -> parentDimensions.height / 2 - elementDimensions.height / 2
                VerticalAlign.TOP -> parentDimensions.height - elementDimensions.height

            }
            return if (parentDimensions is Dimensions.Unknown || elementDimensions is Dimensions.Unknown) Unknown(x, y) else Exact(x, y)
        }

        object Center : Align(HorizontalAlign.CENTER, VerticalAlign.CENTER)
    }

    class VerticalList(val padding: Int, val align: HorizontalAlign = HorizontalAlign.CENTER) : Placement() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            val siblingDimensions = element.parent.children.map { it to gui.layout.getExactDimensions(it) }.toMap()
            val siblingsInList = siblingDimensions.filterKeys { it.placement is VerticalList && it.open }
            var heightSoFar = 0
            var anyUnknown = false
            for ((sibling, dimensions) in siblingsInList) {
                if (dimensions is Dimensions.Unknown) {
                    anyUnknown = true
                }
                heightSoFar += dimensions.height + padding
                if (sibling == element) {
                    break
                }
            }
            var x = 0
            if (align != HorizontalAlign.LEFT) {
                val elementDimensions = gui.layout.getExactDimensions(element)
                val parentDimensions = gui.layout.getExactDimensions(element.parent)
                anyUnknown = anyUnknown || elementDimensions is Dimensions.Unknown || parentDimensions is Dimensions.Unknown
                if (align == HorizontalAlign.CENTER) {
                    x = parentDimensions.width / 2 - elementDimensions.width / 2
                } else {
                    x = parentDimensions.width - elementDimensions.width
                }
            }
            return if (anyUnknown) Unknown(x, gui.layout.getExactDimensions(element.parent).height - heightSoFar)
            else Exact(x, gui.layout.getExactDimensions(element.parent).height - heightSoFar)
        }
    }

    class HorizontalList(val padding: Int, val align: VerticalAlign = VerticalAlign.CENTER) : Placement() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            val siblingDimensions = element.parent.children.map { it to gui.layout.getExactDimensions(it) }.toMap()
            val siblingsInList = siblingDimensions.filterKeys { it.placement is HorizontalList && it.open }
            var widthSoFar = 0
            var anyUnknown = false
            for ((sibling, dimensions) in siblingsInList) {
                if (dimensions is Dimensions.Unknown) {
                    anyUnknown = true
                }
                if (sibling == element) {
                    break
                }
                widthSoFar += dimensions.width + padding
            }
            var y = 0
            if (align != VerticalAlign.BOTTOM) {
                val elementDimensions = gui.layout.getExactDimensions(element)
                val parentDimensions = gui.layout.getExactDimensions(element.parent)
                anyUnknown = anyUnknown || elementDimensions is Dimensions.Unknown || parentDimensions is Dimensions.Unknown
                if (align == VerticalAlign.CENTER) {
                    y = parentDimensions.height / 2 - elementDimensions.height / 2
                } else {
                    y = parentDimensions.height - elementDimensions.height
                }
            }
            return if (anyUnknown) Unknown(widthSoFar, y)
            else Exact(widthSoFar, y)
        }
    }

    class Dynamic(val x: () -> Int, val y: () -> Int) : Placement() {
        override fun get(element: GuiElement, gui: Gui): Exact {
            return Exact(x(), y())
        }
    }
}