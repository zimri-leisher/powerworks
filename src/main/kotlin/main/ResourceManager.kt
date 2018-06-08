package main

import graphics.Image
import mod.Mod
import mod.ModManager
import java.awt.AlphaComposite
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.InputStream
import java.net.URL
import javax.imageio.ImageIO

class ResourceNotFoundException(message: String) : Exception(message)

object ResourceManager {

    private val images = mutableMapOf<String, Image>()

    var currentModContext: Mod? = null

    fun registerImage(identifier: String, path: String = identifier): Image {
        if(identifier in images)
            return images[identifier]!!
        val possibilities = mutableListOf<String>()
        possibilities.add("/textures/$path.png")
        possibilities.add("$path.png")
        possibilities.add(path)
        var url: URL? = null
        for (possibility in possibilities) {
            val preExisting = images[possibility]
            if(preExisting != null)
                return preExisting
            try {
                url = ResourceManager.getRawResource(possibility)
                break
            } catch (e: ResourceNotFoundException) {
                continue
            }
        }
        if (url == null)
            throw ResourceNotFoundException("Resource with identifier $identifier and path $path not found")
        val src = ImageIO.read(url)
        val dest = Game.graphicsConfiguration.createCompatibleImage(src.width, src.height, Transparency.TRANSLUCENT)
        val g2d = dest.createGraphics()
        g2d.composite = AlphaComposite.SrcOver
        g2d.run {
            drawImage(src, 0, 0, null)
            dispose()
        }
        return registerImage(identifier, dest)
    }

    fun registerImage(identifier: String, image: BufferedImage): Image {
        if(identifier in images)
            return images[identifier]!!
        val img = Image(image)
        images.put(identifier, img)
        return Image(image)
    }

    fun getImage(identifier: String): Image {
        try {
            return images[identifier]!!
        } catch (e: KotlinNullPointerException) {
            throw ResourceNotFoundException("Resource with identifier $identifier not found")
        }
    }

    fun getRawResource(path: String): URL {
        if (currentModContext != null) {
            return try {
                ModManager.getMainClass(currentModContext!!).getResource(path)!!
            } catch (e: KotlinNullPointerException) {
                throw ResourceNotFoundException("Resource at $path not found for current mod $currentModContext")
            }
        } else {
            return try {
                ResourceManager.javaClass.getResource(path)!!
            } catch (e: KotlinNullPointerException) {
                throw ResourceNotFoundException("Resource at $path not found")
            }
        }
    }

    fun getRawResourceAsStream(path: String): InputStream {
        if (currentModContext != null)
            return try {
                ModManager.getMainClass(currentModContext!!).getResourceAsStream(path)
            } catch(e: KotlinNullPointerException) {
                throw ResourceNotFoundException("Resource at $path not found for current mod $currentModContext")
            }
        else
            return try {
                ResourceManager.javaClass.getResourceAsStream(path)!!
            } catch(e: KotlinNullPointerException) {
                throw ResourceNotFoundException("Resource at $path not found")
            }
    }
}