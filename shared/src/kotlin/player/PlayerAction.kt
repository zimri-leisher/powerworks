package player

import behavior.Behavior
import behavior.BehaviorTree
import crafting.Recipe
import item.ItemType
import level.*
import level.block.Block
import level.entity.Entity
import network.*
import resource.ResourceList
import resource.ResourceNodeBehavior
import serialization.Id
import java.util.*

sealed class PlayerAction(
        @Id(1)
        val owner: Player) {
    abstract fun verify(): Boolean
    abstract fun act(): Boolean
    abstract fun actGhost()
    abstract fun cancelActGhost()
}

class ErrorAction : PlayerAction(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID())) {

    override fun verify(): Boolean {
        return false
    }

    override fun act(): Boolean {
        return false
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }
}

class TransferItemsBetweenBlock(owner: Player,
                                @Id(2)
                                val blockReference: BlockReference,
                                @Id(3)
                                val add: Boolean,
                                @Id(4)
                                val resources: ResourceList,
                                @Id(5)
                                val containerId: UUID) : PlayerAction(owner) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()),
            BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), false, ResourceList(), UUID.randomUUID())

    override fun verify(): Boolean {
        val block = blockReference.value!! as Block
        val container = block.containers.firstOrNull { it.id == containerId } ?: return false
        if (resources.keys.any { it !is ItemType }) {
            return false
        }
        return if (add) {
            container.canAdd(resources) && owner.brainRobot.inventory.canRemove(resources)
        } else {
            container.canRemove(resources) && owner.brainRobot.inventory.canAdd(resources)
        }
    }

    override fun act(): Boolean {
        blockReference.level.modify(ModifyBlockContainer(blockReference, containerId, resources, add))
        for ((type, quantity) in resources) {
            blockReference.level.modify(ModifyBrainRobotInv(owner.brainRobot.toReference() as MovingObjectReference, type as ItemType, quantity))
        }
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

}

class CreateEntityGroup(owner: Player,
                        @Id(2)
                        val entities: List<MovingObjectReference>) : PlayerAction(owner) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), listOf())

    override fun verify(): Boolean {
        return entities.all { it.value != null }
    }

    override fun act(): Boolean {
        if (entities.isEmpty()) {
            return true
        }
        if (entities.any { it.value == null }) {
            println("Reference to an entity was null")
            return false
        }
        if (entities.map { it.level }.distinct().size != 1) {
            println("Entities were in more than one level")
            return false
        }
        entities.first().level.modify(AddEntititesToGroup(entities))
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

}

class PlaceLevelObject(owner: Player,
                       @Id(2)
                       val levelObjType: LevelObjectType<*>,
                       @Id(3)
                       val xPixel: Int,
                       @Id(4)
                       val yPixel: Int,
                       @Id(5)
                       val rotation: Int,
                       @Id(6)
                       val level: Level) : PlayerAction(owner) {

    private var temporaryGhostObject: GhostLevelObject? = null

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), LevelObjectType.ERROR, 0, 0, 0, LevelManager.EMPTY_LEVEL)

    override fun verify(): Boolean {
        if (levelObjType.itemForm == null) {
            println("no item form")
            return false
        }
        if (!owner.brainRobot.inventory.contains(levelObjType.itemForm!!)) {
            println("doesnt contain item form")
            return false
        }
        if (!level.canAdd(levelObjType, xPixel, yPixel)) {// TODO include rotation
            println("can't add to level $levelObjType $xPixel $yPixel")
            return false
        }
        return true
    }

    override fun act(): Boolean {
        if (levelObjType.itemForm == null) {
            println("Should have been an item form of $levelObjType but wasn't")
            return false
        }
        val removeItem = ModifyBrainRobotInv(owner.brainRobot.toReference() as MovingObjectReference, levelObjType.itemForm!!, -1)
        if (!level.modify(removeItem)) {
            println("Should have been able to remove items from brainrobot but wasn't")
            return false
        }
        val newInstance = levelObjType.instantiate(xPixel, yPixel, rotation)
        if (!level.add(newInstance)) {
            println("Should have been able to add block to level but wasn't")
            return false
        }
        return true
    }

    override fun actGhost() {
        temporaryGhostObject = GhostLevelObject(levelObjType, xPixel, yPixel, rotation)
        level.add(temporaryGhostObject!!)
    }

    override fun cancelActGhost() {
        level.remove(temporaryGhostObject!!)
        temporaryGhostObject = null
    }
}

