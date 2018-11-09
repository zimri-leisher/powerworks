package data

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
 *
 * @param closure executes inside of the directory at the base path, allowing you to specify directories in a DSL-style
 */
class FileSystem(basePath: Path, baseIdentifier: DirectoryIdentifier? = null, closure: Directory.() -> Unit = {}) {

    private val directoryChangeWatchers = mutableMapOf<DirectoryChangeWatcher, MutableList<Path>>()
    private val fileChangeWatcher = FileSystems.getDefault().newWatchService()

    private val directoryIdentifiers = mutableMapOf<DirectoryIdentifier, Path>()

    private val baseDir = Directory(basePath, baseIdentifier, this)

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
     * Goes through and creates all directories in this FileSystem which do not exist on the drive yet
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
     * @param directoryPath the relative [Path] (the path from the base directory to the new directory). Multiple length paths, like `"foo/bar/test"`, are allowed. If the [directoryIdentifier] parameter is not `null`, only the last folder will be given the identifier. [closure] will only be called on the last folder
     * @param directoryIdentifier the [DirectoryIdentifier] of this directory. If `null`, it has none. If not `null`, you can get the path to this directory by calling [FileSystem.getPath] with the relevant [DirectoryIdentifier]
     * @param create whether or not to immediately create the directories as they are declared. If false, you can use [createAllDirectories] or [ensureDirectoryExists] to do this later
     * @param closure a lambda allowing multiple directories to be chained together. For example,
     *
     *
     * `
     *                directory("test") {
     *                  directory("foo")
     *                }
     * `
     *
     * creates a directory named test with another inside of it named foo
     *
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun directory(directoryPath: Path, directoryIdentifier: DirectoryIdentifier? = null, create: Boolean = true, closure: Directory.() -> Unit = {}) {
        baseDir.directory(directoryPath, directoryIdentifier, create, closure)
    }

    /**
     * Creates a directory inside of the base directory of this FileSystem
     * @param directoryName the relative path (the path from the base directory to the new directory) resolved using [Paths.get]. Multiple length paths, like `"foo/bar/test"`, are allowed. If the [directoryIdentifier] parameter is not `null`, only the last folder will be given the identifier. [closure] will only be called on the last folder
     * @param directoryIdentifier the [DirectoryIdentifier] of this directory. If `null`, it has none. If not `null`, you can get the path to this directory by calling [FileSystem.getPath] with the relevant [DirectoryIdentifier]
     * @param create whether or not to immediately create the directories as they are declared. If false, you can use [createAllDirectories] or [ensureDirectoryExists] to do this later
     * @param closure a lambda allowing multiple directories to be chained together. For example,
     *
     *
     * `
     *                directory("test") {
     *                  directory("foo")
     *                }
     * `
     *
     * creates a directory named test with another inside of it named foo
     *
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun directory(directoryName: String, directoryIdentifier: DirectoryIdentifier? = null, create: Boolean = true, closure: Directory.() -> Unit = {}) {
        baseDir.directory(directoryName, directoryIdentifier, create, closure)
    }

    /**
     * If a directory with the [identifier] exists, does nothing, otherwise, it creates the relevant directories
     */
    fun ensureDirectoryExists(identifier: DirectoryIdentifier) {
        val path = getPath(identifier)
        if (Files.notExists(path))
            Files.createDirectories(path)
    }

    /**
     * @return the full [Path] of the [Directory] associated with the [identifier]
     */
    fun getPath(identifier: DirectoryIdentifier): Path {
        return directoryIdentifiers[identifier]
                ?: throw Exception("Directory $identifier was searched for and not found")
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

        fun update() {
            ALL.forEach { it.update() }
        }
    }

    /**
     * Represents a directory in the FileSystem. This doesn't necessarily exist on the hard drive. Creating more of these
     * is the job of the [directory] method
     */
    class Directory(val fullPath: Path, name: DirectoryIdentifier? = null, val parentFileSystem: FileSystem) {

        val children = mutableListOf<Directory>()

        private val files = mutableListOf<File>()

        init {
            if (name != null)
                parentFileSystem.directoryIdentifiers.put(name, fullPath)
        }

