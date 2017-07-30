package level

import java.io.OutputStream

abstract class LevelObject protected constructor(val xPixel: Int, val yPixel: Int) {

    val xTile = xPixel shr 4
    val yTile = yPixel shr 4
    val xChunk = xTile shr 3
    val yChunk = yTile shr 3

    abstract fun render()

    abstract fun save(out: OutputStream)
}