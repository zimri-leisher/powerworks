package screen.element

import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import io.Control
import io.ControlEventType
import main.height
import main.width
import screen.gui.Dimensions
import screen.gui.GuiElement
import screen.Interaction

class ElementCloseButton(parent: GuiElement) : GuiElement(parent) {

    init {
        dimensions = Dimensions.Exact(Image.Gui.CLOSE_BUTTON.width, Image.Gui.CLOSE_BUTTON.height)
    }

    override fun onInteractOn(interaction: Interaction) {
        if (interaction.event.control == Control.INTERACT && interaction.event.type == ControlEventType.PRESS) {
            gui.open = false
        }
        super.onInteractOn(interaction)
    }

    override fun render(params: TextureRenderParams?) {
        Renderer.renderTexture(if (mouseOn) Image.Gui.CLOSE_BUTTON_HIGHLIGHT else Image.Gui.CLOSE_BUTTON, absoluteX, absoluteY)
        super.render(params)
    }
}