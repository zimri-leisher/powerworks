package screen.elements

import graphics.Renderer
import graphics.TextureRenderParams
import io.ControlEvent
import io.ControlEventType
import misc.Geometry
import screen.mouse.Mouse
import screen.mouse.Tooltips
import kotlin.math.ceil

open class GUIIconList(parent: RootGUIElement, name: String,
                       xAlignment: Alignment, yAlignment: Alignment,
                       columns: Int,
                       rows: Int,
                       var renderIcon: GUIIconList.(xPixel: Int, yPixel: Int, index: Int) -> Unit,
                       startingIconCount: Int = columns * rows,
                       var iconSize: Int = 16,
                       var allowSelection: Boolean = false,
                       var onSelectIcon: (index: Int) -> Unit = {},
                       var getToolTip: ((index: Int) -> String?)? = null,
                       var allowRowGrowth: Boolean = false,
                       var maxRows: Int = -1,
                       open: Boolean = false,
                       layer: Int = parent.layer + 1) : GUIElement(parent, name, xAlignment, yAlignment, { columns * (iconSize + ICON_PADDING) - ICON_PADDING }, { rows * (iconSize + ICON_PADDING) - ICON_PADDING }, open, layer) {

    var rows = rows
        set(value) {
            if (field != value) {
                field = value
                alignments.update()
            }
        }
    var columns = columns
        set(value) {
            if (field != value) {
                field = value
                alignments.update()
            }
        }
    var iconCount =
            if (startingIconCount > columns * rows)
                throw IllegalArgumentException("Starting icon count ${startingIconCount} cannot be bigger than rows * columns (${rows * columns}")
            else startingIconCount
        set(value) {
            if (field != value) {
                if (value > rows * columns) {
                    if (allowRowGrowth && (maxRows == -1 || columns * maxRows < value)) {
                        // if we will be able to fit this
                        field = value
                        rows = ceil(field.toDouble() / columns).toInt()
                    } else {
                        field = rows * columns
                        // set it to be as big as possible
                    }
                } else {
                    field = value
                }
            }
        }

    var highlightedIcon = -1
    var selectedIcon = -1

    init {
        alignments.width = { this.columns * (iconSize + ICON_PADDING) - ICON_PADDING }
        alignments.height = { this.rows * (iconSize + ICON_PADDING) - ICON_PADDING }
        val lastYAlignment = alignments.y
        alignments.y = { lastYAlignment() - ((this.rows - 1) * (iconSize + ICON_PADDING)) }
    }

    override fun onInteractOn(event: ControlEvent, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (allowSelection && event.type == ControlEventType.PRESS && button == 0) {
            selectedIcon = getIconAt(xPixel, yPixel)
            if (selectedIcon != -1) {
                onSelectIcon(selectedIcon)
            }
        }
    }

    override fun render() {
        for (index in 0 until iconCount) {
            val x = (index % columns) * (iconSize + ICON_PADDING)
            val y = heightPixels - (index / columns + 1) * (iconSize + ICON_PADDING)
            if (index == selectedIcon) {
                Renderer.renderDefaultRectangle(x + xPixel, y + yPixel, iconSize, iconSize, localRenderParams.combine(TextureRenderParams(brightness = 1.3f)))
            } else if (index == highlightedIcon) {
                Renderer.renderDefaultRectangle(x + xPixel, y + yPixel, iconSize, iconSize, localRenderParams.combine(TextureRenderParams(brightness = 1.1f)))
            } else {
                Renderer.renderDefaultRectangle(x + xPixel, y + yPixel, iconSize, iconSize, localRenderParams)
            }
            renderIcon(x + xPixel, y + yPixel, index)
        }
    }

    fun getIconAt(xPixel: Int, yPixel: Int): Int {
        for (index in 0 until iconCount) {
            val x = (index % columns) * (iconSize + ICON_PADDING)
            val y = heightPixels - (index / columns + 1) * (iconSize + ICON_PADDING)
            if (Geometry.contains(x + this.xPixel, y + this.yPixel, iconSize, iconSize, xPixel, yPixel, 0, 0)) {
                return index
            }
        }
        return -1
    }

    override fun update() {
        highlightedIcon = if (mouseOn) getIconAt(Mouse.xPixel, Mouse.yPixel) else -1
    }

    companion object {
        const val ICON_PADDING = 1

        init {
            Tooltips.addScreenTooltipTemplate({
                if (it is GUIIconList) {
                    if (it.getToolTip != null) {
                        val index = it.getIconAt(Mouse.xPixel, Mouse.yPixel)
                        if (index != -1) {
                            return@addScreenTooltipTemplate it.getToolTip!!.invoke(index)
                        }
                    }
                }
                return@addScreenTooltipTemplate null
            }, 1)
        }
    }
}