package level

enum class Hitbox(val xStart: Int, val yStart: Int, val width: Int, val height: Int) {
    TILE(0, 0, 16, 16),
    NONE(0, 0, 0, 0)
}