        /**
         * Creates a directory inside of this one
         * @param directoryPath the relative [Path] (the path from the base directory to the new directory). Multiple length paths, like `"foo/bar/test"`, are allowed. If the [directoryIdentifier] parameter is not `null`, only the last folder will be given the identifier. [closure] will only be called on the last folder
         * @param directoryIdentifier the [DirectoryIdentifier] of this directory. If `null`, it has none. If not `null`, you can get the path to this directory by calling [FileSystem.getPath] with the relevant [DirectoryIdentifier]
         * @param create whether or not to immediately create the directories as they are declared. If false, you can use [createAllDirectories] or [ensureDirectoryExists] to do this later
         * @param closure a lambda allowing multiple directories to be chained together. For example,
         *
         *
         * `
         *                directory("test") {
         *                  directory("foo")
         *                }
         * `
         *
         * creates a directory named test with another inside of it named foo
         *
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
                    val nextDirectory = Directory(nextPath, parentFileSystem = parentFileSystem)
                    currentDirectory.children.add(nextDirectory)
                    currentDirectory = nextDirectory
                }
            }
            // now we know that the current directory is the one above the final one in the chain (either this, if there is no chain, or the second to last element of the chain)
            val fullPath = fullPath.resolve(directoryPath)
            val d = Directory(fullPath, directoryIdentifier, parentFileSystem)
            currentDirectory.children.add(d)
            if (create)
                if (Files.notExists(fullPath))
                    Files.createDirectories(fullPath)
            closure(d)
        }

        /**
         * Creates a directory inside of this one
         * @param directoryName the relative path (the path from the base directory to the new directory) resolved using [Paths.get]. Multiple length paths, like `"foo/bar/test"`, are allowed. If the [directoryIdentifier] parameter is not `null`, only the last folder will be given the identifier. [closure] will only be called on the last folder
         * @param directoryIdentifier the [DirectoryIdentifier] of this directory. If `null`, it has none. If not `null`, you can get the path to this directory by calling [FileSystem.getPath] with the relevant [DirectoryIdentifier]
         * @param create whether or not to immediately create the directories as they are declared. If false, you can use [createAllDirectories] or [ensureDirectoryExists] to do this later
         * @param closure a lambda allowing multiple directories to be chained together. For example,
         *
         *
         * `
         *                directory("test") {
         *                  directory("foo")
         *                }
         * `
         *
         * creates a directory named test with another inside of it named foo
         *
         */
        fun directory(directoryName: String, directoryIdentifier: DirectoryIdentifier? = null, create: Boolean = true, closure: Directory.() -> Unit = {}) =
                directory(Paths.get(directoryName), directoryIdentifier, create, closure)

        /**
         * Copies a file from one directory (possibly a resources folder), to the specified directory relative to the directory it is called in. It will create all parent directories as necessary.
         * @param internalLocation the full [URL] to the file
         * @param newName the name of the file, including the file type
         */
        fun copyOfFile(internalLocation: URL, newName: String = Paths.get(internalLocation.path).last().toString()) {
            val actualFile = fullPath.resolve(newName)
            val f = actualFile.toFile()
            files.add(f)
            val directory = actualFile.parent
            if (Files.notExists(directory))
                Files.createDirectories(directory)
            if (Files.notExists(actualFile)) {
                Files.createFile(actualFile)
            }
            f.writeBytes(internalLocation.readBytes())
        }

        /**
         * Copies a file from one directory (possibly a resources folder), to the specified directory relative to the directory it is called in. It will create all parent directories as necessary.
         * @param internalLocation the location relative to the resources folder. This is resolved by calling [ResourceManager.getRawResource]
         * @param newName the name of the file, including the file type
         */
        fun copyOfFile(internalLocation: String, newName: String = Paths.get(internalLocation).last().toString()) {
            copyOfFile(ResourceManager.getRawResource(internalLocation), newName)
        }
    }
}

/**
 * Just a quality of life identifier for directories. Used with [FileSystem.getPath] to return the path of a directory
 * which was created using [FileSystem.directory] with the directoryIdentifier parameter set to this
 *
 * @see GameDirectoryIdentifier
 */
interface DirectoryIdentifier