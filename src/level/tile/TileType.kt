package level.tile

import graphics.ImageCollection
import graphics.Texture
import java.util.*

private var nextID = 0

data class TileType(val textures: Array<Texture>) {

    val id = nextID++

    constructor(textures: ImageCollection): this(textures.textures)
    constructor(texture: Texture): this(arrayOf(texture))

    override fun equals(other: Any?): Boolean {
        if(other is TileType) {
            if(other.id == this.id)
                return true
        }
        return false
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(textures)
    }
}