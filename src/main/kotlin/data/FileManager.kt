package data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.ScreenUtils
import main.Game
import java.io.File
import java.nio.file.Paths
import java.util.*

/**
 * An object with various utility methods for interaction with files. Use the [FileManager.fileSystem] property to get
 * paths, create directories and so on
 */
object FileManager {

    val fileSystem = FileSystem(Paths.get(Game.JAR_PATH.substring(0 until Game.JAR_PATH.lastIndexOf("/"))), GameDirectoryIdentifier.ENCLOSING) {
        directory("mods", GameDirectoryIdentifier.MODS)
        directory("data") {
            directory("settings/controls", GameDirectoryIdentifier.CONTROLS) {
                copyOfFile("/settings/controls/default.txt")
                copyOfFile("/settings/controls/texteditor.txt")
            }
            directory("saves", GameDirectoryIdentifier.SAVES)
        }
        directory("screenshots", GameDirectoryIdentifier.SCREENSHOTS, false)
    }

    fun takeScreenshot() {
        fileSystem.ensureDirectoryExists(GameDirectoryIdentifier.SCREENSHOTS)
        val pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight, true)
        var i = 4
        while (i < pixels.size) {
            pixels[i - 1] = 255.toByte()
            i += 4
        }
        val pixmap = Pixmap(Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight, Pixmap.Format.RGBA8888)
        BufferUtils.copy(pixels, 0, pixmap.pixels, pixels.size)
        val calInstance = Calendar.getInstance()
        val fileName = "${fileSystem.getPath(GameDirectoryIdentifier.SCREENSHOTS)}/${calInstance.get(Calendar.DATE) + 1}-${calInstance.get(Calendar.MONTH)}-${calInstance.get(Calendar.YEAR)}"
        var a = 0
        var file = File(fileName + " #$a.png")
        while (file.exists()) {
            a++
            file = File(fileName + " #$a.png")
        }
        PixmapIO.writePNG(FileHandle(file), pixmap)
        pixmap.dispose()
        println("Taken screenshot")
    }
}

enum class GameDirectoryIdentifier : DirectoryIdentifier {
    ENCLOSING, MODS, SCREENSHOTS, SAVES, CONTROLS
}