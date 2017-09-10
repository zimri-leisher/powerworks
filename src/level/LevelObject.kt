package level

import main.Game
import java.io.DataOutputStream

abstract class LevelObject protected constructor(xPixel: Int, yPixel: Int, requiresUpdate: Boolean = true) {

    open val xPixel: Int = xPixel
    open val yPixel: Int = yPixel
    open val xTile = xPixel shr 4
    open val yTile = yPixel shr 4
    open val xChunk = xTile shr 3
    open val yChunk = yTile shr 3

    var requiresUpdate: Boolean = requiresUpdate
        set(value) {
            val c = Game.currentLevel.getChunk(xChunk, yChunk)
            if(field && !value) {
                c.updatesRequired!!.remove(this)
            } else if(!field && value) {
                c.updatesRequired!!.add(this)
            }
        }

    abstract fun render()

    open fun update() {

    }

    open fun save(out: DataOutputStream) {
        out.writeInt(xPixel)
        out.writeInt(yPixel)
    }
}