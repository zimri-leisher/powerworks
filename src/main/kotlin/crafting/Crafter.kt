package crafting

/**
 * All the methods of crafting
 * Used for limiting available recipes
 */
interface Crafter {

    val crafterType: Type

    enum class Type {
        DEFAULT,
        ITEM,
        ROBOT
    }
}