package screen.element

import graphics.Renderer
import graphics.TextureRenderParams
import misc.Geometry
import misc.Coord
import screen.gui.Dimensions
import screen.gui.GuiElement
import screen.Interaction
import screen.mouse.Mouse
import screen.mouse.Tooltips
import kotlin.math.ceil


open class ElementIconList(parent: GuiElement,
                           columns: Int,
                           rows: Int,
                           var renderIcon: ElementIconList.(x: Int, y: Int, index: Int) -> Unit,
                           startingIconCount: Int = columns * rows,
                           var iconSize: Int = 16,
                           var allowSelection: Boolean = false,
                           var onSelectIcon: (index: Int, interaction: Interaction) -> Unit = { _, _ -> },
                           var getToolTip: ((index: Int) -> String?)? = null,
                           var allowRowGrowth: Boolean = false,
                           var maxRows: Int = -1) : GuiElement(parent) {

    var rows = rows
        set(value) {
            if (field != value) {
                field = value
                gui.layout.recalculateExactDimensions(this)
            }
        }
    var columns = columns
        set(value) {
            if (field != value) {
                field = value
                gui.layout.recalculateExactDimensions(this)
            }
        }
    var iconCount =
            if (startingIconCount > columns * rows)
                throw IllegalArgumentException("Starting icon count $startingIconCount cannot be bigger than rows * columns (${rows * columns}")
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

    var onMouseEnterIcon: (index: Int) -> Unit = {}
    var onMouseLeaveIcon: (index: Int) -> Unit = {}


    var highlightedIcon = -1
    var selectedIcon = -1

    init {
        dimensions = Dimensions.Dynamic({
            this.columns * (iconSize + ICON_PADDING) - ICON_PADDING
        }, {
            this.rows * (iconSize + ICON_PADDING) - ICON_PADDING
        })
    }

    override fun onInteractOn(interaction: Interaction) {
        if (allowSelection) {
            selectedIcon = getIconAt(interaction.x, interaction.y)
            if (selectedIcon != -1) {
                onSelectIcon(selectedIcon, interaction)
            }
        }
        super.onInteractOn(interaction)
    }

    override fun render(params: TextureRenderParams?) {
        val actualParams = params ?: TextureRenderParams.DEFAULT
        for (index in 0 until iconCount) {
            val x = (index % columns) * (iconSize + ICON_PADDING)
            val y = height - (index / columns + 1) * (iconSize + ICON_PADDING) + ICON_PADDING
            if (index == selectedIcon) {
                Renderer.renderDefaultRectangle(x + absoluteX, y + absoluteY, iconSize, iconSize, actualParams.combine(TextureRenderParams(brightness = 1.3f, rotation = 180f)))
            } else if (index == highlightedIcon) {
                Renderer.renderDefaultRectangle(x + absoluteX, y + absoluteY, iconSize, iconSize, actualParams.combine(TextureRenderParams(brightness = 1.1f, rotation = 180f)))
            } else {
                Renderer.renderDefaultRectangle(x + absoluteX, y + absoluteY, iconSize, iconSize, actualParams.combine(TextureRenderParams(rotation = 180f)))
            }
            renderIcon(x + absoluteX, y + absoluteY, index)
        }
        super.render(params)
    }

    fun getIconAt(x: Int, y: Int): Int {
        for (index in 0 until iconCount) {
            val iconX = (index % columns) * (iconSize + ICON_PADDING)
            val iconY = height - (index / columns + 1) * (iconSize + ICON_PADDING) + ICON_PADDING
            if (Geometry.contains(iconX + this.absoluteX, iconY + this.absoluteY, iconSize, iconSize, x, y, 0, 0)) {
                return index
            }
        }
        return -1
    }

    fun getIconPosition(index: Int): Coord {
        val x = (index % columns) * (iconSize + ICON_PADDING)
        val y = height - (index / columns + 1) * (iconSize + ICON_PADDING) + ICON_PADDING
        return Coord(x, y)
    }

    override fun update() {
        val newHighlightedIcon = if (mouseOn) getIconAt(Mouse.x, Mouse.y) else -1
        if (newHighlightedIcon != highlightedIcon) {
            if (newHighlightedIcon != -1) {
                onMouseEnterIcon(newHighlightedIcon)
            }
            if (highlightedIcon != -1) {
                onMouseLeaveIcon(highlightedIcon)
            }
        }
        highlightedIcon = newHighlightedIcon
        super.update()
    }

    companion object {
        const val ICON_PADDING = 1

        init {
            Tooltips.addScreenTooltipTemplate({
                if (it is ElementIconList) {
                    if (it.getToolTip != null) {
                        val index = it.getIconAt(Mouse.x, Mouse.y)
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