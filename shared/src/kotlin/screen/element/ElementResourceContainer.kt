package screen.element

import graphics.Renderer
import graphics.TextureRenderParams
import graphics.text.TextManager
import graphics.text.TextRenderParams
import io.Control
import io.ControlEventType
import item.Inventory
import item.ItemType
import main.toColor
import player.ActionTransferResourcesBetweenLevelObjects
import player.PlayerManager
import resource.*
import screen.Interaction
import screen.ScreenManager
import screen.gui.GuiElement
import screen.gui.GuiIngame
import screen.mouse.Mouse

open class ElementResourceContainer(
    parent: GuiElement, width: Int, height: Int,
    container: ResourceContainer,
    allowSelection: Boolean = false,
    var allowModification: Boolean = false,
    onSelect: (type: ResourceType, quantity: Int, interaction: Interaction) -> Unit = { _, _, _ -> }
) :
    ElementIconList(parent, width, height, allowSelection = allowSelection, renderIcon = { _, _, _ -> }),
    ResourceContainerChangeListener {

    open var container = container
        set(value) {
            if (field.id != value.id) {
                field.listeners.remove(this)
                field = value
                if (value is Inventory) {
                    columns = value.width
                    rows = value.height
                }
                currentResources = container.toMutableResourceList()
                value.listeners.add(this)
            }
        }

    var currentResources = container.toMutableResourceList()
        private set

    init {
        onSelectIcon = { index, interaction ->
            if (interaction.event.type == ControlEventType.PRESS) {
                if (index < currentResources.size) {
                    val (type, quantity) = currentResources[index]
                    onSelect(type, quantity, interaction)
                    if (allowModification && interaction.event.control == Control.INTERACT) {
                        // try to transfer resources from this to the highest elementresourcecontainer that isn't this
                        val other = ScreenManager.getSecondaryResourceContainer(this.container)
                        println("trying to transfer to $other")
                        if (other?.attachedLevelObject != null && this.container.attachedLevelObject != null) {
                            // verification will check whether or not this is possible
                            println("transferring")
                            PlayerManager.takeAction(
                                ActionTransferResourcesBetweenLevelObjects(
                                    PlayerManager.localPlayer,
                                    this.container.attachedLevelObject!!.toReference(),
                                    this.container.id,
                                    other.attachedLevelObject!!.toReference(),
                                    other.id, resourceListOf(type to quantity)
                                )
                            )
                        }
                    } else if (interaction.event.control == Control.SECONDARY_INTERACT && container.resourceCategory == ResourceCategory.ITEM) {
                        GuiIngame.Hotbar.addItemType(type as ItemType)
                    }
                } else {
                    if (interaction.event.control == Control.INTERACT && Mouse.heldItemType != null && container.resourceCategory == ResourceCategory.ITEM) {
                        val quantity = PlayerManager.localPlayer.brainRobot.inventory.getQuantity(Mouse.heldItemType!!)
                        if (quantity > 0) {
                            PlayerManager.takeAction(
                                ActionTransferResourcesBetweenLevelObjects(
                                    PlayerManager.localPlayer,
                                    PlayerManager.localPlayer.brainRobot.toReference(),
                                    PlayerManager.localPlayer.brainRobot.inventory.id,
                                    container.attachedLevelObject!!.toReference(),
                                    container.id,
                                    resourceListOf(Mouse.heldItemType!! to quantity)
                                )
                            )
                        }
                    }
                }
            }
        }
        renderIcon = { x, y, index -> renderIconAt(x, y, index) }
        getToolTip = { index ->
            if (index < currentResources.size) {
                val entry = currentResources[index]
                val expectedOfType = container.expected[entry.key]
                "${entry.key} * ${entry.value}" + if (expectedOfType != 0) "(+$expectedOfType)" else ""
            } else {
                val expected = container.expected
                if (index - currentResources.size < expected.size) {
                    val (type, quantity) = expected[index - currentResources.size]
                    if (type !in currentResources.keys) {
                        "(+${type} * ${quantity})"
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

        }
        container.listeners.add(this)
    }

    private fun renderIconAt(x: Int, y: Int, index: Int) {
        if (index < currentResources.size) {
            val entry = currentResources[index]
            entry.key.icon.render(x, y, iconSize, iconSize, true)
            Renderer.renderText(entry.value, x, y)
            val width = TextManager.getStringWidth(entry.value.toString())
            val expectedOfType = container.expected[entry.key]
            if (expectedOfType != 0) {
                Renderer.renderText("(+$expectedOfType)", x + width, y, TextRenderParams(size = 13))
            }
        } else {
            val expected = container.expected
            if (index - currentResources.size < expected.size) {
                val (type, quantity) = expected[index - currentResources.size]
                if (type !in currentResources.keys) {
                    type.icon.render(x, y, iconSize, iconSize, true, TextureRenderParams(color = toColor(a = 0.6f)))
                    Renderer.renderText(quantity, x, y)
                }
            }
        }
    }

    override fun onContainerClear(container: ResourceContainer) {
        currentResources.clear()
    }

    override fun onAddToContainer(container: ResourceContainer, resources: ResourceList) {
        currentResources.putAll(resources)
    }

    override fun onRemoveFromContainer(container: ResourceContainer, resources: ResourceList) {
        currentResources.takeAll(resources)
    }
}