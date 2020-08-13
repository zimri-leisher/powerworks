package screen.gui2

import main.Game

sealed class Dimensions {
    abstract fun getWidthPixelsFor(element: GUIElement): Int
    abstract fun getHeightPixelsFor(element: GUIElement): Int

    fun contract(amount: Int) = Padded(this, -amount, -amount)

    fun grow(amount: Int) = Padded(this, amount, amount)

    fun addWidth(amount: Int) = Padded(this, amount, 0)

    fun addHeight(amount: Int) = Padded(this, 0, amount)

    class Padded(val dimensionsToPad: Dimensions, val xPadding: Int, val yPadding: Int) : Dimensions() {
        override fun getWidthPixelsFor(element: GUIElement) = dimensionsToPad.getWidthPixelsFor(element) + xPadding
        override fun getHeightPixelsFor(element: GUIElement) = dimensionsToPad.getHeightPixelsFor(element) + yPadding
    }

    class Exact(val widthPixels: Int, val heightPixels: Int) : Dimensions() {
        override fun getWidthPixelsFor(element: GUIElement) = widthPixels
        override fun getHeightPixelsFor(element: GUIElement) = heightPixels
    }

    object MatchParent : Dimensions() {
        override fun getWidthPixelsFor(element: GUIElement) = element.parent.widthPixels
        override fun getHeightPixelsFor(element: GUIElement) = element.parent.heightPixels
    }

    object FitChildren : Dimensions() {
        // the furthest right edge
        override fun getWidthPixelsFor(element: GUIElement): Int {
            // find the size of each alignment group and add them together
            var maxLeft = 0
            var maxCenter = 0
            var maxRight = 0
            for (child in element.children) {
                if (child.horizontalAlignment == HorizontalAlignment.LEFT && child.widthPixels > maxLeft) {
                    maxLeft = child.widthPixels
                }
                if (child.horizontalAlignment == HorizontalAlignment.CENTER && child.widthPixels > maxCenter) {
                    maxCenter = child.widthPixels
                }
                if (child.horizontalAlignment == HorizontalAlignment.RIGHT && child.widthPixels > maxRight) {
                    maxRight = child.widthPixels
                }
            }
            return maxLeft + maxRight + maxCenter
        }

        // the highest edge
        override fun getHeightPixelsFor(element: GUIElement): Int {
            // find the size of each alignment group and add them together
            var maxBottom = 0
            var maxCenter = 0
            var maxTop = 0
            for (child in element.children) {
                if (child.verticalAlignment == VerticalAlignment.BOTTOM && child.heightPixels > maxBottom) {
                    maxBottom = child.heightPixels
                }
                if (child.verticalAlignment == VerticalAlignment.CENTER && child.heightPixels > maxCenter) {
                    maxCenter = child.heightPixels
                }
                if (child.verticalAlignment == VerticalAlignment.TOP && child.heightPixels > maxTop) {
                    maxTop = child.heightPixels
                }
            }
            return maxBottom + maxTop + maxCenter
        }
    }

    object Fullscreen : Dimensions() {
        override fun getWidthPixelsFor(element: GUIElement) = Game.WIDTH

        override fun getHeightPixelsFor(element: GUIElement) = Game.HEIGHT
    }
}