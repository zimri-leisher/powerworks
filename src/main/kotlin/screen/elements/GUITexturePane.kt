package screen.elements

import com.badlogic.gdx.graphics.g2d.TextureRegion
import graphics.Renderer
import main.heightPixels
import main.widthPixels

class GUITexturePane(parent: RootGUIElement,
                     name: String,
                     xAlignment: Alignment, yAlignment: Alignment,
                     texture: TextureRegion,
                     widthAlignment: Alignment = { texture.widthPixels }, heightAlignment: Alignment = { texture.heightPixels },
                     open: Boolean = false,
                     layer: Int = parent.layer + 1,
                     var keepAspect: Boolean = false) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {
    var updateDimensionAlignmentOnTextureChange = true


    var texture = texture
        set(value) {
            if (field != value) {
                if (widthPixels == field.widthPixels && heightPixels == field.heightPixels && updateDimensionAlignmentOnTextureChange) {
                    alignments.width = { value.widthPixels }
                    alignments.height = { value.heightPixels }
                }
                field = value
            }
        }

    constructor(parent: RootGUIElement,
                name: String,
                relXPixel: Int, relYPixel: Int,
                texture: TextureRegion,
                widthPixels: Int = texture.widthPixels, heightPixels: Int = texture.heightPixels,
                open: Boolean = false,
                layer: Int = parent.layer + 1,
                keepAspect: Boolean = false) :
            this(parent, name, { relXPixel }, { relYPixel }, texture, { widthPixels }, { heightPixels }, open, layer, keepAspect)

    override fun render() {
        if (keepAspect)
            Renderer.renderTextureKeepAspect(texture, xPixel, yPixel, widthPixels, heightPixels, localRenderParams)
        else
            Renderer.renderTexture(texture, xPixel, yPixel, widthPixels, heightPixels, localRenderParams)
    }
}