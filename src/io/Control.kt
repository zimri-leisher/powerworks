package io

enum class Control {
    UP, DOWN, LEFT, RIGHT,
    /* GUI clicks/scrolls, level clicks, etc */
    INTERACT, SCROLL_UP, SCROLL_DOWN,
    /* For whatever you need whenever you need it */
    DEBUG,
    TOGGLE_RENDER_HITBOXES,
    TOGGLE_CHUNK_INFO,
    /* Hotbar slots */
    SLOT_1, SLOT_2, SLOT_3, SLOT_4, SLOT_5, SLOT_6,SLOT_7, SLOT_8,
    TAKE_SCREENSHOT,
    TOGGLE_VIEW_CONTROLS,
    /* Testing controls */
    GIVE_TEST_ITEM
}