package misc

import level.Level
import serialization.Id
import java.lang.Math.abs

data class Coord(
        @Id(1)
        val x: Int,
        @Id(2)
        val y: Int) {

    private constructor() : this(0, 0)

    fun distance(other: Coord): Double {
        return Geometry.distance(x, y, other.x, other.y)
    }

    fun manhattanDistance(other: Coord): Int {
        return abs(x - other.x) + abs(y - other.y)
    }

    fun toTile() = TileCoord(x shr 4, y shr 4)

    fun enforceBounds(level: Level) = enforceBounds(0, level.width, 0, level.height)

    fun enforceBounds(xMin: Int, xMax: Int, yMin: Int, yMax: Int) = Coord(Math.min(xMax, Math.max(xMin, x)), Math.min(yMax, Math.max(yMin, y)))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Coord

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }
}

data class TileCoord(
        @Id(1)
        val xTile: Int,
        @Id(2)
        val yTile: Int) {

    private constructor() : this(0, 0)

    fun toCoord() = Coord(xTile shl 4, yTile shl 4)

    fun enforceBounds(level: Level) = enforceBounds(0, level.widthTiles, 0, level.heightTiles)

    fun enforceBounds(xMin: Int, xMax: Int, yMin: Int, yMax: Int) = TileCoord(Math.min(xMax, Math.max(xMin, xTile)), Math.min(yMax, Math.max(yMin, yTile)))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TileCoord

        if (xTile != other.xTile) return false
        if (yTile != other.yTile) return false

        return true
    }

    override fun hashCode(): Int {
        var result = xTile
        result = 31 * result + yTile
        return result
    }
}