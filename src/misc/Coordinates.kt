package misc

data class PixelCoord(var xPixel: Int, var yPixel: Int) {
    fun toTile() = TileCoord(xPixel shr 4, yPixel shr 4)
}

data class TileCoord(var xTile: Int, var yTile: Int) {
    fun toPixel() = PixelCoord(xTile shl 4, yTile shl 4)
}