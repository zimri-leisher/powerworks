package setting

import data.FileManager
import data.GameDirectoryIdentifier

object Settings {

    val UNKNOWN = UnknownSetting()

    val FPS = IntSetting("Max FPS", { it in 1..9999 }).set(60)

    val SCALE = IntSetting("Game scale", {it in 1..6}).set(4)

    val ALL = mutableListOf<Setting<*>>(UNKNOWN, FPS, SCALE)

    fun tryLoad(name: String): Boolean {
        val loadedSettings = FileManager.tryLoadObject(GameDirectoryIdentifier.SETTINGS, "$name.dat", ALL::class.java)
        if (loadedSettings != null) {
            for (setting in loadedSettings) {
                val internalSetting = ALL.firstOrNull { it.name == setting.name }
                internalSetting?.trySet(setting.get()) ?: println("unknown setting ${setting.name}")
            }
            return true
        }
        return false
    }

    fun save(name: String) {
        FileManager.saveObject(GameDirectoryIdentifier.SETTINGS, "$name.dat", ALL)
    }
}