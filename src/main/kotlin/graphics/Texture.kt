package graphics

import java.awt.image.BufferedImage

interface Texture {
    val currentImage: BufferedImage
    val widthPixels: Int
    val heightPixels: Int
}