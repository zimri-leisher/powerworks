package screen

import graphics.Renderer
import graphics.Renderer.params
import graphics.Texture

class GUITexturePane(parent: RootGUIElement,
                     name: String,
                     relXPixel: Int, relYPixel: Int,
                     val texture: Texture,
                     widthPixels: Int = texture.widthPixels, heightPixels: Int = texture.heightPixels,
                     open: Boolean = false,
                     layer: Int = parent.layer + 1) :
        GUIElement(parent, name, relXPixel, relYPixel, widthPixels, heightPixels, open, layer) {

    override fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel, widthPixels, heightPixels, params)
    }
}