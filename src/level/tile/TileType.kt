package level.tile

import graphics.ImageCollection
import graphics.Texture
import inv.ItemType

private var nextID = 0

open class OreTileType(textures: ImageCollection, name: String,
                       val maxAmount: Int,
                       val minAmount: Int,
                       val minedItem: ItemType,
                       val backgroundType: TileType,
                       val scatter: Int,
                       val generationChance: Double) : TileType(name, textures) {

    init {
        ALL.add(this)
    }

    companion object {
        val ALL = mutableListOf<OreTileType>()

        val GRASS_IRON_ORE = OreTileType(ImageCollection.GRASS_IRON_ORE_TILE, "Grass and iron ore",
                10,
                1,
                ItemType.IRON_ORE,
                TileType.GRASS,
                5,
                .1)

        val GRASS_COPPER_ORE = OreTileType(ImageCollection.GRASS_COPPER_ORE_TILE, "Grass and copper ore",
                10,
                1,
                ItemType.COPPER_ORE,
                TileType.GRASS,
                3,
                .07)
    }
}

open class TileType(val name: String, val textures: Array<Texture>) {

    constructor(name: String, textures: ImageCollection) : this(name, textures.textures)
    constructor(name: String, texture: Texture) : this(name, arrayOf(texture))

    val id = nextID++

    override fun equals(other: Any?): Boolean {
        if (other is TileType) {
            if (other.id == this.id)
                return true
        }
        return false
    }

    companion object {
        val GRASS = TileType("Grass", ImageCollection.GRASS_TILE)
    }

    override fun toString() = name

    override fun hashCode(): Int {
        return id
    }
}