package level

import java.io.File

data class LevelInfo(val name: String, val dateCreated: String, val settings: LevelGeneratorSettings, var levelFile: File, var infoFile: File) {
    companion object {
        fun parse(rawData: List<String>, levelFile: File, infoFile: File): LevelInfo {
            return LevelInfo(rawData[0], rawData[1], LevelGeneratorSettings(rawData[2].toInt(), rawData[3].toInt()), levelFile, infoFile)
        }
    }
}