package mod

import data.FileManager
import data.GameDirectory
import main.Game
import main.ResourceManager
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.jar.JarFile

data class ModInfo(val mod: Mod, val clazz: Class<*>, val name: String, val desc: String, val vers: String)

object ModManager {

    val modInfos = mutableListOf<ModInfo>()

    fun getMainClass(mod: Mod) = modInfos.first { it.mod == mod }.clazz

    fun initialize() {
        println("Initializing mods")
        val files = File(FileManager.getPath(GameDirectory.MODS).toString()).listFiles { _, name -> name.endsWith(".jar") }
        val urls = files.map { URL("jar:file:${FileManager.getPath(GameDirectory.MODS)}/${it.name}!/") }
        val pluginLoader = URLClassLoader(urls.toTypedArray())

        files.forEach { file ->
            val jar = JarFile(file)
            val jarLoader = URLClassLoader(arrayOf(URL("jar:file:${FileManager.getPath(GameDirectory.MODS)}/${file.name}!/")))
            val serviceLoader = ServiceLoader.load(Mod::class.java)
            serviceLoader.forEach { println("found a mod class in ${jar.name}") }
            jar.stream().filter { it.name.endsWith(".class") && it.name != "module-info.class" }.forEach { clasS ->
                val clazz = pluginLoader.loadClass(clasS.name.replace("/", ".").removeSuffix(".class"))
                if (clazz.interfaces.any { it == Mod::class.java }) {
                    try {
                        val infoText = clazz.getResource("/info.txt").readText().lines()
                        val name = infoText[0].split(":")[1]
                        val desc = infoText[1].split(":")[1]
                        val vers = infoText[2].split(":")[1]
                        println("Found mod $name")
                        val mod = ModInfo(clazz.getDeclaredConstructor().newInstance() as Mod, clazz, name, desc, vers)
                        modInfos.add(mod)
                    } catch (e: FileNotFoundException) {
                        println("ERROR: Jar file ${jar.name} found but contains no res/info.txt. Skipped loading")
                    } catch (e: IndexOutOfBoundsException) {
                        println("ERROR: Problem loading, probably incorrectly formatted res/info.txt of mod in jar file ${jar.name}. Skipped loading")
                    }
                }
            }
        }
        modInfos.forEach {
            setCurrentModContext(it.mod)
            try {
                it.mod.initialize()
            } catch (e: Exception) {
                println("ERROR: Exception loading mod ${it.name}:")
                e.printStackTrace(System.out)
            }

        }
        setCurrentModContext(null)
    }

    fun setCurrentModContext(mod: Mod?) {
        ResourceManager.currentModContext = mod
    }

    fun shutdown() {
        println("Shutting down mods")
        modInfos.forEach {
            setCurrentModContext(it.mod)
            try {
                it.mod.shutdown()
            } catch (e: Exception) {
                println("ERROR: Exception shutting down mod ${it.name}:")
                e.printStackTrace(System.out)
            }

        }
        setCurrentModContext(null)
    }
}