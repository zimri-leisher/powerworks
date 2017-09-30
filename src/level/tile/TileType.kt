package level.tile

import graphics.ImageCollection
import graphics.Texture

private var nextID = 0

sealed class OreTileType(val parentType: TileType, val maxAmount: Int, val minAmount: Int) : TileType(parentType.textures) {
    object GRASS_IRON_ORE : OreTileType(TileType.GRASS_IRON_ORE, 10, 1)
}

sealed class TileType(val textures: Array<Texture>) {

    object GRASS : TileType(ImageCollection.GRASS_TILE)
    object GRASS_IRON_ORE : TileType(ImageCollection.GRASS_IRON_ORE_TILE)

    constructor(textures: ImageCollection): this(textures.textures)
    constructor(texture: Texture): this(arrayOf(texture))

    val id = nextID++

    override fun equals(other: Any?): Boolean {
        if(other is TileType) {
            if(other.id == this.id)
                return true
        }
        return false
    }
}