package level

import java.io.File

data class LevelInfo(val name: String, val dateCreated: String, val levelFile: File, val infoFile: File) {
    companion object {
        fun parse(rawData: List<String>, levelFile: File, infoFile: File): LevelInfo {
            return LevelInfo(rawData[0], rawData[1], levelFile, infoFile)
        }
    }
}