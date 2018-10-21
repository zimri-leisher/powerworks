package crafting

/**
 * All the methods of crafting.
 * Used for limiting available recipes. You don't want a player to be able to craft a robot, so you say that the robot's
 * recipe can only be made by a Crafter.Type.ROBOT
 */
interface Crafter {

    val crafterType: Type

    enum class Type {
        DEFAULT,
        ITEM,
        ROBOT
    }
}