package screen.gui2

import graphics.Renderer
import graphics.TextureRenderParams
import misc.Geometry
import screen.mouse.Mouse
import screen.mouse.Tooltips
import kotlin.math.ceil


open class ElementIconList(parent: GuiElement,
                           columns: Int,
                           rows: Int,
                           var renderIcon: ElementIconList.(xPixel: Int, yPixel: Int, index: Int) -> Unit,
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
            selectedIcon = getIconAt(interaction.xPixel, interaction.yPixel)
            if (selectedIcon != -1) {
                onSelectIcon(selectedIcon, interaction)
            }
        }
    }

    override fun render(params: TextureRenderParams?) {
        val actualParams = params ?: TextureRenderParams.DEFAULT
        for (index in 0 until iconCount) {
            val x = (index % columns) * (iconSize + ICON_PADDING)
            val y = heightPixels - (index / columns + 1) * (iconSize + ICON_PADDING) + ICON_PADDING
            if (index == selectedIcon) {
                Renderer.renderDefaultRectangle(x + absoluteXPixel, y + absoluteYPixel, iconSize, iconSize, actualParams.combine(TextureRenderParams(brightness = 1.3f, rotation = 180f)))
            } else if (index == highlightedIcon) {
                Renderer.renderDefaultRectangle(x + absoluteXPixel, y + absoluteYPixel, iconSize, iconSize, actualParams.combine(TextureRenderParams(brightness = 1.1f, rotation = 180f)))
            } else {
                Renderer.renderDefaultRectangle(x + absoluteXPixel, y + absoluteYPixel, iconSize, iconSize, actualParams.combine(TextureRenderParams(rotation = 180f)))
            }
            renderIcon(x + absoluteXPixel, y + absoluteYPixel, index)
        }
        super.render(params)
    }

    fun getIconAt(xPixel: Int, yPixel: Int): Int {
        for (index in 0 until iconCount) {
            val x = (index % columns) * (iconSize + ICON_PADDING)
            val y = heightPixels - (index / columns + 1) * (iconSize + ICON_PADDING) + ICON_PADDING
            if (Geometry.contains(x + this.absoluteXPixel, y + this.absoluteYPixel, iconSize, iconSize, xPixel, yPixel, 0, 0)) {
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
                if (it is ElementIconList) {
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