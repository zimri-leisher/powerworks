package screen.elements

import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import main.toColor
import resource.ResourceType
import screen.ResourceTypeSelector

class GUIResourceTypeSelection(parent: RootGUIElement, name: String,
                               xAlignment: Alignment, yAlignment: Alignment,
                               /**
                                * For a [ResourceType] to show up in the selector, it must pass this predicate. You
                                * can use this to rule out categories of resource types that you don't want. [resource.ResourceTypeGroup]s
                                * with elements that both match and don't match this predicate will only show elements
                                * that match it. If none of their elements match it, they will not show up at all
                                */
                               var typeSelectionPredicate: (ResourceType) -> Boolean = { true },
                               /**
                                * The number of columns, or essentially the number of types per row.
                                * If [allowRowGrowth] is false, [columns] * [rows] is the maximum number of types or type groups
                                * that can be selected
                                */
                               columns: Int = 3,
                               /**
                                * The number of rows to start off with. If [allowRowGrowth] is false, no more will be grown
                                * and so [columns] * [rows] is the maximum number of types or groups that can be selected
                                */
                               rows: Int = 1,
                               /**
                                * Whether or not to allow the number of rows to grow as types get selected
                                */
                               allowRowGrowth: Boolean = false,
                               /**
                                * The maximum number of rows to allow growth to. This does nothing if [allowRowGrowth] is false, and
                                * will allow infinite row growth if the value is -1
                                */
                               maxRows: Int = -1,
                               startingTypes: MutableList<ResourceType> = mutableListOf(),
                               open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIIconList(parent, name, xAlignment, yAlignment, columns, rows, { _, _, _ -> }, 1, allowSelection = true, allowRowGrowth = allowRowGrowth, maxRows = maxRows, open = open, layer = layer, iconSize = 8) {

    val currentTypes = startingTypes
    private var waitingForSelection = false

    init {
        getToolTip = {
            if (it != currentTypes.size)
                currentTypes[it].name
            else
                null
        }
        renderIcon = { xPixel, yPixel, index -> renderIconAt(xPixel, yPixel, index) }
        onSelectIcon = { onSelect(it) }
    }

    private fun onSelect(index: Int) {
        if (index > currentTypes.lastIndex) {
            // adding a new type
            ResourceTypeSelector.possibleTypePredicate = { it !in currentTypes && typeSelectionPredicate(it) }
            waitingForSelection = true
            ResourceTypeSelector.open = true
            ResourceTypeSelector.windowGroup.bringToTop(ResourceTypeSelector)
        } else {
            currentTypes.removeAt(index)
            iconCount = currentTypes.size + 1
        }
        selectedIcon = -1
    }

    private fun renderIconAt(xPixel: Int, yPixel: Int, index: Int) {
        if (index > currentTypes.lastIndex) {
            // it is the plus button
            Renderer.renderTexture(Image.GUI.PLUS, xPixel + 2, yPixel + 2, 4, 4, localRenderParams.combine(TextureRenderParams(color = toColor(0.6f, 1f, 0.6f))))
        } else {
            val type = currentTypes[index]
            val isHighlighted = highlightedIcon == index
            if (isHighlighted) {
                type.icon.render(xPixel, yPixel, iconSize, iconSize, true, localRenderParams.combine(TextureRenderParams(color = toColor(alpha = 0.4f))))
                Renderer.renderTexture(Image.GUI.MINUS, xPixel + 2, yPixel + 2, 4, 4, localRenderParams.combine(TextureRenderParams(color = toColor(r = 1f, g = 0.6f, b = 0.6f))))
            } else {
                type.icon.render(xPixel, yPixel, iconSize, iconSize, true, localRenderParams)
            }
        }
    }

    override fun update() {
        if (waitingForSelection) {
            val selected = ResourceTypeSelector.getSelection()
            if (selected != null) {
                selected.forEach { if (it !in currentTypes) currentTypes.add(it) }
                iconCount = Math.min(currentTypes.size + 1, rows * columns)
                ResourceTypeSelector.open = false
                waitingForSelection = false
            }
        }
        super.update()
    }

}