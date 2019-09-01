package level

data class LevelGeneratorSettings(val widthTiles: Int = 256, val heightTiles: Int = 256) {
    /**
     * This is only used for the default level that everything is created in
     */
    val empty get() = widthTiles == 0 && heightTiles == 0
}