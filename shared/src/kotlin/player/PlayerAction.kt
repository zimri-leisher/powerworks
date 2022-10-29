package player

import behavior.Behavior
import behavior.BehaviorTree
import crafting.Recipe
import item.ItemType
import level.*
import level.block.BlockType
import level.block.CrafterBlock
import level.block.CrafterBlockType
import level.block.FarseekerBlock
import level.entity.Entity
import level.entity.EntityType
import level.update.*
import network.*
import player.team.TeamPermission
import resource.*
import serialization.AsReference
import serialization.AsReferenceRecursive
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
        ResourceTransaction(SourceContainer(), SourceContainer(), stackOf(ItemType.ERROR, 1))
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
    @AsReferenceRecursive
    val entities: List<Entity>
) : PlayerAction(owner) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), listOf())

    override fun verify(): Boolean {
        if (entities.isEmpty()) {
            return true
        }
        if (!entities.all { it.team.check(TeamPermission.CONTROL_ENTITIES, owner) }) {
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
        FarseekerBlock(0, 0),
        LevelManager.EMPTY_LEVEL
    )

    override fun verify(): Boolean {
        if (level.id !in block.availableDestinations.keys) {
            println("Level not available to go to")
            return false
        }
        return true
    }

    override fun getUpdates(): List<LevelUpdate> {
        return listOf(FarseekerBlockSetDestinationLevel(block, level, block.level))
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
    @Id(6)
    @AsReference
    val level: Level
) : PlayerAction(owner) {

    private var temporaryGhostObject: GhostLevelObject? = null

    private constructor() : this(
        Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()),
        PhysicalLevelObjectType.ERROR,
        0,
        0,
        LevelManager.EMPTY_LEVEL
    )

    override fun verify(): Boolean {
        if (levelObjType is BlockType<*>) {
            if (!owner.brainRobot.inventory.canRemove(levelObjType.itemForm)) {
                println("doesnt contain item form ${levelObjType.itemForm}")
                return false
            }
        } else if (levelObjType is EntityType<*>) {
            if (!owner.brainRobot.inventory.canRemove(levelObjType.itemForm)) {
                println("doesnt contain item form ${levelObjType.itemForm}")
                return false
            }
        } else {
            return false
        }
        if (level.getCollisionsWith(levelObjType.hitbox, x, y).any()) {
            println("can't add to level $levelObjType $x $y")
            return false
        }
        return true
    }

    override fun getUpdates(): List<LevelUpdate> {
        val itemForm = (levelObjType as? EntityType<*>)?.itemForm ?: (levelObjType as BlockType<*>).itemForm
        val newInstance =
            (levelObjType as? EntityType<*>)?.spawn(x, y) ?: (levelObjType as BlockType<*>).place(x / 16, y / 16)
        val add = LevelObjectAdd(newInstance, level)
        return listOf(
            ResourceTransactionExecute(
                ResourceTransaction(
                    owner.brainRobot.inventory,
                    SourceContainer(),
                    stackOf(itemForm, 1)
                ), ResourceTransactionExecutor.Source, level
            ),
            add
        )
    }
}

/**
 * Removes the [PhysicalLevelObject]s specified by the given [objs] from their respective [Level]s, and adds their item forms,
 * if they exist, to the [owner]'s brain robot inventory
 */
class ActionLevelObjectRemove(
    owner: Player,
    @Id(2)
    @AsReferenceRecursive
    val objs: List<LevelObject>
) : PlayerAction(owner) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), listOf())

    override fun verify(): Boolean {
        for (reference in objs) {
            if (!reference.team.check(TeamPermission.MODIFY_LEVEL_OBJECTS, owner)) {
                return false
            }
        }
        return true
    }

    override fun getUpdates(): List<LevelUpdate> {
        val updates = mutableListOf<LevelUpdate>()
        for (obj in objs) {
            val itemForm = (obj.type as? EntityType<*>)?.itemForm ?: (obj.type as BlockType<*>).itemForm
            updates.add(LevelObjectRemove(obj))
            updates.add(
                ResourceTransactionExecute(
                    ResourceTransaction(
                        SourceContainer(),
                        owner.brainRobot.inventory,
                        stackOf(itemForm, 1)
                    ), ResourceTransactionExecutor.Source, obj.level
                )
            )
        }
        return updates
    }

    override fun toString(): String {
        return "Remove level objects: ${objs.joinToString()}"
    }
}

/**
 * Runs the given [behavior] with the given [arg] on the [entityReferences]
 */
class ActionControlEntity(
    owner: Player,
    @Id(2)
    @AsReferenceRecursive
    val entityReferences: List<Entity>,
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
            if (!reference.team.check(TeamPermission.CONTROL_ENTITIES, owner)) {
                println("no permission to control entity")
                return false
            }
        }
        return true
    }

    override fun getUpdates(): List<LevelUpdate> {
        val updates = mutableListOf<LevelUpdate>()
        for (reference in entityReferences) {
            updates.add(EntityRunBehavior(reference, behavior, arg, reference.level))
        }
        return updates
    }
}

/**
 * Changes the recipe of the given [crafter] to [recipe]
 */
class ActionSelectCrafterRecipe(
    owner: Player,
    @Id(2)
    @AsReference
    val crafter: CrafterBlock,
    @Id(3)
    val recipe: Recipe?
) : PlayerAction(owner) {
    private constructor() : this(
        Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()),
        CrafterBlock(CrafterBlockType.ITEM_CRAFTER, 0, 0),
        null
    )

    override fun verify(): Boolean {
        if (!crafter.team.check(TeamPermission.MODIFY_LEVEL_OBJECTS, owner)) {
            return false
        }
        return true
    }

    override fun getUpdates(): List<LevelUpdate> {
        return listOf(CrafterBlockSelectRecipe(crafter, recipe, crafter.level))
    }
}