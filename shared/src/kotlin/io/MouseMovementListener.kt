package io

/**
 * To use, add to [Mouse.mouseMovementListeners]
 */
interface MouseMovementListener {
    /**
     * Called whenever the mouse is moved.
     *
     * This object must be added to [Mouse.mouseMovementListeners] for this to work
     */
    fun onMouseMove(prevX: Int, prevY: Int)
}