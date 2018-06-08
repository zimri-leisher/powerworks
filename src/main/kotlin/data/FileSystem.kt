package data

import main.Game
import main.ResourceManager
import java.io.File
import java.net.URL
import java.nio.file.*

interface DirectoryChangeWatcher {
    fun onDirectoryChange(dir: Path)
}

/**
 * Creates a file system at the given base path
 *
 * Useful for maintaining specific file structures determined at runtime.
 * @see FileManager
 *
 * @param closure executes inside of the directory at the base path, allowing you to immediately specify directories
 */
class FileSystem(basePath: Path, baseIdentifier: DirectoryIdentifier? = null, closure: Directory.() -> Unit = {}) {

    private val directoryChangeWatchers = mutableMapOf<DirectoryChangeWatcher, MutableList<Path>>()
    private val fileChangeWatcher = FileSystems.getDefault().newWatchService()

    val baseDir = Directory(basePath, baseIdentifier)

    init {
        ALL.add(this)
        closure(baseDir)
    }

    fun registerDirectoryChangeWatcher(w: DirectoryChangeWatcher, dir: Path) {
        dir.register(fileChangeWatcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)
        if (w in directoryChangeWatchers) {
            directoryChangeWatchers.get(w)!!.add(dir)
        } else {
            directoryChangeWatchers.put(w, mutableListOf(dir))
        }
    }

    /**
     * Goes through and creates all directories in this FileSystem as necessary. Used in combination with the create parameter on the directory function
     */
    fun createAllDirectories() {

        fun create(d: Directory) {
            if (Files.notExists(d.fullPath))
                Files.createDirectories(d.fullPath)
            d.children.forEach { create(it) }
        }

        create(baseDir)

    }

    /**
     * Creates a directory inside of the base directory of this FileSystem
     * @param directoryPath the relative path (the path from the base directory to the new directory). Multiple length paths, like 'foo/bar/test', are allowed. If the directoryIdentifier parameter is not null, only the last folder will be given the identifier. Closure will only be called on the last folder
     * @param directoryIdentifier the DirectoryIdentifier of this directory. If null, it has none. If not null, you can get the path to this directory by calling FileSystem.getPath with the relevant DirectoryIdentifier
     * @param create whether or not to immediately create the directories as they are declared. If false, you can use FileSystem.createAllDirectories or ensureDirectoryExists to do this later
     * @param closure a lambda allowing multiple directories to be chained together. For example,
     *                directory("test") {
     *                  directory("foo")
     *                }
     *                creates a directory named test with another inside of it named foo
     */
    fun directory(directoryPath: Path, directoryIdentifier: DirectoryIdentifier? = null, create: Boolean = true, closure: Directory.() -> Unit = {}) {
        baseDir.directory(directoryPath, directoryIdentifier, create, closure)
    }

    /**
     * If the given identifier exists, does nothing, otherwise, it creates the relevant directories
     */
    fun ensureDirectoryExists(id: DirectoryIdentifier) {
        val path = getPath(id)
        if(Files.notExists(path))
            Files.createDirectories(path)
    }

    fun getPath(dir: DirectoryIdentifier): Path {
        if(dir == GameDirectoryIdentifier.JAR)
            return Paths.get(JAR_PATH)
        if(dir == GameDirectoryIdentifier.TEMP)
            return Paths.get(TEMP_DIR)

        fun search(d: Directory): Path? {
            if (d.name == dir)
                return d.fullPath
            d.children.forEach {
                val p = search(it)
                if (p != null)
                    return p
            }
            return null
        }

        val p = search(baseDir)
        if (p != null)
            return p
        throw Exception("Please add the directory $dir to the directory tree when specifying directory identifiers, it was searched for and not found")
    }

    private fun update() {
        val changeKey = fileChangeWatcher.poll()
        if (changeKey != null) {
            for (event in changeKey.pollEvents()) {
                val type = event.kind()
                if (type == StandardWatchEventKinds.OVERFLOW)
                    continue
                val ev = event as WatchEvent<Path>
                val dir = ev.context()
                directoryChangeWatchers.filterValues { dir in it }.forEach { watcher, _ -> watcher.onDirectoryChange(dir) }
            }
            changeKey.reset()
        }
    }

