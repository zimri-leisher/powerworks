package screen.gui2

import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import io.Control
import io.ControlEventType
import main.heightPixels
import main.widthPixels

class ElementCloseButton(parent: GuiElement) : GuiElement(parent) {

    init {
        dimensions = Dimensions.Exact(Image.GUI.CLOSE_BUTTON.widthPixels, Image.GUI.CLOSE_BUTTON.heightPixels)
    }

    override fun onInteractOn(interaction: Interaction) {
        if(interaction.event.control == Control.INTERACT && interaction.event.type == ControlEventType.PRESS) {
            gui.open = false
        }
        super.onInteractOn(interaction)
    }

    override fun render(params: TextureRenderParams?) {
        Renderer.renderTexture(Image.GUI.CLOSE_BUTTON, absoluteXPixel, absoluteYPixel)
        super.render(params)
    }
}