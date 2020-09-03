package player

import behavior.Behavior
import behavior.BehaviorTree
import crafting.Recipe
import item.ItemType
import level.*
import level.block.Block
import level.block.CrafterBlock
import level.block.FarseekerBlock
import level.entity.Entity
import level.update.*
import network.*
import player.team.TeamPermission
import resource.ResourceList
import resource.ResourceNodeBehavior
import serialization.Id
import java.util.*

sealed class PlayerAction(
        @Id(1)
        val owner: Player) {
    /**
     * @return `true` if the action is possible for [owner] at the current game state, `false` otherwise
     */
    abstract fun verify(): Boolean

    /**
     * Takes the action this [PlayerAction] represents. Implementations of this method have no responsibility to
     * communicate actions over the network, all of that should be handled in [LevelUpdate] instances
     * @return `true` if the action was successful, `false` otherwise
     */
    abstract fun act(): Boolean

    /**
     * Visually fakes the action that would be taken if [act] were called. Implementations of this method should make no
     * change to the game state. This is only so that there is instantaneous client visual feedback
     */
    abstract fun actGhost()

    /**
     * Cancel the fake action taken by [actGhost]
     */
    abstract fun cancelActGhost()
}

class ActionError : PlayerAction(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID())) {

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

/**
 * Adds or removes items from the container matching the given [containerId] in the given [blockReference], and removes or
 * adds them to/from the [owner]'s brain robot inventory
 */
class ActionTransferItemsBetweenBlock(owner: Player,
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
        if (blockReference.value == null) {
            return false
        }
        val block = blockReference.value!! as Block
        if (!block.team.check(TeamPermission.MODIFY_LEVEL_OBJECTS, owner)) {
            return false
        }
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
        blockReference.level.modify(BlockContainerModify(blockReference, containerId, resources, add))
        for ((type, quantity) in resources) {
            blockReference.level.modify(BrainRobotInvModify(owner.brainRobot.toReference() as MovingObjectReference, type as ItemType, quantity))
        }
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

}

/**
 * Creates an [level.entity.EntityGroup] consisting of the given [entities]
 */
class ActionEntityCreateGroup(owner: Player,
                              @Id(2)
                        val entities: List<MovingObjectReference>) : PlayerAction(owner) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), listOf())

    override fun verify(): Boolean {
        if (!entities.all { it.value != null }) {
            return false
        }
        if (!entities.all { it.value!!.team.check(TeamPermission.CONTROL_ENTITIES, owner) }) {
            return false
        }
        return true
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
        entities.first().level.modify(EntityAddToGroup(entities))
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

}

class ActionFarseekerBlockSetLevel(owner: Player,
                                   @Id(2)
                             val blockReference: BlockReference,
                                   @Id(3)
                             val level: Level) : PlayerAction(owner) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), LevelManager.EMPTY_LEVEL)

    override fun verify(): Boolean {
        if (blockReference.value == null) {
            println("Reference was null")
            return false
        }
        if (blockReference.value!! !is FarseekerBlock) {
            println("Reference was not to farseeker")
            return false
        }
        if (level.id !in (blockReference.value!! as FarseekerBlock).availableDestinations.keys) {
            println("Level not available to go to")
            return false
        }
        return true
    }

    override fun act(): Boolean { // TODO clarify what act returns and what verify returns
        val level = blockReference.value?.level ?: return false
        level.modify(FarseekerBlockSetDestinationLevel(blockReference, this.level))
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }
}

/**
 * Places a [LevelObject] of the given [levelObjType] at the given [x], [y] and with the given [rotation] in the
 * given [level].
 */
