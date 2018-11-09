package screen.elements

import com.badlogic.gdx.graphics.g2d.TextureRegion
import graphics.Renderable
import graphics.Texture
import main.heightPixels
import main.widthPixels

class GUITexturePane(parent: RootGUIElement,
                     name: String,
                     xAlignment: Alignment, yAlignment: Alignment,
                     renderable: Renderable,
                     widthAlignment: Alignment = { renderable.widthPixels }, heightAlignment: Alignment = { renderable.heightPixels },
                     open: Boolean = false,
                     layer: Int = parent.layer + 1,
                     var keepAspect: Boolean = false) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    var updateDimensionAlignmentOnTextureChange = true

    var renderable = renderable
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
                renderable: Renderable,
                widthPixels: Int = renderable.widthPixels, heightPixels: Int = renderable.heightPixels,
                open: Boolean = false,
                layer: Int = parent.layer + 1,
                keepAspect: Boolean = false) :
            this(parent, name, { relXPixel }, { relYPixel }, renderable, { widthPixels }, { heightPixels }, open, layer, keepAspect)

    constructor(parent: RootGUIElement,
                name: String,
                relXPixel: Int, relYPixel: Int,
                textureRegion: TextureRegion,
                widthPixels: Int = textureRegion.widthPixels, heightPixels: Int = textureRegion.heightPixels,
                open: Boolean = false,
                layer: Int = parent.layer + 1,
                keepAspect: Boolean = false) :
            this(parent, name, { relXPixel }, { relYPixel }, Texture(textureRegion), { widthPixels }, { heightPixels }, open, layer, keepAspect)

    constructor(parent: RootGUIElement,
                name: String,
                xAlignment: Alignment, yAlignment: Alignment,
                textureRegion: TextureRegion,
                widthAlignment: Alignment = { textureRegion.widthPixels }, heightAlignment: Alignment = { textureRegion.heightPixels },
                open: Boolean = false,
                layer: Int = parent.layer + 1,
                keepAspect: Boolean = false) : this(parent, name, xAlignment, yAlignment, Texture(textureRegion), widthAlignment, heightAlignment, open, layer, keepAspect)

    override fun render() {
        renderable.render(xPixel, yPixel, widthPixels, heightPixels, keepAspect, localRenderParams)
    }
}