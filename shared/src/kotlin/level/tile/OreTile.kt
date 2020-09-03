package level.tile

import graphics.Renderer
import level.Level
import level.LevelManager
import level.getChunkAtChunk
import level.getTileAtTile
import misc.Geometry

private enum class TileState(val indices: List<Int>, val connections: Array<Boolean>) {

    NONE(1, arrayOf(false, false, false, false)),
    ALL(listOf(2, 3, 4, 19), arrayOf(true, true, true, true)),
    UPPER_LEFT(5, arrayOf(false, true, true, false)),
    UPPER(6, arrayOf(false, true, true, true)),
    UPPER_RIGHT(7, arrayOf(false, false, true, true)),
    LEFT(8, arrayOf(true, true, true, false)),
    RIGHT(9, arrayOf(true, false, true, true)),
    LOWER_LEFT(10, arrayOf(true, true, false, false)),
    LOWER(11, arrayOf(true, true, false, true)),
    LOWER_RIGHT(12, arrayOf(true, false, false, true)),
    SINGLE_UP(13, arrayOf(true, false, false, false)),
    SINGLE_RIGHT(14, arrayOf(false, true, false, false)),
    SINGLE_DOWN(15, arrayOf(false, false, true, false)),
    SINGLE_LEFT(16, arrayOf(false, false, false, true)),
    HORIZONTAL(17, arrayOf(false, true, false, true)),
    VERTICAL(18, arrayOf(true, false, true, false));

    constructor(index: Int, connections: Array<Boolean>) : this(listOf(index), connections)
}

class OreTile(override val type: OreTileType, xTile: Int, yTile: Int, level: Level) : Tile(type, xTile, yTile, level) {

    private constructor() : this(OreTileType.ROCK_IRON_ORE, 0, 0, LevelManager.EMPTY_LEVEL)

    private var state = TileState.NONE

    var amount = ((type.maxAmount - type.minAmount) * Math.random() + 1 + type.minAmount).toInt()
        set(value) {
            if (value < 1) {
                level.getChunkAtChunk(xChunk, yChunk).setTile(Tile(type.backgroundType, xTile, yTile, level))
                field = value
            }
        }

    var firstTimeRendered = false

    fun updateState() {
        val array = arrayOf(false, false, false, false)
        for (i in 0..3) {
            val tileAt = level.getTileAtTile(xTile + Geometry.getXSign(i), yTile + Geometry.getYSign(i))
            if (tileAt.type == this.type) {
                array[i] = true
            }
        }
        state = TileState.values().first { it.connections.contentEquals(array) }
        // got an error collection empty bug here
        texture = type.textures[state.indices.filter { it - 1 <= type.textures.lastIndex }.random() - 1]
    }

    override fun render() {
        if (!firstTimeRendered) {
            firstTimeRendered = true
            updateState()
        }
        Renderer.renderTexture(texture, x, y)
    }
}