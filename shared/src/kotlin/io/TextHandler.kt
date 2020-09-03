package io

/**
 * Handles key typing events. Use [InputManager.textHandler]
 */
interface TextHandler {
    fun handleChar(c: Char)
}