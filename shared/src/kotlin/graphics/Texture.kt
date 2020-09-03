package graphics

import com.badlogic.gdx.graphics.g2d.TextureRegion
import data.GameResourceManager
import main.height
import main.width
import serialization.Id
import serialization.Input
import serialization.Output
import serialization.Serializer

class Texture(
        val region: TextureRegion,
        @Id(2)
        override val xOffset: Int = 0,
        @Id(3)
        override val yOffset: Int = 0) : Renderable() {

    private constructor() : this(Image.Misc.ERROR, 0, 0)

    @Id(4)
    override val width = region.width

    @Id(5)
    override val height = region.height

    override fun render(x: Int, y: Int, width: Int, height: Int, keepAspect: Boolean, params: TextureRenderParams) {
        if (keepAspect) {
            Renderer.renderTextureKeepAspect(region, x + xOffset, y + yOffset, width, height, params)
        } else {
            Renderer.renderTexture(region, x + xOffset, y + yOffset, width, height, params)
        }
    }
}

class TextureSerializer : Serializer.Tagged<Texture>() {

    override fun write(obj: Any, output: Output) {
        obj as Texture
        output.writeUTF(GameResourceManager.getIdentifier(obj.region)!!)
        super.write(obj, output)
    }

    override fun instantiate(input: Input): Texture {
        return Texture(GameResourceManager.getAtlasTexture(input.readUTF()))
    }
}