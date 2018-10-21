package level

class Hitbox private constructor(val xStart: Int, val yStart: Int, val width: Int, val height: Int) {
    companion object {
        val TILE = Hitbox(0, 0, 16, 16)
        val TILE3X3 = Hitbox(0, 0, 48, 48)
        val TILE2X2 = Hitbox(0, 0, 32, 32)
        val TILE2X1 = Hitbox(0, 0, 32, 16)
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