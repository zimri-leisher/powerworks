package screen.gui

import graphics.Renderer
import graphics.TextureRenderParams
import io.ControlBind
import io.ControlMap
import screen.ScreenLayer

object GuiSettings : Gui(ScreenLayer.MENU_4) {
    init {
        define {
            background {
                dimensions = Dimensions.Fullscreen

                tabs(placement = Placement.Align(HorizontalAlign.CENTER, VerticalAlign.TOP)) {
                    tab("Controls") {}
                    tab("Video") {}
                    tab("Audio") {}
                }
                list(Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(20, 0)) {
                    for (i in 0..30) {
                        controlBind(ControlMap.DEFAULT.binds.first())
                    }
                }
                button("Back", { GuiSettings.open = false; GuiEscapeMenu.open = true }, placement = Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP), padding = 8)
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
        Renderer.renderText("test", absoluteX + width / 2, absoluteY + height / 2)
        super.render(params)
    }
}