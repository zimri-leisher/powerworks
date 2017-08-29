package io

enum class Control {
    UP, DOWN, LEFT, RIGHT,
    /* GUI clicks/scrolls, level clicks, etc */
    INTERACT, SCROLL_UP, SCROLL_DOWN,
    /* For whatever you need whenever you need it */
    DEBUG
}