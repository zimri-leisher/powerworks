package data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import mod.Mod
import mod.ModManager
import java.io.File
import java.io.InputStream
import java.net.URL

class ResourceNotFoundException(message: String) : Exception(message)

object ResourceManager {

    private val images = mutableMapOf<String, Texture>()

    val libgdxTextureManager = AssetManager(PowerworksTextureFileHandleResolver())
    val libgdxSoundManager = AssetManager(PowerworksSoundFileHandleResolver())

    val textureAtlases = mutableListOf<TextureAtlas>()

    val defaultTextureFile: FileHandle = Gdx.files.internal("textures/misc/error.png")

    var currentModContext: Mod? = null

    /**
     * Loads a texture atlas and adds it to the internal list
     */
    fun registerAtlas(path: String) = TextureAtlas(path).apply { textureAtlases.add(this) }

    /**
     * Gets a texture from one of the loaded texture atlases
     */
    fun getTextureFromAtlas(identifier: String): TextureRegion {
        var region: TextureRegion? = null
        for(atlas in textureAtlases) {
            region = atlas.findRegion(identifier)
            if(region != null)
                break
        }
        if(region == null) {
            throw ResourceNotFoundException("Resource with identifier $identifier not found in an atlas")
        }
        return region
    }

    /**
     * Adds a texture to the internal map
     * @param identifier the identifier to store it under
     * @param path the path to send to the PowerworksTextureFileHandlerResolver. This path may not be exact, as the
     * resolver will check multiple possibilities
     * @param asynchronous whether to block until the texture is loaded or return a texture which will have its
     * TextureData filled when it is loaded. The temporary texture will be the misc/error.png texture
     */
    fun registerTexture(identifier: String, path: String = identifier, asynchronous: Boolean = true): Texture {
        if (identifier in images) {
            return images[identifier]!!
        }
        if (asynchronous) {
            val ret = Texture(defaultTextureFile)
            libgdxTextureManager.load(path, Texture::class.java, TextureLoader.TextureParameter().apply {
                texture = ret
            })
            images.put(identifier, ret)
            return ret
        } else {
            libgdxTextureManager.load(path, Texture::class.java)
            libgdxTextureManager.finishLoadingAsset(path)
            return libgdxTextureManager.get(path)
        }
    }

    /**
     * Adds a texture to the internal map
     * @param identifier the identifier to store it under
     * @param texture the texture to store
     * @return the texture - this is handy for loading it by saying i: Image = registerTexture("some_identifier", Utils.modify(...))
     */
    fun registerTexture(identifier: String, texture: Texture): Texture {
        if (identifier in images) {
            throw IllegalArgumentException("Texture under identifier $identifier already present")
        }
        images.put(identifier, texture)
        return texture
    }

    fun update() {
        libgdxTextureManager.update()
        libgdxSoundManager.update()
    }

    /**
     * @return a texture that matches the identifier.
     * @throws ResourceNotFoundException if there is no matching
     */
    fun getTexture(identifier: String): Texture {
        try {
            return images[identifier]!!
        } catch (e: KotlinNullPointerException) {
            throw ResourceNotFoundException("Resource with identifier $identifier not found")
        }
    }

    /**
     * @param path the path relative to the compiled jar. If this is called in a mod, it will be relative to the compiled
     * jar of the mod.
     * @return an absolute URL to the path
     */
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

    /**
     * @param path the path relative to the compiled jar. If this is called in a mod, it will be relative to the compiled
     * jar of the mod.
     * @return an InputStream to the resource
     */
    fun getRawResourceAsStream(path: String): InputStream {
        if (currentModContext != null)
            return try {
                ModManager.getMainClass(currentModContext!!).getResourceAsStream(path)
            } catch (e: KotlinNullPointerException) {
                throw ResourceNotFoundException("Resource at $path not found for current mod $currentModContext")
            }
        else
            return try {
                ResourceManager.javaClass.getResourceAsStream(path)!!
            } catch (e: KotlinNullPointerException) {
                throw ResourceNotFoundException("Resource at $path not found")
            }
    }

    fun dispose() {
        textureAtlases.forEach { it.dispose() }
    }
}