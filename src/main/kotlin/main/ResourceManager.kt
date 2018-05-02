package main

import mod.Mod
import mod.ModManager
import java.net.URL

object ResourceManager {

    var currentModContext: Mod? = null

    fun getResource(path: String): URL {
        if (currentModContext != null) {
            return ModManager.getMainClass(currentModContext!!).getResource(path)
        } else {
            return ResourceManager.javaClass.getResource(path)
        }
    }

    fun getResourceAsStream(path: String) = if (currentModContext != null) ModManager.getMainClass(currentModContext!!).getResourceAsStream(path) else ResourceManager.javaClass.getResourceAsStream(path)
}