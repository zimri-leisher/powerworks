package crafting

/**
 * All the methods of crafting
 * Used for limiting available recipes
 */
interface Crafter {

    val crafterType: Int

    companion object {
        val PLAYER = 1
        val ITEM_CRAFTER = 2
        val ROBOT_CRAFTER = 3
    }
}