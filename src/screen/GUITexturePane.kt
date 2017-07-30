package screen

import graphics.Renderer
import graphics.Texture

class GUITexturePane(parent: RootGUIElement? = RootGUIElementObject,
                     name: String,
                     relXPixel: Int, relYPixel: Int,
                     val texture: Texture,
                     widthPixels: Int = texture.widthPixels, heightPixels: Int = texture.heightPixels,
                     layer: Int = (parent?.layer ?: 0) + 1) :
        GUIElement(parent, name, relXPixel, relYPixel, widthPixels, heightPixels, layer) {

    override fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel, widthPixels, heightPixels, params)
    }
}