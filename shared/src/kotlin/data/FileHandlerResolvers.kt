package data

import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import java.net.URL

abstract class PowerworksFileHandleResolver : FileHandleResolver {

    override fun resolve(fileName: String?): FileHandle {
        if (fileName == null) throw IllegalArgumentException("fileName cannot be null")
        val possibilities = getPossibilities(fileName)
        var url: URL? = null
        for (possibility in possibilities) {
            try {
                url = ResourceManager.getRawResource(possibility)
                break
            } catch (e: ResourceNotFoundException) {
                continue
            }
        }
        if (url == null) {
            throw ResourceNotFoundException("Resource at $fileName not found")
        }
        return FileHandle(url.file)
    }

    abstract fun getPossibilities(fileName: String): List<String>
}

class PowerworksTextureFileHandleResolver : PowerworksFileHandleResolver() {
    override fun getPossibilities(fileName: String) = listOf(
            fileName, "$fileName.png", "/textures/$fileName",
            "/textures/$fileName.png", "/$fileName"
    )
}

class PowerworksSoundFileHandleResolver : PowerworksFileHandleResolver() {
    override fun getPossibilities(fileName: String) = listOf(
            fileName, "$fileName.ogg", "/sounds/$fileName",
            "/sounds/$fileName.ogg", "/$fileName"
    )
}