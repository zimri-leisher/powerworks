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
import player.ActionDoResourceTransaction
import player.PlayerManager
import resource.*
import screen.Interaction
import screen.ScreenManager
import screen.gui.GuiElement
import screen.gui.GuiIngame
import screen.mouse.Mouse
import kotlin.math.exp

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
                        if (other != null) {
                            PlayerManager.takeAction(
                                ActionDoResourceTransaction(
                                    PlayerManager.localPlayer,
                                    ResourceTransaction(this.container, other, stackOf(type, quantity))
                                )
                            )
                        }
                    } else if (interaction.event.control == Control.SECONDARY_INTERACT && type is ItemType) {
                        GuiIngame.Hotbar.addItemType(type)
                    }
                } else {
                    if (interaction.event.control == Control.INTERACT && Mouse.heldItemType != null) {
                        val quantity = PlayerManager.localPlayer.brainRobot.inventory.getQuantity(Mouse.heldItemType!!)
                        if (quantity > 0) {
                            PlayerManager.takeAction(
                                ActionDoResourceTransaction(
                                    PlayerManager.localPlayer,
                                    ResourceTransaction(
                                        PlayerManager.localPlayer.brainRobot.inventory,
                                        container,
                                        stackOf(Mouse.heldItemType!!, quantity)
                                    )
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
                val expectedOfType = container.getFlowInProgressForType(entry.type)
                "${entry.key} * ${entry.value}" + if (expectedOfType != 0) "(+$expectedOfType)" else ""
            } else {
                val expected =
                    container.getFlowInProgress().filter { it.direction == ResourceFlowDirection.IN }.map { it.stack }
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
            Renderer.renderText(entry.quantity, x, y)
            val width = TextManager.getStringWidth(entry.value.toString())
            // TODO fix this code duplication...
            val expectedOfType = container.getFlowInProgressForType(entry.type)
            if (expectedOfType != 0) {
                Renderer.renderText("(+$expectedOfType)", x + width, y, TextRenderParams(size = 13))
            }
        } else {
            val expected =
                container.getFlowInProgress().filter { it.direction == ResourceFlowDirection.IN }.map { it.stack }
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