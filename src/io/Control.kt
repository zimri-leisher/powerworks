package io

enum class Control {
    UP, DOWN, LEFT, RIGHT,
    /* GUI clicks, level clicks, etc */
    INTERACT,
    /* For whatever you need whenever you need it */
    DEBUG
}