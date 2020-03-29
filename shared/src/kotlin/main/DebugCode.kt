package main

/**
 * Codes used for the displaying of information that is not necessary for normal gameplay but helpful for developers
 *
 * To enable one, use Game.currentDebugCode
 */
enum class DebugCode {
    /**
     * Default, does nothing
     */
    NONE,
    /**
     * Mouse position on screen and level
     */
    POSITION_INFO,
    /**
     * Information about tube blocks and groups
     */
    TUBE_INFO,
    /**
     * Information about pipe blocks and groups
     */
    PIPE_INFO,
    /**
     * Information about the screen elements at the mouse
     */
    SCREEN_INFO,
    /**
     * Hitboxes of all level objects
     */
    RENDER_HITBOXES,
    /**
     * Chunk boundaries and other chunk information
     */
    CHUNK_INFO,
    /**
     * Information about the resources nodes at the mouse position in the level
     */
    RESOURCE_NODES_INFO
}