class RemoveLevelObjectAction(owner: Player,
                              @Id(2)
                              val references: List<LevelObjectReference>) : PlayerAction(owner) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), listOf())

    override fun verify(): Boolean {
        for (reference in references) {
            if (reference.value == null) {
                return false
            }
        }
        return true
    }

    override fun act(): Boolean {
        for (reference in references) {
            if (reference.value == null) {
                println("Reference should have been able to be resolved, but wasn't")
                return false
            }
            val value = reference.value!!
            value.level.remove(value)
            if (value.type.itemForm != null) {
                value.level.modify(ModifyBrainRobotInv(owner.brainRobot.toReference() as MovingObjectReference, value.type.itemForm!!, 1))
            }
        }
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun toString(): String {
        return "Remove level objects: ${references.joinToString()}"
    }
}

class ControlEntityAction(owner: Player,
                          @Id(2)
                          val entityReferences: List<MovingObjectReference>,
                          @Id(3)
                          val behavior: BehaviorTree,
                          @Id(4)
                          val arg: Any?) : PlayerAction(owner) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), listOf(), Behavior.ERROR, null)

    override fun verify(): Boolean {
        for (reference in entityReferences) {
            if (reference.value == null) {
                return false
            }
        }
        return true
    }

    override fun act(): Boolean {
        for (reference in entityReferences) {
            if (reference.value == null) {
                println("Reference should have been able to be resolved, but wasn't")
                return false
            }
            if (reference.value !is Entity) {
                println("Reference was not to an entity but should have been")
                return false
            }
            (reference.value as Entity).behavior.run(behavior, argument = arg)
        }
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

}

class SelectCrafterRecipeAction(owner: Player,
                                @Id(2)
                                val crafter: BlockReference,
                                @Id(3)
                                val recipe: Recipe?) : PlayerAction(owner) {
    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), null)

    override fun verify(): Boolean {
        return crafter.value != null
    }

    override fun act(): Boolean {
        if (crafter.value == null) {
            println("Reference should have been able to be resolved, but wasn't")
            return false
        }
        crafter.level.modify(SelectCrafterRecipe(crafter, recipe))
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }
}

class EditResourceNodeBehaviorAction(owner: Player,
                                     @Id(2)
                                     val node: ResourceNodeReference,
                                     @Id(3)
                                     val newBehavior: ResourceNodeBehavior) : PlayerAction(owner) {
    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), ResourceNodeReference(0, 0, LevelManager.EMPTY_LEVEL, UUID.randomUUID()), ResourceNodeBehavior.EMPTY_BEHAVIOR)

    override fun verify(): Boolean {
        return node.value != null
    }

    override fun act(): Boolean {
        if (node.value == null) {
            println("Reference should have been able to be resolved but wasn't")
            return false
        }
        node.value!!.behavior = newBehavior
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

}

class PickUpDroppedItemAction(
        owner: Player,
        @Id(2)
        val droppedItemReferences: List<DroppedItemReference>) : PlayerAction(owner) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), listOf())

    override fun verify(): Boolean {
        for (reference in droppedItemReferences) {
            if (reference.value == null) {
                return false
            }
        }
        return true
    }

    override fun act(): Boolean {
        for (reference in droppedItemReferences) {
            if (reference.value == null) {
                println("Reference should have been able to be resolved but wasn't")
                return false
            }
            val value = reference.value!!
            if (value !is DroppedItem) {
                println("Reference should have been to a droppped item but wasn't")
                return false
            }
            owner.brainRobot.inventory.add(value.itemType, value.quantity)
            value.level.remove(value)
        }
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }
}