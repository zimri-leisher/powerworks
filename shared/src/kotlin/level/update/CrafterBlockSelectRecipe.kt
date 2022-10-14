package level.update

import crafting.Recipe
import level.Level
import level.LevelManager
import level.block.CrafterBlock
import level.block.CrafterBlockType
import network.BlockReference
import player.Player
import serialization.AsReference
import serialization.Id
import java.util.*

/**
 * A level update for changing the [Recipe] of a [CrafterBlock].
 */
class CrafterBlockSelectRecipe(
    /**
     * A reference to the [CrafterBlock] to modify.
     */
    @AsReference
    @Id(2) val crafter: CrafterBlock,
    /**
     * The recipe to set the [CrafterBlock]'s recipe to.
     */
    @Id(3) val recipe: Recipe?,
    level: Level
) : LevelUpdate(LevelUpdateType.CRAFTER_SELECT_RECIPE, level) {

    private constructor() : this(CrafterBlock(CrafterBlockType.ITEM_CRAFTER, 0, 0), null, LevelManager.EMPTY_LEVEL)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct() = crafter.level == level

    override fun act() {
        crafter.recipe = recipe
    }

    override fun actGhost() {
        // TODO graphically change it but don't actually?
    }

    override fun cancelActGhost() {
        // TODO undo graphical change
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is CrafterBlockSelectRecipe) {
            return false
        }

        if (other.crafter !== crafter) {
            return false
        }

        if (other.recipe != recipe) {
            return false
        }

        return true
    }
}