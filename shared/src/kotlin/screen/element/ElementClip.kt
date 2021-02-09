package screen.element

import graphics.Renderer
import graphics.TextureRenderParams
import screen.gui.GuiElement

class ElementClip(parent: GuiElement) : GuiElement(parent){
    override fun render(params: TextureRenderParams?) {
        Renderer.pushClip(absoluteX, absoluteY, width, height)
        super.render(params)
        Renderer.popClip()
    }
}