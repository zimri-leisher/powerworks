package level.update

import crafting.Recipe
import level.Level
import level.LevelManager
import level.block.CrafterBlock
import network.BlockReference
import player.Player
import serialization.Id
import java.util.*

class CrafterBlockSelectRecipe(
        @Id(2) val crafterReference: BlockReference,
        @Id(3) val recipe: Recipe?
) : LevelUpdate(LevelModificationType.SELECT_CRAFTER_RECIPE) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), null)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level) = crafterReference.value != null && crafterReference.value!!.level == level

    override fun act(level: Level) {
        val crafter = crafterReference.value!! as CrafterBlock
        crafter.recipe = recipe
    }

    override fun actGhost(level: Level) {
        // TODO graphically change it but don't actually?
    }

    override fun cancelActGhost(level: Level) {
        // TODO undo graphical change
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is CrafterBlockSelectRecipe) {
            return false
        }

        if (other.crafterReference.value != crafterReference.value) {
            return false
        }

        if (other.recipe != recipe) {
            return false
        }

        return true
    }

}