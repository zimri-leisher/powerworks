package level

import serialization.Id

class Hitbox private constructor(
        @Id(1)
        val xStart: Int,
        @Id(2)
        val yStart: Int,
        @Id(3)
        val width: Int,
        @Id(4)
        val height: Int) {

    private constructor() : this(0, 0, 0, 0)

    companion object {
        val BULLET = Hitbox(0, 0, 8, 4)
        val TILE = Hitbox(0, 0, 16, 16)
        val TILE3X3 = Hitbox(0, 0, 48, 48)
        val TILE2X2 = Hitbox(0, 0, 32, 32)
        val TILE2X1 = Hitbox(0, 0, 32, 16)
        val STANDARD_ROBOT = Hitbox(3, 0, 16, 16)
        val DROPPED_ITEM = Hitbox(0, 0, 8, 8)
        val NONE = Hitbox(0, 0, 0, 0)

        fun rotate(h: Hitbox, rotation: Int): Hitbox {
            if (h == NONE)
                return NONE
            return with(h) {
                when (rotation % 4) {
                    1 -> Hitbox(h.yStart, -h.xStart, h.height, h.width)
                    2 -> Hitbox(-h.xStart, -h.yStart, h.width, h.height)
                    3 -> Hitbox(xStart + (width - height) / 2, yStart + (height - width) / 2, height, width)
                    else -> h
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hitbox

        if (xStart != other.xStart) return false
        if (yStart != other.yStart) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = xStart
        result = 31 * result + yStart
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}