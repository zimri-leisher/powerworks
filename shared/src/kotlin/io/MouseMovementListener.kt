package io

/**
 * To use, add to [InputManager.mouseMovementListeners]
 */
interface MouseMovementListener {
    /**
     * Called whenever the mouse is moved.
     *
     * This object must be added to [InputManager.mouseMovementListeners] for this to work
     */
    fun onMouseMove(pXPixel: Int, pYPixel: Int)
}