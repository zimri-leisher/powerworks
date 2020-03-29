package misc

import level.Level
import serialization.Id

data class PixelCoord(
        @Id(1)
        var xPixel: Int,
        @Id(2)
        var yPixel: Int) {

    private constructor() : this(0, 0)

    fun toTile() = TileCoord(xPixel shr 4, yPixel shr 4)

    fun enforceBounds(level: Level) = enforceBounds(0, level.widthPixels, 0, level.heightPixels)

    fun enforceBounds(xMin: Int, xMax: Int, yMin: Int, yMax: Int) {
        xPixel = Math.min(xMax, Math.max(xMin, xPixel))
        yPixel = Math.min(yMax, Math.max(yMin, yPixel))
    }

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
        var xTile: Int,
        @Id(2)
        var yTile: Int) {

    private constructor() : this(0, 0)

    fun toPixel() = PixelCoord(xTile shl 4, yTile shl 4)

    fun enforceBounds(level: Level) = enforceBounds(0, level.widthTiles, 0, level.heightTiles)

    fun enforceBounds(xMin: Int, xMax: Int, yMin: Int, yMax: Int) {
        xTile = Math.min(xMax, Math.max(xMin, xTile))
        yTile = Math.min(yMax, Math.max(yMin, yTile))
    }

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