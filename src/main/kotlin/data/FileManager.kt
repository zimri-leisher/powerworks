package data

import graphics.Renderer
import main.Game
import main.ResourceManager
import screen.ScreenManager
import java.io.File
import java.nio.file.*
import java.util.*
import javax.imageio.ImageIO
import java.nio.file.WatchEvent

object FileManager {

    val fileSystem: FileSystem

    init {
        fileSystem = FileSystem(Paths.get(Game.JAR_PATH.substring(0 until Game.JAR_PATH.lastIndexOf("/"))), GameDirectoryIdentifier.ENCLOSING) {
            directory("mods", GameDirectoryIdentifier.MODS)
            directory("data") {
                directory("settings/controls", GameDirectoryIdentifier.CONTROLS) {
                    copyOfFile("/settings/controls/default.txt")
                }
                directory("saves", GameDirectoryIdentifier.SAVES)
            }
            directory("screenshots", GameDirectoryIdentifier.SCREENSHOTS, false)
        }
    }

    fun takeScreenshot() {
        fileSystem.ensureDirectoryExists(GameDirectoryIdentifier.SCREENSHOTS)
        val ss = Game.graphicsConfiguration.createCompatibleImage(Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE)
        Renderer.g2d = ss.createGraphics()
        ScreenManager.render()
        Renderer.g2d.dispose()
        val calInstance = Calendar.getInstance()
        val fileName = "${fileSystem.getPath(GameDirectoryIdentifier.SCREENSHOTS)}/${calInstance.get(Calendar.DATE) + 1}-${calInstance.get(Calendar.MONTH)}-${calInstance.get(Calendar.YEAR)}"
        var i = 0
        var file = File(fileName + " #$i.png")
        while (file.exists()) {
            i++
            file = File(fileName + " #$i.png")
        }
        ImageIO.write(ss, "png", file)
        println("Taken screenshot")
    }
}

enum class GameDirectoryIdentifier : DirectoryIdentifier {
    JAR, TEMP, ENCLOSING, MODS, SCREENSHOTS, SAVES, CONTROLS
}