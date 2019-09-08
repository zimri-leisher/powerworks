package level

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag

class Hitbox private constructor(
        @Tag(1)
        val xStart: Int,
        @Tag(2)
        val yStart: Int,
        @Tag(3)
        val width: Int,
        @Tag(4)
        val height: Int) {
    companion object {
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
}