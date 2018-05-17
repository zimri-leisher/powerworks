package level

class Hitbox private constructor(val xStart: Int, val yStart: Int, val width: Int, val height: Int) {
    companion object {
        val TILE = Hitbox(0, 0, 16, 16)
        val TILE2X2 = Hitbox(0, 0, 32, 32)
        val DROPPED_ITEM = Hitbox(0, 0, 8, 8)
        val NONE = Hitbox(0, 0, 0, 0)

        fun rotate(h: Hitbox, rotation: Int): Hitbox {
            return when(rotation) {
                1 -> Hitbox(h.yStart, -h.xStart, h.height, h.width)
                2 -> Hitbox(-h.xStart, -h.yStart, h.width, h.height)
                3 -> Hitbox(-h.yStart, h.xStart, h.height, h.width)
                else -> h
            }

        }
    }
}