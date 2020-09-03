package level

import misc.PixelCoord
import misc.TileCoord
import serialization.Id

data class LevelPosition(
        @Id(1)
        val xPixel: Int,
        @Id(2)
        val yPixel: Int,
        @Id(3)
        val level: Level) {
    private constructor() : this(0, 0, LevelManager.EMPTY_LEVEL)

    val xTile get() = xPixel shr 4
    val yTile get() = yPixel shr 4
    val xChunk get() = xPixel shr CHUNK_PIXEL_EXP
    val yChunk get() = yPixel shr CHUNK_PIXEL_EXP

    fun tile() = TileCoord(xTile, yTile)

    fun pixel() = PixelCoord(xPixel, yPixel)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LevelPosition

        if (xPixel != other.xPixel) return false
        if (yPixel != other.yPixel) return false
        if (level.id != other.level.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = xPixel
        result = 31 * result + yPixel
        result = 31 * result + level.id.hashCode()
        return result
    }
}