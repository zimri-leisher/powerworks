package level

import misc.Coord
import misc.TileCoord
import serialization.Id

data class LevelPosition(
        @Id(1)
        val x: Int,
        @Id(2)
        val y: Int,
        @Id(3)
        val level: Level) {
    private constructor() : this(0, 0, LevelManager.EMPTY_LEVEL)

    val xTile get() = x shr 4
    val yTile get() = y shr 4
    val xChunk get() = x shr CHUNK_EXP
    val yChunk get() = y shr CHUNK_EXP

    fun tile() = TileCoord(xTile, yTile)

    fun coord() = Coord(x, y)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LevelPosition

        if (x != other.x) return false
        if (y != other.y) return false
        if (level.id != other.level.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + level.id.hashCode()
        return result
    }
}