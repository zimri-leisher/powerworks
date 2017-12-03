package graphics

import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class ImageCollection(path: String, numberOfImages: Int) {

    companion object {
        val CURSOR_RIGHT_CLICK = ImageCollection("/textures/cursor/cursor_right_click_anim.png", 8)
        val GRASS_TILE = ImageCollection("/textures/tile/grass.png", 4)
        val GRASS_IRON_ORE_TILE = ImageCollection("/textures/tile/grass_iron_ore.png", 3)
        val GRASS_COPPER_ORE_TILE = ImageCollection("/textures/tile/grass_copper_ore_temp.png", 1)
        val CONVEYOR_BELT_CONNECTED_LEFT = ImageCollection("/textures/block/conveyor_belt_connected_left_anim.png", 2)
        val CONVEYOR_BELT_CONNECTED_UP = ImageCollection("/textures/block/conveyor_belt_connected_up_anim.png", 4)
        val PLAYER = ImageCollection("/textures/player/player.png", 4)
        val TUBE_CORNER = ImageCollection("/textures/block/tube/corner.png", 4)
        val TUBE_3_WAY = ImageCollection("/textures/block/tube/3_way.png", 4)

    }

    val textures: Array<Texture>
    var width: Int
    var height: Int

    operator fun get(i: Int): Texture {
        return textures[i]
    }

    init {
        val image = ImageIO.read(ImageCollection::class.java.getResource(path))
        if (image.width % numberOfImages != 0)
            throw Exception("Image is not properly formatted")
        width = image.width / numberOfImages
        height = image.height
        val t = arrayOfNulls<Texture>(numberOfImages)
        val conf = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
        for (i in 0..numberOfImages - 1) {
            val sub = image.getSubimage(width * i, 0, width, image.height)
            val newImage = BufferedImage(sub.colorModel, sub.raster.createCompatibleWritableRaster(width, image.height), image.isAlphaPremultiplied, null)
            val dest = conf.createCompatibleImage(newImage.width, newImage.height, newImage.transparency)
            sub.copyData(newImage.raster)
            val g = dest.createGraphics()
            g.drawImage(newImage, 0, 0, null)
            g.dispose()
            t[i] = Image(dest)
        }
        textures = t.requireNoNulls()
    }
}