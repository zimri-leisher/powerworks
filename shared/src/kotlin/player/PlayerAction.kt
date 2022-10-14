package player

import behavior.Behavior
import behavior.BehaviorTree
import crafting.Recipe
import level.*
import level.block.CrafterBlock
import level.block.FarseekerBlock
import level.entity.Entity
import level.update.*
import network.*
import player.team.TeamPermission
import resource.*
import serialization.AsReference
import serialization.Id
import java.util.*

sealed class PlayerAction(
    @Id(1)
    val owner: Player
) {

    var ghostActions: List<LevelUpdate>? = null

    /**
     * @return `true` if the action is possible for [owner] at the current game state, `false` otherwise
     */
    abstract fun verify(): Boolean

    /**
     * Takes the action this [PlayerAction] represents. Implementations of this method have no responsibility to
     * communicate actions over the network, all of that should be handled in [LevelUpdate] instances
     * @return `true` if the action was successful, `false` otherwise
     */
    abstract fun getUpdates(): List<LevelUpdate>

    fun act() = getUpdates().all { it.level.modify(it) }

    /**
     * Visually fakes the action that would be taken if [act] were called. Implementations of this method should make no
     * change to the game state. This is only so that there is instantaneous client visual feedback
     */
    fun actGhost() {
        ghostActions = getUpdates()
        ghostActions!!.forEach { it.actGhost() }
    }

    /**
     * Cancel the fake action taken by [actGhost]
     */
    fun cancelActGhost() {
        ghostActions?.forEach { it.cancelActGhost() }
        ghostActions = null
    }
}

class ActionError : PlayerAction(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID())) {

    override fun verify(): Boolean {
        return false
    }

    override fun getUpdates() = emptyList<LevelUpdate>()
}

class ActionDoResourceTransaction(
    owner: Player,
    @Id(2)
    val transaction: ResourceTransaction
) : PlayerAction(owner) {

    private constructor() : this(
        Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()),
        ResourceTransaction(SourceContainer(), SourceContainer(), emptyResourceList())
    )

    override fun verify(): Boolean {
        if (!transaction.isValid()) {
            return false
        }
        // want to make sure that this is between two containers that the player is nearby
        // get nodes from the containers
        // if they are within some distance, do the action
        // TODO
        if (!owner.resourceTransactionExecutor.canExecute(transaction)) {
            return false
        }
        return true
    }

    override fun getUpdates(): List<LevelUpdate> {
        return listOf(ResourceTransactionExecute(transaction, owner.resourceTransactionExecutor, transaction.src.level))
    }
}

/**
 * Creates an [level.entity.EntityGroup] consisting of the given [entities]
 */
class ActionEntityCreateGroup(
    owner: Player,
    @Id(2)
    // TODO list reference
    val entities: List<MovingObjectReference>
) : PlayerAction(owner) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), listOf())

    override fun verify(): Boolean {
        if (entities.isEmpty()) {
            return true
        }
        if (entities.any { it.value == null }) {
            println("Entity reference in group create was null")
            return false
        }
        if (!entities.all { it.value!!.team.check(TeamPermission.CONTROL_ENTITIES, owner) }) {
            return false
        }
        if (entities.map { it.level }.distinct().size != 1) {
            println("Entities were in more than one level")
            return false
        }
        return true
    }

    override fun getUpdates(): List<LevelUpdate> {
        return listOf(EntityAddToGroup(entities, entities.first().level))
    }
}

class ActionFarseekerBlockSetLevel(
    owner: Player,
    @Id(2)
    @AsReference
    val block: FarseekerBlock,
    @Id(3)
    val level: Level
) : PlayerAction(owner) {

    private constructor() : this(
        Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()),
        BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0),
        LevelManager.EMPTY_LEVEL
    )

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

    override fun getUpdates(): List<LevelUpdate> {
        return listOf(FarseekerBlockSetDestinationLevel(blockReference))
    }

    override fun act() {
        val level = blockReference.value!!.level
        level.modify(FarseekerBlockSetDestinationLevel(blockReference, this.level))
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }
}

/**
 * Places a [PhysicalLevelObject] of the given [levelObjType] at the given [x], [y] and with the given [rotation] in the
 * given [level].
 */
