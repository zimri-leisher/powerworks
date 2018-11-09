package graphics

import com.badlogic.gdx.graphics.g2d.TextureRegion
import main.heightPixels
import main.widthPixels

class Texture(val region: TextureRegion, override val xPixelOffset: Int = 0, override val yPixelOffset: Int = 0) : Renderable() {

    override val widthPixels = region.widthPixels
    override val heightPixels = region.heightPixels

    override fun render(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, keepAspect: Boolean, params: TextureRenderParams) {
        if (keepAspect) {
            Renderer.renderTextureKeepAspect(region, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels, params)
        } else {
            Renderer.renderTexture(region, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels, params)
        }
    }

}