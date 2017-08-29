package level.tile

import graphics.ImageCollection
import graphics.ImageCollections
import graphics.Texture
import java.util.*

private var nextID = 0

object OreTileTypes {
    val GRASS_IRON_ORE = OreTileType(ImageCollections.GRASS_IRON_ORE_TILE, 10)
}

object TileTypes {
    val GRASS = TileType(ImageCollections.GRASS_TILE)
}

open class TileType constructor(val textures: Array<Texture>) {

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
}