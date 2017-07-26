package screen

import graphics.Renderer
import graphics.Texture

class GUITexturePane(parent: RootGUIElement, relXPixel: Int, relYPixel: Int, val texture: Texture, widthPixels: Int = texture.widthPixels, heightPixels:Int = texture.heightPixels) : GUIElement(parent, relXPixel, relYPixel, widthPixels, heightPixels) {
    override fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel, widthPixels, heightPixels)
    }
}