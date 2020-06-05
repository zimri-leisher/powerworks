package player

import behavior.Behavior
import behavior.BehaviorTree
import crafting.Recipe
import level.*
import level.block.CrafterBlock
import level.entity.Entity
import network.*
import resource.ResourceNodeBehavior
import serialization.Id
import java.util.*

sealed class PlayerAction(
        @Id(1)
        val owner: Player) {
    abstract fun verify(): Boolean
    abstract fun act(): Boolean
    abstract fun actTransient()
    abstract fun cancelActTransient()
}

class ErrorAction : PlayerAction(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID())) {

    override fun verify(): Boolean {
        return false
    }

    override fun act(): Boolean {
        return false
    }

    override fun actTransient() {
    }

    override fun cancelActTransient() {
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
        val removeItem = ModifyBrainRobotInv(owner.brainRobot.toReference(), levelObjType.itemForm!!, -1)
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

    override fun actTransient() {
        temporaryGhostObject = GhostLevelObject(levelObjType, xPixel, yPixel, rotation)
        level.add(temporaryGhostObject!!)
    }

    override fun cancelActTransient() {
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
                value.level.modify(ModifyBrainRobotInv(owner.brainRobot.toReference(), value.type.itemForm!!, 1))
            }
        }
        return true
    }

    override fun actTransient() {
    }

    override fun cancelActTransient() {
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
            if(reference.value !is Entity) {
                println("Reference was not to an entity but should have been")
                return false
            }
            (reference.value as Entity).runBehavior(behavior, argument = arg)
        }
        return true
    }

    override fun actTransient() {
    }

    override fun cancelActTransient() {
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

    override fun actTransient() {
    }

    override fun cancelActTransient() {
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

    override fun actTransient() {
    }

    override fun cancelActTransient() {
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
            if(value !is DroppedItem) {
                println("Reference should have been to a droppped item but wasn't")
                return false
            }
            owner.brainRobot.inventory.add(value.itemType, value.quantity)
            value.level.remove(value)
        }
        return true
    }

    override fun actTransient() {
    }

    override fun cancelActTransient() {
    }
}