package graphics

import com.badlogic.gdx.graphics.g2d.TextureRegion
import data.ResourceManager
import main.heightPixels
import main.widthPixels

class ImageCollection(identifier: String, numberOfFrames: Int) {

    companion object {
        val GRASS_TILE = ImageCollection("tile/grass", 4)
        val GRASS_IRON_ORE_TILE = ImageCollection("tile/grass_iron_ore", 3)
        val GRASS_COPPER_ORE_TILE = ImageCollection("tile/grass_copper_ore", 4)
        val PLAYER = ImageCollection("robot/robot", 4)
        val TUBE_CORNER = ImageCollection("block/tube/corner", 4)
        val TUBE_3_WAY = ImageCollection("block/tube/3_way", 4)
        val PIPE_CORNER = ImageCollection("block/pipe/corner", 4)
        val PIPE_3_WAY = ImageCollection("block/pipe/3_way", 4)
    }

    val textures: Array<TextureRegion>
    var width: Int
    var height: Int

    operator fun get(i: Int): TextureRegion {
        return textures[i]
    }

    init {
        val list = mutableListOf<TextureRegion>()
        for(i in 1..numberOfFrames) {
            list.add(ResourceManager.getAtlasTexture(identifier, i))
        }
        width = list[0].widthPixels
        height = list[1].heightPixels
        textures = list.toTypedArray()
    }
}