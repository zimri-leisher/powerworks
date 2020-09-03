package misc

import level.Level
import serialization.Id
import java.lang.Math.abs

data class PixelCoord(
        @Id(1)
        val xPixel: Int,
        @Id(2)
        val yPixel: Int) {

    private constructor() : this(0, 0)

    fun distance(other: PixelCoord): Double {
        return Geometry.distance(xPixel, yPixel, other.xPixel, other.yPixel)
    }

    fun manhattanDistance(other: PixelCoord): Int {
        return abs(xPixel - other.xPixel) + abs(yPixel - other.yPixel)
    }

    fun toTile() = TileCoord(xPixel shr 4, yPixel shr 4)

    fun enforceBounds(level: Level) = enforceBounds(0, level.widthPixels, 0, level.heightPixels)

    fun enforceBounds(xMin: Int, xMax: Int, yMin: Int, yMax: Int) = PixelCoord(Math.min(xMax, Math.max(xMin, xPixel)), Math.min(yMax, Math.max(yMin, yPixel)))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PixelCoord

        if (xPixel != other.xPixel) return false
        if (yPixel != other.yPixel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = xPixel
        result = 31 * result + yPixel
        return result
    }
}

data class TileCoord(
        @Id(1)
        val xTile: Int,
        @Id(2)
        val yTile: Int) {

    private constructor() : this(0, 0)

    fun pixel() = PixelCoord(xTile shl 4, yTile shl 4)

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