    companion object {
        val ALL = mutableListOf<FileSystem>()

        val JAR_PATH = Game::class.java.protectionDomain.codeSource.location.toURI().path.drop(1)
        val ENCLOSING_DIR = JAR_PATH.substring(0 until JAR_PATH.lastIndexOf("/"))
        val TEMP_DIR = System.getProperty("java.io.tmpdir")!!

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}

/**
 * Just a quality of life identifier for directories. Used with getPath method of FileSystem to return the path of a directory based just on this
 *
 * @see GameDirectoryIdentifier
 */
interface DirectoryIdentifier

/**
 * Represents a directory in the FileSystem.
 */
class Directory(val fullPath: Path, val name: DirectoryIdentifier? = null) {

    val children = mutableListOf<Directory>()

    val files = mutableListOf<File>()

    /**
     * Creates a directory inside of this one
     * @param directoryPath the relative path (the path from this directory to the new directory). Multiple length paths, like 'foo/bar/test', are allowed. If the directoryIdentifier parameter is not null, only the last folder will be given the identifier. Closure will only be called on the last folder
     * @param directoryIdentifier the DirectoryIdentifier of this directory. If null, it has none. If not null, you can get the path to this directory by calling FileSystem.getPath with the relevant DirectoryIdentifier
     * @param create whether or not to immediately create the directories as they are declared. If false, you can use FileSystem.create to do this later
     * @param closure a lambda allowing multiple directories to be chained together. For example,
     *                directory("test") {
     *                  directory("foo")
     *                }
     *                creates a directory named test with another inside of it named foo
     */
    fun directory(directoryPath: Path, directoryIdentifier: DirectoryIdentifier? = null, create: Boolean = true, closure: Directory.() -> Unit = {}) {
        // if the path is skipping over a directory, for example, it is textures/block/miners
        val count = directoryPath.nameCount
        var currentDirectory = this
        if (count > 1) {
            // go through all but the last, the last one will be the one that gets the ID and the one where the closure is run in
            for (subpath in directoryPath.subpath(0, count - 1)) {
                val nextPath = currentDirectory.fullPath.resolve(subpath)
                if (create)
                    if (Files.notExists(nextPath))
                        Files.createDirectories(nextPath)
                val nextDirectory = Directory(nextPath)
                currentDirectory.children.add(nextDirectory)
                currentDirectory = nextDirectory
            }
        }
        // now we know that the current directory is the one above the final one in the chain (either this, if there is no chain, or the second to last element of the chain)
        val fullPath = fullPath.resolve(directoryPath)
        val d = Directory(fullPath, directoryIdentifier)
        currentDirectory.children.add(d)
        if (create)
            if (Files.notExists(fullPath))
                Files.createDirectories(fullPath)
        closure(d)
    }

    /**
     * Creates a directory inside of this one
     * @param directoryName the relative name (the path from this directory to the new directory). Multiple length names, like 'foo/bar/test', are allowed. If the directoryIdentifier parameter is not null, only the last folder will be given the identifier. Closure will only be called on the last folder
     * @param directoryIdentifier the DirectoryIdentifier of this directory. If null, it has none. If not null, you can get the path to this directory by calling FileSystem.getPath with the relevant DirectoryIdentifier
     * @param create whether or not to immediately create the directories as they are declared. If false, you can use FileSystem.create to do this later
     * @param closure a lambda allowing multiple directories to be chained together. For example,
     *                directory("test") {
     *                  directory("foo")
     *                }
     *                creates a directory named test with another inside of it named foo
     */
    fun directory(directoryName: String, directoryIdentifier: DirectoryIdentifier? = null, create: Boolean = true, closure: Directory.() -> Unit = {}) =
            directory(Paths.get(directoryName), directoryIdentifier, create, closure)

    /**
     * Copies a file from one directory, presumably in the game resources folder, to the specified directory. It will create all parent directories as necessary
     * @param originalLocation the full URL to the file
     * @param newName the name of the file, including the file type
     */
    fun copyOfFile(originalLocation: URL, newName: String = Paths.get(originalLocation.path).last().toString()) {
        val actualFile = fullPath.resolve(newName)
        val f = actualFile.toFile()
        files.add(f)
        val directory = actualFile.parent
        if(Files.notExists(directory))
            Files.createDirectories(directory)
        if (Files.notExists(actualFile)) {
            Files.createFile(actualFile)
        }
        f.writeBytes(originalLocation.readBytes())
    }

    /**
     * @param originalLocation the location relative to the resources folder
     * @param newName the name of the file, including the file type
     */
    fun copyOfFile(originalLocation: String, newName: String = Paths.get(originalLocation).last().toString()) {
        copyOfFile(ResourceManager.getRawResource(originalLocation), newName)
    }

}