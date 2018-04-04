package mod

/**
 * A Powerworks mod (modification).
 * Allows for the user to create content for the game and share that content in the form of a .jar file with others
 */
interface Mod {

    /**
     * Called during the loading of the game
     */
    fun initialize()

    /**
     * Called during the shutting down of the game
     */
    fun shutdown()
}