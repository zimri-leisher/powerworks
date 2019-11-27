package data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import java.io.InputStream
import java.net.URL

class ResourceNotFoundException(message: String) : Exception(message)

object ResourceManager {

    private val images = mutableMapOf<String, Texture>()

    private val libgdxTextureManager = AssetManager(PowerworksTextureFileHandleResolver())
    private val libgdxSoundManager = AssetManager(PowerworksSoundFileHandleResolver())

    private val textureAtlases = mutableListOf<TextureAtlas>()
    private val textureRegions = mutableMapOf<String, TextureRegion>()

    private val defaultTextureFile: FileHandle = Gdx.files.internal("textures/misc/error.png")

    /**
     * Loads a [TextureAtlas] and adds it to the internal list
     */
    fun registerAtlas(path: String) = TextureAtlas(path).apply { textureAtlases.add(this) }

    /**
     * Gets an identifier from a [TextureRegion]
     */
    fun getIdentifier(textureRegion: TextureRegion) =
            textureRegions.filter { it.value == textureRegion }.values.firstOrNull()

    /**
     * Gets a texture from one of the loaded [TextureAtlas]
     * @param index the index of the texture (the number after the last underscore if there is one). -1 if no index
     */
    fun getAtlasTexture(identifier: String, index: Int = -1): TextureRegion {
        val fullId = identifier + if (index != -1) "_$index" else ""
        val r = textureRegions[fullId]
        if (r != null)
            return r
        var region: TextureRegion? = null
        for (atlas in textureAtlases) {
            region = if (index == -1) atlas.findRegion(identifier) else atlas.findRegion(identifier, index)
            if (region != null)
                break
        }
        if (region == null) {
            throw ResourceNotFoundException("Resource with identifier $identifier ${if (index != -1) "and index $index" else ""} not found in an atlas")
        }
        textureRegions.put(fullId, region)
        return region
    }

    /**
     * Adds a texture to the internal map
     * @param identifier the identifier to store it under
     * @param path the path to send to the [PowerworksTextureFileHandleResolver]. This path need not be exact, as the
     * resolver will check multiple possibilities
     * @param asynchronous whether to block until the texture is loaded or return a texture which will have its
     * texture data filled when it is loaded. The temporary texture will be the [graphics.Image.Misc.ERROR] texture
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
     * Adds a [Texture] to the internal map
     * @param identifier the identifier to store it under
     * @param texture the texture to store
     * @return the [texture] parameter (for chaining)
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
     * @return a [Texture] that matches the identifier.
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
     * @param path the path relative to the compiled jar. If this is called in a [Mod], it will be relative to the compiled
     * jar of the mod.
     * @return an absolute [URL] to the path
     */
    fun getRawResource(path: String): URL {
        return try {
            ResourceManager.javaClass.getResource(path)!!
        } catch (e: KotlinNullPointerException) {
            throw ResourceNotFoundException("Resource at $path not found")
        }
    }

    /**
     * @param path the path relative to the compiled jar. If this is called in a [Mod], it will be relative to the compiled
     * jar of the mod.
     * @return an [InputStream] to the resource
     */
    fun getRawResourceAsStream(path: String): InputStream {
        return try {
            ResourceManager.javaClass.getResourceAsStream(path)!!
        } catch (e: KotlinNullPointerException) {
            throw ResourceNotFoundException("Resource at $path not found")
        }
    }

    /**
     * Disposes of resources loaded with this
     */
    fun dispose() {
        textureAtlases.forEach { it.dispose() }
        libgdxSoundManager.dispose()
        libgdxTextureManager.dispose()
    }
}