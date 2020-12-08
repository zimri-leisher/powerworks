package screen.element

import graphics.Renderer
import graphics.TextureRenderParams
import io.Control
import io.ControlEventType
import screen.Interaction
import screen.gui.GuiElement

class ElementButton(parent: GuiElement, var render: Boolean, var toggle: Boolean,
                    var onPress: ElementButton.(interaction: Interaction) -> Unit = {},
                    var onRelease: ElementButton.(interaction: Interaction) -> Unit = {}) : GuiElement(parent) {

    var down = false

    override fun onInteractOn(interaction: Interaction) {
        if (interaction.event.control == Control.INTERACT) {
            if (!toggle) {
                if (interaction.event.type == ControlEventType.PRESS) {
                    down = true
                    onPress(interaction)
                } else if (interaction.event.type == ControlEventType.RELEASE && down) {
                    down = false
                    onRelease(interaction)
                }
            } else {
                if (down) {
                    if (interaction.event.type == ControlEventType.RELEASE) {
                        onRelease(interaction)
                    }
                } else {
                    if (interaction.event.type == ControlEventType.RELEASE) {
                        onPress(interaction)
                    }
                }

            }
        }
        super.onInteractOn(interaction)
    }

    override fun onMouseLeave() {
        if (!toggle) {
            down = false
        }
        super.onMouseLeave()
    }

    override fun render(params: TextureRenderParams?) {
        if(render) {
            if (params != null) {
                if (down) {
                    Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height, params.combine(TextureRenderParams(brightness = .9f, rotation = 180f)))
                } else if (mouseOn) {
                    Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height, params.combine(TextureRenderParams(brightness = 1.2f)))
                } else {
                    Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height, params)
                }
            } else {
                if (down) {
                    Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height, TextureRenderParams(brightness = .9f, rotation = 180f))
                } else if (mouseOn) {
                    Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height, TextureRenderParams(brightness = 1.2f))
                } else {
                    Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height)
                }
            }
        }
        super.render(params)
    }
}