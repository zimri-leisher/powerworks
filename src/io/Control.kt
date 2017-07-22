package io

/**
 * The input type to block if the handler was used
 */
enum class BlockingType(val value: Byte) {
    NONE(0b00000000), KEYBOARD(0b00000001), MOUSE(0b00000010), MOUSEWHEEL(0b00000100)
}

enum class Control(public val blocking: BlockingType = BlockingType.NONE, public val repeatDelayMs: Int = 0) {
    UP, DOWN, LEFT, RIGHT,
}