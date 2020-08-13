package graphics

import com.badlogic.gdx.graphics.g2d.TextureRegion
import data.GameResourceManager
import main.heightPixels
import main.widthPixels
import serialization.Id
import serialization.Input
import serialization.Output
import serialization.Serializer

class Texture(
        val region: TextureRegion,
        @Id(2)
        override val xPixelOffset: Int = 0,
        @Id(3)
        override val yPixelOffset: Int = 0) : Renderable() {

    private constructor() : this(Image.Misc.ERROR, 0, 0)

    @Id(4)
    override val widthPixels = region.widthPixels

    @Id(5)
    override val heightPixels = region.heightPixels

    override fun render(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, keepAspect: Boolean, params: TextureRenderParams) {
        if (keepAspect) {
            Renderer.renderTextureKeepAspect(region, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels, params)
        } else {
            Renderer.renderTexture(region, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels, params)
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