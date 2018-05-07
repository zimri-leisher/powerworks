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



interface DirectoryChangeWatcher {
    fun onDirectoryChange(dir: Path)
}

private class Dir(val path: Path, val name: GameDirectory? = null, init: Dir.() -> Unit = {}) {
    constructor(path: String, name: GameDirectory? = null, init: Dir.() -> Unit = {}) : this(Paths.get(path), name, init)

    val children = mutableListOf<Dir>()
    val files = mutableListOf<File>()

    init {
        init()
    }

    fun directory(path: Path, name: GameDirectory? = null, closure: Dir.() -> Unit = {}) {
        val fullPath = this.path.resolve(path)
        val d = Dir(fullPath, name)
        children.add(d)
        if (Files.notExists(fullPath))
            Files.createDirectories(fullPath)
        closure(d)
    }

    fun directory(path: String, name: GameDirectory? = null, closure: Dir.() -> Unit = {}) = directory(Paths.get(path), name, closure)

    fun copyOfFile(originalLocation: String, newName: String = originalLocation.substring(originalLocation.lastIndexOf("/") + 1)) {
        val actualFile = path.resolve(newName)
        val f = actualFile.toFile()
        files.add(f)
        if (Files.notExists(actualFile)) {
            Files.createFile(actualFile)
        }
        f.writeBytes(ResourceManager.getResource(originalLocation).readBytes())
    }
}

enum class GameDirectory {
    JAR, ENCLOSING, MODS, SCREENSHOTS, SAVES, CONTROLS
}

object FileManager {

    val JAR_PATH = Game::class.java.protectionDomain.codeSource.location.toURI().path.drop(1)
    val TEMP_DIR = System.getProperty("java.io.tmpdir")

    val fileChangeWatcher = FileSystems.getDefault().newWatchService()

    private val directoryChangeWatchers = mutableMapOf<DirectoryChangeWatcher, MutableList<Path>>()

    private lateinit var baseDir: Dir

    init {
        baseDirectory {
            directory("mods", GameDirectory.MODS)
            directory("data") {
                directory("settings") {
                    directory("controls", GameDirectory.CONTROLS) {
                        copyOfFile("/settings/controls/default.txt")
                    }
                }
                directory("saves", GameDirectory.SAVES)
            }
            directory("screenshots", GameDirectory.SCREENSHOTS)
        }
    }

    fun getPath(dir: GameDirectory): Path {
        fun search(d: Dir): Path? {
            if(d.name == dir)
                return d.path
            d.children.forEach {
                val p = search(it)
                if(p != null)
                    return p
            }
            return null
        }
        val p = search(baseDir)
        if(p != null)
            return p
        throw Exception("Please add the directory $dir to the game directory initializer, it was searched for and not found")
    }

    private fun baseDirectory(closure: Dir.() -> Unit) {
        baseDir = Dir(JAR_PATH.substring(0 until JAR_PATH.lastIndexOf("/")), GameDirectory.ENCLOSING, closure)
    }

    fun registerDirectoryChangeWatcher(w: DirectoryChangeWatcher, dir: Path) {
        dir.register(fileChangeWatcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)
        if(w in directoryChangeWatchers) {
            directoryChangeWatchers.get(w)!!.add(dir)
        } else {
            directoryChangeWatchers.put(w, mutableListOf(dir))
        }
    }

    fun update() {
        val changeKey = fileChangeWatcher.poll()
        if(changeKey != null) {
            for(event in changeKey.pollEvents()) {
                val type = event.kind()
                if(type == StandardWatchEventKinds.OVERFLOW)
                    continue
                val ev = event as WatchEvent<Path>
                val dir = ev.context()
                directoryChangeWatchers.filterValues { dir in it }.forEach { watcher, _ -> watcher.onDirectoryChange(dir) }
            }
            changeKey.reset()
        }
    }

    fun takeScreenshot() {
        val ss = Game.graphicsConfiguration.createCompatibleImage(Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE)
        Renderer.g2d = ss.createGraphics()
        ScreenManager.render()
        Renderer.g2d.dispose()
        val calInstance = Calendar.getInstance()
        val fileName = "${getPath(GameDirectory.SCREENSHOTS)}/${calInstance.get(Calendar.MONTH) + 1}-${calInstance.get(Calendar.DATE)}-${calInstance.get(Calendar.YEAR)}"
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