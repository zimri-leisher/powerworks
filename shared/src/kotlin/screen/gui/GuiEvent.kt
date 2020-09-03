package screen.gui

import graphics.TextureRenderParams
import screen.Interaction

enum class GuiEvent {
    INTERACT_ON, DESELECT,
    MOUSE_ENTER, MOUSE_LEAVE,
    OPEN, CLOSE,
    UPDATE,
    CHANGE_DIMENSION,
    CHANGE_PLACEMENT,
    RENDER
}

sealed class GuiEventListener(val type: GuiEvent)

class GuiInteractOnListener(val handle: GuiElement.(Interaction) -> Unit) : GuiEventListener(GuiEvent.INTERACT_ON)
class GuiDeselectListener(val handle: GuiElement.() -> Unit) : GuiEventListener(GuiEvent.DESELECT)
class GuiMouseEnterListener(val handle: GuiElement.() -> Unit) : GuiEventListener(GuiEvent.MOUSE_ENTER)
class GuiMouseLeaveListener(val handle: GuiElement.() -> Unit) : GuiEventListener(GuiEvent.MOUSE_LEAVE)
class GuiOpenListener(val handle: GuiElement.() -> Unit) : GuiEventListener(GuiEvent.OPEN)
class GuiCloseListener(val handle: GuiElement.() -> Unit) : GuiEventListener(GuiEvent.CLOSE)
class GuiUpdateListener(val handle: GuiElement.() -> Unit) : GuiEventListener(GuiEvent.UPDATE)
class GuiChangeDimensionListener(val handle: GuiElement.() -> Unit) : GuiEventListener(GuiEvent.CHANGE_DIMENSION)
class GuiChangePlacementListener(val handle: GuiElement.() -> Unit) : GuiEventListener(GuiEvent.CHANGE_PLACEMENT)
class GuiRenderListener(val handle: GuiElement.(x: Int, y: Int, TextureRenderParams?) -> Unit) : GuiEventListener(GuiEvent.RENDER)


