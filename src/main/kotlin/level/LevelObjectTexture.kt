package level

import com.badlogic.gdx.graphics.g2d.TextureRegion
import graphics.*

class LevelObjectTextures(vararg val textures: Renderable) {

    constructor(vararg textures: TextureRegion) : this(*textures.map { Texture(it) }.toTypedArray())
    constructor(col: ImageCollection) : this(*col.textures)

    fun render(l: LevelObject, params: TextureRenderParams = TextureRenderParams.DEFAULT) {
        val texture = textures[Math.min(l.rotation, textures.lastIndex)]
        texture.render(l.xPixel, l.yPixel, params = params)
    }
}