class ActionLevelObjectPlace(owner: Player,
                             @Id(2)
                       val levelObjType: LevelObjectType<*>,
                             @Id(3)
                       val x: Int,
                             @Id(4)
                       val y: Int,
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
        if (level.getCollisionsWith(levelObjType.hitbox, x, y).any()) {
            println("can't add to level $levelObjType $x $y")
            return false
        }
        return true
    }

    override fun act(): Boolean {
        if (levelObjType.itemForm == null) {
            println("Should have been an item form of $levelObjType but wasn't")
            return false
        }
        val removeItem = BrainRobotInvModify(owner.brainRobot.toReference() as MovingObjectReference, levelObjType.itemForm!!, -1)
        if (!level.modify(removeItem)) {
            println("Should have been able to remove items from brainrobot but wasn't")
            return false
        }
        val newInstance = levelObjType.instantiate(x, y, rotation)
        newInstance.team = owner.team
        if (!level.add(newInstance)) {
            println("Should have been able to add block to level but wasn't")
            return false
        }
        return true
    }

    override fun actGhost() {
        temporaryGhostObject = GhostLevelObject(levelObjType, x, y, rotation)
        level.add(temporaryGhostObject!!)
    }

    override fun cancelActGhost() {
        level.remove(temporaryGhostObject!!)
        temporaryGhostObject = null
    }
}

/**
 * Removes the [LevelObject]s specified by the given [references] from their respective [Level]s, and adds their item forms,
 * if they exist, to the [owner]'s brain robot inventory
 */
class ActionLevelObjectRemove(owner: Player,
                              @Id(2)
                              val references: List<LevelObjectReference>) : PlayerAction(owner) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), listOf())

    override fun verify(): Boolean {
        for (reference in references) {
            if (reference.value == null) {
                return false
            }
            if (!reference.value!!.team.check(TeamPermission.MODIFY_LEVEL_OBJECTS, owner)) {
                return false
            }
        }
        return true
    }

    override fun act(): Boolean {
        val resourcesToGiveToPlayer = ResourceList()
        // 99 percent of the time all the levels of the objects will be the same level
        // so we make a lil optimization
        for (reference in references) {
            if (reference.value == null) {
                println("Reference should have been able to be resolved, but wasn't")
                return false
            }
            val value = reference.value!!
            value.level.remove(value)
            if (value.type.itemForm != null) {
                resourcesToGiveToPlayer.add(value.type.itemForm!!, 1)
            }
        }
        for ((type, quantity) in resourcesToGiveToPlayer) {
            references.first().level.modify(BrainRobotInvModify(owner.brainRobot.toReference() as MovingObjectReference, type as ItemType, quantity))
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

/**
 * Runs the given [behavior] with the given [arg] on the [entityReferences]
 */
class ActionControlEntity(owner: Player,
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
                println("reference is null SDFJSLDFJK")
                return false
            }
            if (!reference.value!!.team.check(TeamPermission.CONTROL_ENTITIES, owner)) {
                println("no permission SFDJKLSDF")
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

/**
 * Changes the recipe of the given [crafter] to [recipe]
 */
class ActionSelectCrafterRecipe(owner: Player,
                                @Id(2)
                                val crafter: BlockReference,
                                @Id(3)
                                val recipe: Recipe?) : PlayerAction(owner) {
    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), null)

    override fun verify(): Boolean {
        if (crafter.value == null) {
            return false
        }
        if (crafter.value !is CrafterBlock) {
            return false
        }
        if (!crafter.value!!.team.check(TeamPermission.MODIFY_LEVEL_OBJECTS, owner)) {
            return false
        }
        return true
    }

    override fun act(): Boolean {
        if (crafter.value == null) {
            println("Reference should have been able to be resolved, but wasn't")
            return false
        }
        crafter.level.modify(CrafterBlockSelectRecipe(crafter, recipe))
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }
}

/**
 * Changes the [ResourceNodeBehavior] of the given [node] to [behavior]
 */
class ActionEditResourceNodeBehavior(owner: Player,
                                     @Id(2)
                                     val node: ResourceNodeReference,
                                     @Id(3)
                                     val behavior: ResourceNodeBehavior) : PlayerAction(owner) {
    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), ResourceNodeReference(0, 0, LevelManager.EMPTY_LEVEL, UUID.randomUUID()), ResourceNodeBehavior.EMPTY_BEHAVIOR)

    override fun verify(): Boolean {
        println("verifying :")
        if (node.value == null) {
            println("node is false")
            return false
        }
        if (!node.value!!.team.check(TeamPermission.MODIFY_RESOURCE_NODE, owner)) {
            println("no permission")
            return false
        }
        return true
    }

    override fun act(): Boolean {
        if (node.value == null) {
            println("Reference should have been able to be resolved but wasn't")
            return false
        }
        println("player action taken")
        node.level.modify(ResourceNodeBehaviorEdit(node, behavior))
        return true
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }
}