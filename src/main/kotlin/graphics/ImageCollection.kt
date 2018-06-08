package graphics

import main.ResourceManager
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class ImageCollection(path: String, numberOfImages: Int) {

    companion object {
        val CURSOR_RIGHT_CLICK = ImageCollection("cursor/cursor_right_click_anim", 8)
        val GRASS_TILE = ImageCollection("tile/grass", 4)
        val GRASS_IRON_ORE_TILE = ImageCollection("tile/grass_iron_ore", 3)
        val GRASS_COPPER_ORE_TILE = ImageCollection("tile/grass_copper_ore", 4)
        val CONVEYOR_BELT_CONNECTED_LEFT = ImageCollection("block/conveyor_belt_connected_left_anim", 2)
        val CONVEYOR_BELT_CONNECTED_UP = ImageCollection("block/conveyor_belt_connected_up_anim", 4)
        val PLAYER = ImageCollection("player/player", 4)
        val TUBE_CORNER = ImageCollection("block/tube/corner", 4)
        val TUBE_3_WAY = ImageCollection("block/tube/3_way", 4)
        val PIPE_CORNER = ImageCollection("block/pipe/corner", 4)
        val PIPE_3_WAY = ImageCollection("block/pipe/3_way", 4)
        val MINER = ImageCollection("block/miner/miner", 2)
    }

    val textures: Array<Texture>
    var width: Int
    var height: Int

    operator fun get(i: Int): Texture {
        return textures[i]
    }

    init {
        val image = ResourceManager.registerImage(path).currentImage
        if (image.width % numberOfImages != 0)
            throw Exception("Image is not properly formatted")
        width = image.width / numberOfImages
        height = image.height
        val t = arrayOfNulls<Texture>(numberOfImages)
        val conf = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
        for (i in 0 until numberOfImages) {
            val sub = image.getSubimage(width * i, 0, width, image.height)
            val newImage = BufferedImage(sub.colorModel, sub.raster.createCompatibleWritableRaster(width, image.height), image.isAlphaPremultiplied, null)
            val dest = conf.createCompatibleImage(newImage.width, newImage.height, newImage.transparency)
            sub.copyData(newImage.raster)
            val g = dest.createGraphics()
            g.drawImage(newImage, 0, 0, null)
            g.dispose()
            t[i] = ResourceManager.registerImage("${path}_$i", dest)
        }
        textures = t.requireNoNulls()
    }
}