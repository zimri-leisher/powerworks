package level.tile

import com.badlogic.gdx.graphics.g2d.TextureRegion
import graphics.ImageCollection
import item.ItemType
import item.OreItemType

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
                9000,
                3000,
                OreItemType.IRON_ORE,
                TileType.GRASS,
                5,
                .1)

        val GRASS_COPPER_ORE = OreTileType(ImageCollection.GRASS_COPPER_ORE_TILE, "Grass and copper ore",
                5000,
                3000,
                OreItemType.COPPER_ORE,
                TileType.GRASS,
                3,
                .07)
    }
}

open class TileType(val name: String, val textures: Array<TextureRegion>) {

    constructor(name: String, textures: ImageCollection) : this(name, textures.textures)
    constructor(name: String, texture: TextureRegion) : this(name, arrayOf(texture))

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