package screen.gui

import graphics.Renderer
import graphics.TextureRenderParams
import graphics.text.TextManager
import io.ControlBind
import io.ControlMap
import io.InputManager
import screen.ScreenLayer

object GuiSettings : Gui(ScreenLayer.MENU_4) {
    init {
        define {
            background {
                dimensions = Dimensions.Fullscreen
                list(horizontalAlign = HorizontalAlign.CENTER, placement = Placement.Align(HorizontalAlign.CENTER, VerticalAlign.TOP).offset(0, -6)) {
                    tabs(6) {
                        tab("Controls") {}
                        tab("Video") {}
                        tab("Audio") {}
                    }
                    // CONTROLS
                    background(TextureRenderParams(rotation = 180f, brightness = 0.6f)) {
                        dimensions = Dimensions.Fullscreen.pad(-50, -50)
                        open = true
                        clip(Placement.Origin, Dimensions.Fullscreen.pad(-50, -50)) {
                            list(Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(20, 0)) {
                                for (controlBind in ControlMap.DEFAULT.binds) {
                                    controlBind(controlBind)
                                }
                            }
                        }
                    }
                }
                button(
                    "Back",
                    { GuiSettings.open = false; GuiEscapeMenu.open = true },
                    placement = Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP),
                    padding = 8
                )
            }
        }
    }
}

class ElementControlBind(parent: GuiElement, var value: ControlBind) : GuiElement(parent) {

    init {
        dimensions = Dimensions.Exact(48, 16)
    }

    override fun render(params: TextureRenderParams?) {
        Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height, params ?: TextureRenderParams.DEFAULT)
        val displayString = InputManager.map.getControlString(value.result)
        val textDimensions = TextManager.getStringBounds(displayString)
        Renderer.renderText(
            displayString,
            absoluteX + (width - textDimensions.width) / 2,
            absoluteY + (height - textDimensions.height) / 2
        )
        super.render(params)
    }
}