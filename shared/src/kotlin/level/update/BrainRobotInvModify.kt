package level.update

import item.ItemType
import level.Level
import level.LevelManager
import level.entity.robot.BrainRobot
import network.MovingObjectReference
import player.Player
import resource.ResourceList
import serialization.Id
import java.util.*
import kotlin.math.absoluteValue

class BrainRobotInvModify(
        @Id(4) val brainReference: MovingObjectReference,
        @Id(2) val itemType: ItemType,
        @Id(3) val quantity: Int
) : LevelUpdate(LevelModificationType.GIVE_BRAIN_ROBOT_ITEM) {

    private constructor() : this(MovingObjectReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), ItemType.ERROR, 0)

    override val playersToSendTo: Set<Player>?
        get() = setOf(((brainReference.value ?: brainReference.resolve()!!) as BrainRobot).player)

    override fun canAct(level: Level): Boolean {
        if (brainReference.value == null || brainReference.value !is BrainRobot) {
            return false
        }
        val value = brainReference.value as BrainRobot
        val resources = ResourceList(itemType to quantity.absoluteValue)
        if (quantity < 0 && !value.inventory.canRemove(resources)) {
            return false
        }
        if (quantity > 0 && !value.inventory.canAdd(resources)) {
            return false
        }
        return true
    }

    override fun act(level: Level) {
        val value = brainReference.value as BrainRobot
        val resources = ResourceList(itemType to quantity.absoluteValue)
        if (quantity < 0) {
            value.inventory.remove(resources)
        } else if (quantity > 0) {
            value.inventory.add(resources)
        }
    }

    override fun actGhost(level: Level) {
        // TODO fake add?
    }

    override fun cancelActGhost(level: Level) {
        // TODO cancel fake add?
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is BrainRobotInvModify) {
            return false
        }

        if (other.brainReference.value != brainReference.value) {
            return false
        }

        return other.itemType == itemType && other.quantity == quantity
    }

}