package data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.ScreenUtils
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import main.Game
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * An object with various utility methods for interaction with files. Use [FileManager.fileSystem] to get
 * paths, create directories and so on
 */
object FileManager {

    val fileSystem = FileSystem(Paths.get(Game.JAR_PATH.substring(0 until Game.JAR_PATH.lastIndexOf("/"))), GameDirectoryIdentifier.ENCLOSING) {
        if(Game.IS_SERVER) {
            directory("server") {
                directory("saves", GameDirectoryIdentifier.SAVES)
                directory("players", GameDirectoryIdentifier.PLAYERS)
            }
        } else {
            directory("settings/controls", GameDirectoryIdentifier.CONTROLS) {
                copyOfFile("/settings/controls/default.txt")
                copyOfFile("/settings/controls/texteditor.txt")
            }
            directory("data") {
                directory("saves", GameDirectoryIdentifier.SAVES)
                directory("screenshots", GameDirectoryIdentifier.SCREENSHOTS, false)
            }
        }
    }

    /**
     * Uses [Game.KRYO] to load an object at the [path] of the given (class)[objectType]
     * @return the object if its file exists, null otherwise
     */
    fun <T> loadObject(path: Path, objectType: Class<T>): T? {
        val file = path.toFile()
        if (!file.exists()) {
            return null
        }
        val input = Input(FileInputStream(file))
        val obj = Game.KRYO.readObject(input, objectType)
        input.close()
        return obj
    }

    /**
     * Uses [Game.KRYO] to load an object at the [directoryIdentifier] + [further] of the given (class)[objectType]
     * @return the object if its file exists, null otherwise
     */
    fun <T> loadObject(directoryIdentifier: DirectoryIdentifier, further: String, objectType: Class<T>) = loadObject(fileSystem.getPath(directoryIdentifier).resolve(further), objectType)

    /**
     * Uses [Game.KRYO] to save the given [obj] at the [path]. Creates directories if they do not exist
     */
    fun saveObject(path: Path, obj: Any?) {
        val directory = path.parent
        if (Files.notExists(directory))
            Files.createDirectories(directory)
        if (Files.notExists(path)) {
            Files.createFile(path)
        }
        val output = Output(FileOutputStream(path.toFile()))
        Game.KRYO.writeObject(output, obj)
        output.close()
    }

    /**
     * @return true if a file of the given [name] exists in the given [dir]
     */
    fun fileExists(name: String, dir: DirectoryIdentifier) = fileSystem.getPath(dir).resolve(name).toFile().exists()

    /**
     * Uses [Game.KRYO] to save the given [obj] at the path [directoryIdentifier] + [further]. Creates directories if they do not exist
     */
    fun saveObject(directoryIdentifier: DirectoryIdentifier, further: String, obj: Any?) = saveObject(fileSystem.getPath(directoryIdentifier).resolve(further), obj)

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
    ENCLOSING, SCREENSHOTS, CONTROLS, SAVES, PLAYERS
}