class ActionLevelObjectPlace(
    owner: Player,
    @Id(2)
    val levelObjType: PhysicalLevelObjectType<*>,
    @Id(3)
    val x: Int,
    @Id(4)
    val y: Int,
    @Id(5)
    val rotation: Int,
    @Id(6)
    val level: Level
) : PlayerAction(owner) {

    private var temporaryGhostObject: GhostLevelObject? = null

    private constructor() : this(
        Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()),
        PhysicalLevelObjectType.ERROR,
        0,
        0,
        0,
        LevelManager.EMPTY_LEVEL
    )

    override fun verify(): Boolean {
        if (levelObjType.itemForm == null) {
            println("Should have been an item form of $levelObjType but wasn't")
            return false
        }
        if (!owner.brainRobot.inventory.canRemove(levelObjType.itemForm!!)) {
            println("doesnt contain item form")
            return false
        }
        if (level.getCollisionsWith(levelObjType.hitbox, x, y).any()) {
            println("can't add to level $levelObjType $x $y")
            return false
        }
        return true
    }

    override fun act() {
        val removeItem =
            LevelObjectResourceContainerModify(owner.brainRobot, false, resourceListOf(levelObjType.itemForm!! to 1))
        level.modify(removeItem)
        val newInstance = levelObjType.instantiate(x, y, rotation)
        newInstance.team = owner.team
        level.add(newInstance)
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
 * Removes the [PhysicalLevelObject]s specified by the given [references] from their respective [Level]s, and adds their item forms,
 * if they exist, to the [owner]'s brain robot inventory
 */
class ActionLevelObjectRemove(
    owner: Player,
    @Id(2)
    val references: List<LevelObjectReference>
) : PlayerAction(owner) {

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

    override fun act() {
        val resourcesToGiveToPlayer = mutableResourceListOf()
        // 99 percent of the time all the levels of the objects will be the same level
        // so we make a lil optimization
        for (reference in references) {
            val value = reference.value!!
            value.level.remove(value)
            if (value.type.itemForm != null) {
                resourcesToGiveToPlayer.put(value.type.itemForm!!, 1)
            }
        }
        for ((type, quantity) in resourcesToGiveToPlayer) {
            references.first().level.modify(
                LevelObjectResourceContainerModify(
                    owner.brainRobot,
                    true,
                    resourceListOf(type to quantity)
                )
            )
        }
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
class ActionControlEntity(
    owner: Player,
    @Id(2)
    val entityReferences: List<MovingObjectReference>,
    @Id(3)
    val behavior: BehaviorTree,
    @Id(4)
    val arg: Any?
) : PlayerAction(owner) {

    private constructor() : this(
        Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()),
        listOf(),
        Behavior.ERROR,
        null
    )

    override fun verify(): Boolean {
        for (reference in entityReferences) {
            if (reference.value == null || reference.value !is Entity) {
                println("Control entity reference is null or not entity (${reference.value})")
                return false
            }
            if (!reference.value!!.team.check(TeamPermission.CONTROL_ENTITIES, owner)) {
                println("no permission SFDJKLSDF")
                return false
            }
        }
        return true
    }

    override fun act() {
        for (reference in entityReferences) {
            (reference.value as Entity).behavior.run(behavior, argument = arg)
        }
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

}

/**
 * Changes the recipe of the given [crafter] to [recipe]
 */
class ActionSelectCrafterRecipe(
    owner: Player,
    @Id(2)
    val crafter: BlockReference,
    @Id(3)
    val recipe: Recipe?
) : PlayerAction(owner) {
    private constructor() : this(
        Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()),
        BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0),
        null
    )

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

    override fun act() {
        crafter.level.modify(CrafterBlockSelectRecipe(crafter, recipe))
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }
}

/**
 * Changes the [ResourceNodeBehavior] of the given [node] to [behavior]
 */
class ActionEditResourceNodeBehavior(
    owner: Player,
    @Id(2)
    val node: ResourceNodeReference,
    @Id(3)
    val behavior: ResourceNodeBehavior
) : PlayerAction(owner) {
    private constructor() : this(
        Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()),
        ResourceNodeReference(0, 0, LevelManager.EMPTY_LEVEL, UUID.randomUUID()),
        ResourceNodeBehavior.EMPTY_BEHAVIOR
    )

    override fun verify(): Boolean {
        if (node.value == null) {
            return false
        }
        if (!node.value!!.team.check(TeamPermission.MODIFY_RESOURCE_NODE, owner)) {
            return false
        }
        return true
    }

    override fun act() {
        node.level.modify(ResourceNodeBehaviorEdit(node, behavior))
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }
}