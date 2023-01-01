package data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.ScreenUtils
import main.Game
import serialization.Input
import serialization.Output
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
        if (Game.IS_SERVER) {
            directory("server") {
                directory("saves", GameDirectoryIdentifier.SAVES)
                directory("players", GameDirectoryIdentifier.PLAYERS)
            }
        } else {
            directory("settings", GameDirectoryIdentifier.SETTINGS) {
                directory("controls", GameDirectoryIdentifier.CONTROLS) {
                    copyOfFile("/settings/controls/default.txt")
                    copyOfFile("/settings/controls/texteditor.txt")
                }
            }
            directory("data") {
                directory("saves", GameDirectoryIdentifier.SAVES)
                directory("screenshots", GameDirectoryIdentifier.SCREENSHOTS, false)
            }
        }
    }

    /**
     * Uses [Input] to load an object at the [directoryIdentifier] + [further] of the given (class)[objectType]
     * @return the object if its file exists, null otherwise
     */
    fun <T> tryLoadObject(directoryIdentifier: DirectoryIdentifier, further: String, objectType: Class<T>) = tryLoadObject(fileSystem.getPath(directoryIdentifier).resolve(further), objectType)

    fun tryGetFile(path: Path): File? {
        val file = path.toFile()
        if (!file.exists()) {
            return null
        }
        return file
    }

    fun tryGetFile(directoryIdentifier: DirectoryIdentifier, further: String) = tryGetFile(fileSystem.getPath(directoryIdentifier).resolve(further))

    /**
     * Uses [Input] to load an object at the [path] of the given (class)[objectType]
     * @return the object if its file exists, null otherwise
     */
    fun <T> tryLoadObject(path: Path, objectType: Class<T>): T? {
        val file = tryGetFile(path) ?: return null
        val input = Input(FileInputStream(file))
        println(file.absolutePath)
        val obj = input.read(objectType)
        input.close()
        return obj
    }

    fun tryGetFileInputStream(path: Path): FileInputStream? {
        val file = tryGetFile(path) ?: return null
        return FileInputStream(file)
    }

    fun tryGetFileInputStream(directoryIdentifier: DirectoryIdentifier, further: String) = tryGetFileInputStream(fileSystem.getPath(directoryIdentifier).resolve(further))

    /**
     * @return true if a file of the given [name] exists in the given [dir]
     */
    fun fileExists(dir: DirectoryIdentifier, name: String) = fileSystem.getPath(dir).resolve(name).toFile().exists()

    /**
     * Uses [Output] to save the given [obj] at the [path]. Creates directories if they do not exist
     */
    fun saveObject(path: Path, obj: Any) {
        val directory = path.parent
        if (Files.notExists(directory))
            Files.createDirectories(directory)
        if (Files.exists(path)) {
            Files.delete(path)
        }
        Files.createFile(path)
        val output = Output(FileOutputStream(path.toFile()))
        output.write(obj)
        output.close()
    }

    /**
     * Uses [Output] to save the given [obj] at the path [directoryIdentifier] + [further]. Creates directories if they do not exist
     */
    fun saveObject(directoryIdentifier: DirectoryIdentifier, further: String, obj: Any) = saveObject(fileSystem.getPath(directoryIdentifier).resolve(further), obj)

    fun getFileOutputStream(path: Path): FileOutputStream {
        val directory = path.parent
        if (Files.notExists(directory))
            Files.createDirectories(directory)
        if (Files.exists(path)) {
            Files.delete(path)
        }
        Files.createFile(path)
        return FileOutputStream(path.toFile())
    }

    fun getFileOutputStream(directoryIdentifier: DirectoryIdentifier, further: String) = getFileOutputStream(fileSystem.getPath(directoryIdentifier).resolve(further))

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
    ENCLOSING, SCREENSHOTS, CONTROLS, SAVES, PLAYERS, SETTINGS
}