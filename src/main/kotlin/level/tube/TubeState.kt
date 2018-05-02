package level.tube

import graphics.Image.Block
import graphics.Texture
import graphics.ImageCollection as Col

enum class TubeState(val texture: Texture, val connections: Array<Boolean>, val closedEnds: Array<Boolean> = arrayOf(false, false, false, false)) {
    NONE(Block.TUBE_2_WAY_VERTICAL, arrayOf(
            false, false, false, false
    ), arrayOf(
            true, false, true, false
    )),
    UP_ONLY(Block.TUBE_2_WAY_VERTICAL, arrayOf(
            true, false, false, false
    ), arrayOf(
           false, false, true, false
    )),
    RIGHT_ONLY(Block.TUBE_2_WAY_HORIZONTAL, arrayOf(
            false, true, false, false
    ), arrayOf(
            false, false, false, true
    )),
    DOWN_ONLY(Block.TUBE_2_WAY_VERTICAL, arrayOf(
            false, false, true, false
    ), arrayOf(
            true, false, false, false
    )),
    LEFT_ONLY(Block.TUBE_2_WAY_HORIZONTAL, arrayOf(
            false, false, false, true
    ), arrayOf(
            false, true, false, false
    )),
    UP_DOWN(Block.TUBE_2_WAY_VERTICAL, arrayOf(
            true, false, true, false
    )),
    RIGHT_LEFT(Block.TUBE_2_WAY_HORIZONTAL, arrayOf(
            false, true, false, true
    )),
    UP_RIGHT(Col.TUBE_CORNER[0], arrayOf(
            true, true, false, false
    )),
    RIGHT_DOWN(Col.TUBE_CORNER[1], arrayOf(
            false, true, true, false
    )),
    DOWN_LEFT(Col.TUBE_CORNER[2], arrayOf(
            false, false, true, true
    )),
    LEFT_UP(Col.TUBE_CORNER[3], arrayOf(
            true, false, false, true
    )),
    LEFT_UP_RIGHT(Col.TUBE_3_WAY[0], arrayOf(
            true, true, false, true
    )),
    UP_RIGHT_DOWN(Col.TUBE_3_WAY[1], arrayOf(
            true, true, true, false
    )),
    RIGHT_DOWN_LEFT(Col.TUBE_3_WAY[2], arrayOf(
            false, true, true, true
    )),
    DOWN_LEFT_UP(Col.TUBE_3_WAY[3], arrayOf(
            true, false, true, true
    )),
    ALL(Block.TUBE_4_WAY, arrayOf(
            true, true, true, true
    ));

    enum class Group(vararg val states: TubeState) {
        INTERSECTION(UP_RIGHT,
                RIGHT_DOWN,
                DOWN_LEFT,
                LEFT_UP,
                LEFT_UP_RIGHT,
                UP_RIGHT_DOWN,
                RIGHT_DOWN_LEFT,
                DOWN_LEFT_UP,
                ALL),
        NOT_INTERSECTION(NONE,
                UP_ONLY,
                RIGHT_ONLY,
                DOWN_ONLY,
                LEFT_ONLY,
                UP_DOWN,
                RIGHT_LEFT),
        CORNER(UP_RIGHT,
                RIGHT_DOWN,
                DOWN_LEFT,
                LEFT_UP);
        operator fun contains(t: TubeState): Boolean {
            return states.contains(t)
        }
    }

    companion object {

        val ALL_STATES = mutableListOf<TubeState>()

        fun getState(dirs: Array<Boolean>): TubeState {
            return values().first { it.connections.contentEquals(dirs) }
        }
    }
}