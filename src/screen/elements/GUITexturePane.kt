package screen.elements

import graphics.Renderer
import graphics.Renderer.params
import graphics.Texture

class GUITexturePane(parent: RootGUIElement,
                     name: String,
                     xAlignment: () -> Int, yAlignment: () -> Int,
                     texture: Texture,
                     widthAlignment: () -> Int = { texture.widthPixels }, heightAlignment: () -> Int = { texture.heightPixels },
                     open: Boolean = false,
                     layer: Int = parent.layer + 1,
                     var keepAspect: Boolean = false) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {
    var updateDimensionAlignmentOnTextureChange = true


    var texture = texture
        set(value) {
            if (field != value) {
                if (widthPixels == field.widthPixels && heightPixels == field.heightPixels && updateDimensionAlignmentOnTextureChange) {
                    widthAlignment = { value.widthPixels }
                    heightAlignment = { value.heightPixels }
                }
                field = value
            }
        }

    constructor(parent: RootGUIElement,
                name: String,
                relXPixel: Int, relYPixel: Int,
                texture: Texture,
                widthPixels: Int = texture.widthPixels, heightPixels: Int = texture.heightPixels,
                open: Boolean = false,
                layer: Int = parent.layer + 1,
                keepAspect: Boolean = false) :
            this(parent, name, { relXPixel }, { relYPixel }, texture, { widthPixels }, { heightPixels }, open, layer, keepAspect)

    override fun render() {
        if (keepAspect)
            Renderer.renderTextureKeepAspect(texture, xPixel, yPixel, widthPixels, heightPixels)
        else
            Renderer.renderTexture(texture, xPixel, yPixel, widthPixels, heightPixels, params)
    }
}