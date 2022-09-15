package level.update

import crafting.Recipe
import level.LevelManager
import level.block.CrafterBlock
import network.BlockReference
import player.Player
import serialization.Id
import java.util.*

/**
 * A level update for changing the [Recipe] of a [CrafterBlock].
 */
class CrafterBlockSelectRecipe(
        /**
         * A reference to the [CrafterBlock] to modify.
         */
        @Id(2) val crafterReference: BlockReference,
        /**
         * The recipe to set the [CrafterBlock]'s recipe to.
         */
        @Id(3) val recipe: Recipe?
) : GameUpdate(LevelUpdateType.CRAFTER_SELECT_RECIPE) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), null)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct() = crafterReference.value != null && crafterReference.value!!.level == level

    override fun act() {
        val crafter = crafterReference.value!! as CrafterBlock
        crafter.recipe = recipe
    }

    override fun actGhost() {
        // TODO graphically change it but don't actually?
    }

    override fun cancelActGhost() {
        // TODO undo graphical change
    }

    override fun equivalent(other: GameUpdate): Boolean {
        if (other !is CrafterBlockSelectRecipe) {
            return false
        }

        if (other.crafterReference.value == null || other.crafterReference.value !== crafterReference.value) {
            return false
        }

        if (other.recipe != recipe) {
            return false
        }

        return true
    }

    override fun resolveReferences() {
        crafterReference.value = crafterReference.resolve()
    }
}