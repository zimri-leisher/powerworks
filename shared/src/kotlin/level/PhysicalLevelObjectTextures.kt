package level

import com.badlogic.gdx.graphics.g2d.TextureRegion
import graphics.*
import serialization.Id

class PhysicalLevelObjectTextures(
        @Id(1)
        vararg val textures: Renderable) {

    constructor(vararg textures: TextureRegion) : this(*textures.map { Texture(it) }.toTypedArray())
    constructor(col: ImageCollection) : this(*col.textures)
    private constructor() : this(Image.Misc.ERROR)

    fun render(l: PhysicalLevelObject, params: TextureRenderParams = TextureRenderParams.DEFAULT) = render(l.x, l.y, l.rotation, params)

    fun render(x: Int, y: Int, rotation: Int, params: TextureRenderParams = TextureRenderParams.DEFAULT) {
        val texture = textures[Math.min(rotation, textures.lastIndex)]
        texture.render(x, y, params = params)
    }

    operator fun get(i: Int) = textures[i]
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PhysicalLevelObjectTextures

        if (!textures.contentEquals(other.textures)) return false

        return true
    }

    override fun hashCode(): Int {
        return textures.contentHashCode()
    }
}