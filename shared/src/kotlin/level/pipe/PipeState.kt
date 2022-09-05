package level.pipe

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import graphics.Image
import graphics.ImageCollection

enum class PipeState(
    val connections: Array<Boolean>,
    val closedEnds: Array<Boolean> = arrayOf(false, false, false, false)
) {
    NONE(
        arrayOf(
            false, false, false, false
        ), arrayOf(
            true, false, true, false
        )
    ),
    UP_ONLY(
        arrayOf(
            true, false, false, false
        ), arrayOf(
            false, false, true, false
        )
    ),
    RIGHT_ONLY(
        arrayOf(
            false, true, false, false
        ), arrayOf(
            false, false, false, true
        )
    ),
    DOWN_ONLY(
        arrayOf(
            false, false, true, false
        ), arrayOf(
            true, false, false, false
        )
    ),
    LEFT_ONLY(
        arrayOf(
            false, false, false, true
        ), arrayOf(
            false, true, false, false
        )
    ),
    UP_DOWN(
        arrayOf(
            true, false, true, false
        )
    ),
    RIGHT_LEFT(
        arrayOf(
            false, true, false, true
        )
    ),
    UP_RIGHT(
        arrayOf(
            true, true, false, false
        )
    ),
    RIGHT_DOWN(
        arrayOf(
            false, true, true, false
        )
    ),
    DOWN_LEFT(
        arrayOf(
            false, false, true, true
        )
    ),
    LEFT_UP(
        arrayOf(
            true, false, false, true
        )
    ),
    LEFT_UP_RIGHT(
        arrayOf(
            true, true, false, true
        )
    ),
    UP_RIGHT_DOWN(
        arrayOf(
            true, true, true, false
        )
    ),
    RIGHT_DOWN_LEFT(
        arrayOf(
            false, true, true, true
        )
    ),
    DOWN_LEFT_UP(
        arrayOf(
            true, false, true, true
        )
    ),
    ALL(
        arrayOf(
            true, true, true, true
        )
    );

    enum class Group(vararg val states: PipeState) {
        INTERSECTION(
            UP_RIGHT,
            RIGHT_DOWN,
            DOWN_LEFT,
            LEFT_UP,
            LEFT_UP_RIGHT,
            UP_RIGHT_DOWN,
            RIGHT_DOWN_LEFT,
            DOWN_LEFT_UP,
            ALL
        ),
        NOT_INTERSECTION(
            NONE,
            UP_ONLY,
            RIGHT_ONLY,
            DOWN_ONLY,
            LEFT_ONLY,
            UP_DOWN,
            RIGHT_LEFT
        ),
        CORNER(
            UP_RIGHT,
            RIGHT_DOWN,
            DOWN_LEFT,
            LEFT_UP
        );

        operator fun contains(t: PipeState): Boolean {
            return states.contains(t)
        }
    }

    companion object {

        private fun <T> Array<T>.get(bool: Boolean) = if (bool) get(1) else get(0)
        private fun <T> Array<T>.set(bool: Boolean, v: T) = if (bool) set(1, v) else set(0, v)

        val tensor: Array<Array<Array<Array<PipeState>>>>

        init {
            tensor = arrayOf(
                arrayOf(
                    arrayOf(
                        arrayOf(NONE, NONE),
                        arrayOf(NONE, NONE)
                    ),
                    arrayOf(
                        arrayOf(NONE, NONE),
                        arrayOf(NONE, NONE)
                    )
                ),
                arrayOf(
                    arrayOf(
                        arrayOf(NONE, NONE),
                        arrayOf(NONE, NONE)
                    ),
                    arrayOf(
                        arrayOf(NONE, NONE),
                        arrayOf(NONE, NONE)
                    )
                )
            )
            for (up in listOf(true, false)) {
                for (right in listOf(true, false)) {
                    for (down in listOf(true, false)) {
                        for (left in listOf(true, false)) {
                            val matchingState = PipeState.values().first {
                                it.connections[0] == up &&
                                        it.connections[1] == right &&
                                        it.connections[2] == down &&
                                        it.connections[3] == left
                            }
                            tensor.get(up).get(right).get(down).set(left, matchingState)
                        }
                    }
                }
            }
        }

        private fun Boolean.toInt() = if (this) 1 else 0

        fun getState(up: Boolean, right: Boolean, down: Boolean, left: Boolean): PipeState {
            return tensor[up.toInt()][right.toInt()][down.toInt()][left.toInt()]
        }
    }
}