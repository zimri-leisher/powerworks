package io

enum class Control {
    UP, DOWN, LEFT, RIGHT,
    /* GUI clicks/scrolls, level clicks, etc */
    INTERACT, SCROLL_UP, SCROLL_DOWN,
    /* For whatever you need whenever you need it */
    DEBUG,
    /* Hotbar slots */
    SLOT_1, SLOT_2, SLOT_3, SLOT_4, SLOT_5, SLOT_6,SLOT_7, SLOT_8
}