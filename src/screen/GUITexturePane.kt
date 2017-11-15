package screen

import graphics.Renderer
import graphics.Renderer.params
import graphics.Texture

class GUITexturePane(parent: RootGUIElement,
                     name: String,
                     xAlignment: () -> Int, yAlignment: () -> Int,
                     val texture: Texture,
                     widthAlignment: () -> Int = { texture.widthPixels }, heightAlignment: () -> Int = { texture.heightPixels },
                     open: Boolean = false,
                     layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    constructor(parent: RootGUIElement,
                name: String,
                relXPixel: Int, relYPixel: Int,
                texture: Texture,
                widthPixels: Int = texture.widthPixels, heightPixels: Int = texture.heightPixels,
                open: Boolean = false,
                layer: Int = parent.layer + 1) :
            this(parent, name, { relXPixel }, { relYPixel }, texture, { widthPixels }, { heightPixels }, open, layer)

    override fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel, widthPixels, heightPixels, params)
    }
}