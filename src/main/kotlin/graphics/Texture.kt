package graphics

import java.awt.image.BufferedImage

interface Texture {
    var currentImage: BufferedImage
    val widthPixels: Int
    val heightPixels: Int
}