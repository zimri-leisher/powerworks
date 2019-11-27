package graphics

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import main.heightPixels
import main.widthPixels

class Texture(
        @Tag(1)
        val region: TextureRegion,
        @Tag(2)
        override val xPixelOffset: Int = 0,
        @Tag(3)
        override val yPixelOffset: Int = 0) : Renderable() {

    @Tag(4)
    override val widthPixels = region.widthPixels
    @Tag(5)
    override val heightPixels = region.heightPixels

    override fun render(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, keepAspect: Boolean, params: TextureRenderParams) {
        if (keepAspect) {
            Renderer.renderTextureKeepAspect(region, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels, params)
        } else {
            Renderer.renderTexture(region, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels, params)
        }
    }

}