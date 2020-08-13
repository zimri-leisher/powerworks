package graphics

import com.badlogic.gdx.graphics.g2d.TextureRegion
import data.GameResourceManager
import serialization.Id

class ImageCollection(
        @Id(1)
        val identifier: String,
        numberOfFrames: Int) {

    private constructor() : this("misc/error", 1)

    @Id(2)
    val textures: Array<TextureRegion>

    operator fun get(i: Int): TextureRegion {
        return textures[i]
    }

    init {
        ALL.add(this)
        val list = mutableListOf<TextureRegion>()
        for (i in 1..numberOfFrames) {
            list.add(GameResourceManager.getAtlasTexture(identifier, i))
        }
        textures = list.toTypedArray()
    }

    companion object {

        val ALL = mutableListOf<ImageCollection>()

        val GRASS_TILE = ImageCollection("tile/grass", 4)
        val ROCK_TILE = ImageCollection("tile/rock", 8)
        val ROCK_COPPER_ORE_TILE = ImageCollection("tile/rock_copper_ore", 18)
        val ROCK_IRON_ORE_TILE = ImageCollection("tile/rock_iron_ore", 19)
        val GRASS_IRON_ORE_TILE = ImageCollection("tile/grass_iron_ore", 3)
        val GRASS_COPPER_ORE_TILE = ImageCollection("tile/grass_copper_ore", 4)
        val ROBOT = ImageCollection("robot/robot", 4)
        val TUBE_CORNER = ImageCollection("block/tube/corner", 4)
        val TUBE_3_WAY = ImageCollection("block/tube/3_way", 4)
        val PIPE_CORNER = ImageCollection("block/pipe/corner", 4)
        val PIPE_3_WAY = ImageCollection("block/pipe/3_way", 4)
    }
}
