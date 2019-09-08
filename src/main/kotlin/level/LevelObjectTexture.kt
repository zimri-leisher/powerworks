package level

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import graphics.ImageCollection
import graphics.Renderable
import graphics.Texture
import graphics.TextureRenderParams

class LevelObjectTextures(
        @Tag(1)
        vararg val textures: Renderable) {

    constructor(vararg textures: TextureRegion) : this(*textures.map { Texture(it) }.toTypedArray())
    constructor(col: ImageCollection) : this(*col.textures)

    fun render(l: LevelObject, params: TextureRenderParams = TextureRenderParams.DEFAULT) = render(l.xPixel, l.yPixel, l.rotation, params)

    fun render(xPixel: Int, yPixel: Int, rotation: Int, params: TextureRenderParams = TextureRenderParams.DEFAULT) {
        val texture = textures[Math.min(rotation, textures.lastIndex)]
        texture.render(xPixel, yPixel, params = params)
    }

    operator fun get(i: Int) = textures[i]
}