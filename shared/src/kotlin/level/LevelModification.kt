package level

import behavior.leaves.EntityPath
import crafting.Recipe
import item.ItemType
import level.block.*
import level.entity.Entity
import level.entity.EntityGroup
import level.entity.Formation
import level.entity.robot.BrainRobot
import level.moving.MovingObject
import misc.PixelCoord
import network.BlockReference
import network.LevelObjectReference
import network.MovingObjectReference
import network.ResourceNodeReference
import player.Player
import resource.ResourceList
import serialization.Id
import java.util.*
import kotlin.Comparator
import kotlin.math.absoluteValue

enum class LevelModificationType {
    DEFAULT,
    ADD_OBJECT,
    REMOVE_OBJECT,
    PUSH_MOVING_OBJECT,
    TELEPORT_MOVING_OBJECT,
    SELECT_CRAFTER_RECIPE,
    DAMAGE_ENTITY,
    GIVE_BRAIN_ROBOT_ITEM,
    MODIFY_BLOCK_CONTAINER,
    TRANSFER_THROUGH_RESOURCE_NODE,
    MACHINE_BLOCK_FINISH_WORK,
    SET_ENTITY_PATH,
    UPDATE_ENTITY_PATH_POSITION,
    ADD_ENTITIES_TO_GROUP,
    SET_ENTITY_FORMATION,
    SET_ENTITY_TARGET
}

sealed class LevelModification(
        @Id(1)
        val type: LevelModificationType) {

    abstract val playersToSendTo: Set<Player>?

    abstract fun canAct(level: Level): Boolean

    abstract fun act(level: Level)

    abstract fun actGhost(level: Level)
    abstract fun cancelActGhost(level: Level)

    abstract fun equivalent(other: LevelModification): Boolean
}

class SetEntityFormation(
        @Id(2)
        val positions: Map<MovingObjectReference, PixelCoord>,
        @Id(3)
        val center: PixelCoord
) : LevelModification(LevelModificationType.SET_ENTITY_FORMATION) {

    private constructor() : this(mapOf(), PixelCoord(0, 0))

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if (positions.isEmpty()) {
            return true
        }
        val entities = positions.keys.map { it.value as Entity? }
        if (entities.any { it == null }) {
            return false
        }
        entities as List<Entity>
        if (entities.map { it.group }.distinct().size != 1) {
            return false
        }
        return true
    }

    override fun act(level: Level) {
        val group = (positions.keys.first().value!! as Entity).group!!
        val formation = Formation(center, positions.mapKeys { it.key.value!! as Entity })
        group.formation = formation
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is SetEntityFormation) {
            return false
        }

        if (other.positions.size != positions.size) {
            return false
        }
        if (other.positions.any { (key, value) ->
                    positions.none { (key2, value2) ->
                        key.value != key2.value && value != value2
                    }
                }) {
            return false
        }

        if (other.center != center) {
            return false
        }

        return true
    }

}

class AddEntititesToGroup(@Id(2) val entitiesInGroup: List<MovingObjectReference>) : LevelModification(LevelModificationType.ADD_ENTITIES_TO_GROUP) {

    private constructor() : this(listOf())

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        return entitiesInGroup.all { it.value != null }
    }

    override fun act(level: Level) {
        val dereferencedEntities = entitiesInGroup.map { it.value!! as Entity }
        val group = EntityGroup(dereferencedEntities)
        dereferencedEntities.forEach { it.group = group }
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is AddEntititesToGroup) {
            return false
        }

        if (other.entitiesInGroup.size != entitiesInGroup.size) {
            return false
        }

        if (other.entitiesInGroup.any { otherEntityRef -> entitiesInGroup.none { it.value == otherEntityRef.value } }) {
            return false
        }
        return true
    }

}

class UpdateEntityPathPosition(@Id(2) val entityReference: MovingObjectReference,
                               @Id(5) val pathIndex: Int,
                               @Id(6) val timeReachedStep: Int,
                               @Id(4) val pathHash: Int) : LevelModification(LevelModificationType.UPDATE_ENTITY_PATH_POSITION) {

    private constructor() : this(MovingObjectReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), 0, 0, 0)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level) = entityReference.value != null

    override fun act(level: Level) {
        val entity = entityReference.value!! as Entity
        if (entity.behavior.path.hashCode() != pathHash) {
            println("path hash different, needs to resync")
        }
        (entityReference.value!! as Entity).behavior.shouldBeAtStepAtTime(pathIndex, timeReachedStep)
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is UpdateEntityPathPosition) {
            return false
        }

        return other.entityReference.value == entityReference.value && other.pathIndex == pathIndex && other.pathHash == pathHash && other.timeReachedStep == timeReachedStep
    }

}

class SetEntityTarget(
        @Id(2)
        val entityReference: MovingObjectReference,
        @Id(3)
        val target: LevelObjectReference?
) : LevelModification(LevelModificationType.SET_ENTITY_TARGET) {

    private constructor() : this(MovingObjectReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0))

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if (entityReference.value == null) {
            return false
        }
        if (target != null && target.value == null) {
            return false
        }
        return true
    }

    override fun act(level: Level) {
        (entityReference.value!! as Entity).behavior.target = target?.value
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is SetEntityTarget) {
            return false
        }
        if (entityReference.value != null && other.entityReference.value != entityReference.value) {
            return false
        }
        if (target?.value != other.target?.value) {
            return false
        }
        return true
    }

}

class SetEntityPath(@Id(2) val entityReference: MovingObjectReference,
                    @Id(4) val startPosition: PixelCoord,
                    @Id(3) val path: EntityPath) : LevelModification(LevelModificationType.SET_ENTITY_PATH) {

    private constructor() : this(MovingObjectReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), PixelCoord(0, 0), EntityPath(PixelCoord(0, 0), listOf()))

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        return entityReference.value != null
    }

    override fun act(level: Level) {
        (entityReference.value!! as Entity).apply {
            setPosition(startPosition.xPixel, startPosition.yPixel)
            behavior.follow(path)
        }
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is SetEntityPath) {
            return false
        }

        if (other.entityReference.value != entityReference.value) {
            return false
        }
        return path == other.path && startPosition == other.startPosition
    }

}

class MachineBlockFinishWork(
        @Id(2) val blockReference: BlockReference
) : LevelModification(LevelModificationType.MACHINE_BLOCK_FINISH_WORK) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0))

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level) = blockReference.value != null && blockReference.value is MachineBlock

    override fun act(level: Level) {
        val block = blockReference.value!! as MachineBlock
        block.currentWork = 0
        block.onFinishWork()
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is MachineBlockFinishWork) {
            return false
        }
        return other.blockReference.value == blockReference.value
    }

}

class TransferThroughResourceNode(
        @Id(2) val nodeReference: ResourceNodeReference,
        @Id(3) val resources: ResourceList,
        @Id(4) val output: Boolean,
        @Id(5) val checkIfAble: Boolean,
        @Id(6) val mustContainOrHaveSpace: Boolean
) : LevelModification(LevelModificationType.TRANSFER_THROUGH_RESOURCE_NODE) {

    private constructor() : this(ResourceNodeReference(0, 0, LevelManager.EMPTY_LEVEL, UUID.randomUUID()), ResourceList(), false, false, false)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if (nodeReference.value == null) {
            return false
        }
        val node = nodeReference.value!!
        if (output) {
            if (!node.canOutput(resources, mustContainOrHaveSpace)) {
                return false
            }
        } else {
            if (!node.canInput(resources, mustContainOrHaveSpace)) {
                return false
            }
        }
        return true
    }

    override fun act(level: Level) {
        val node = nodeReference.value!!
        if (output) {
            node.output(resources, checkIfAble, mustContainOrHaveSpace)
        } else {
            node.input(resources, checkIfAble, mustContainOrHaveSpace)
        }
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is TransferThroughResourceNode) {
            return false
        }
        return other.nodeReference.value == nodeReference.value && other.resources == resources &&
                other.mustContainOrHaveSpace == mustContainOrHaveSpace && other.checkIfAble == checkIfAble
    }

}

class ModifyBlockContainer(
        @Id(2) val blockReference: BlockReference,
        @Id(5) val containerId: UUID,
        @Id(3) val resources: ResourceList,
        @Id(4) val add: Boolean
) : LevelModification(LevelModificationType.MODIFY_BLOCK_CONTAINER) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), UUID.randomUUID(), ResourceList(), false)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if (blockReference.value == null || blockReference.value !is Block) {
            return false
        }
        val block = blockReference.value as Block
        val container = block.containers.firstOrNull { it.id == containerId } ?: return false

        if (!add) {
            if (!container.canRemove(resources)) {
                return false
            }
        } else {
            if (!container.canAdd(resources)) {
                return false
            }
        }
        return true
    }

    override fun act(level: Level) {
        val block = blockReference.value as Block
        val container = block.containers.first { it.id == containerId }
        if (!add) {
            container.remove(resources)
        } else {
            container.add(resources)
        }
    }

    override fun actGhost(level: Level) {
        // TODO add ghost items to container
    }

    override fun cancelActGhost(level: Level) {
        // TODO add ghost items to container
    }

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is ModifyBlockContainer) {
            return false
        }
        if (other.blockReference.value != blockReference.value) {
            return false
        }
        if (other.resources != resources || other.containerId != containerId) {
            return false
        }
        return true
    }

}

class ModifyBrainRobotInv(
        @Id(4) val brainReference: MovingObjectReference,
        @Id(2) val itemType: ItemType,
        @Id(3) val quantity: Int
) : LevelModification(LevelModificationType.GIVE_BRAIN_ROBOT_ITEM) {

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

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is ModifyBrainRobotInv) {
            return false
        }

        if (other.brainReference.value != brainReference.value) {
            return false
        }

        return other.itemType == itemType && other.quantity == quantity
    }

}

class SelectCrafterRecipe(
        @Id(2) val crafterReference: BlockReference,
        @Id(3) val recipe: Recipe?
) : LevelModification(LevelModificationType.SELECT_CRAFTER_RECIPE) {

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

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is SelectCrafterRecipe) {
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


class RemoveObject(
        @Id(2)
        val objReference: LevelObjectReference
) : LevelModification(LevelModificationType.REMOVE_OBJECT) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0))

    constructor(obj: LevelObject) : this(obj.toReference())

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level) = objReference.value != null && objReference.level == level

    override fun act(level: Level) {
        val obj = objReference.value!!
        if (obj is Block) {
            for (x in 0 until obj.type.widthTiles) {
                for (y in 0 until obj.type.heightTiles) {
                    level.getChunkFromTile(obj.xTile + x, obj.yTile + y).removeBlock(obj, obj.xTile + x, obj.yTile + y, (x == 0 && y == 0))
                }
            }
            obj.inLevel = false
        } else if (obj is MovingObject) {
            val chunk = level.getChunkAt(obj.xChunk, obj.yChunk)
            if (obj is DroppedItem) {
                chunk.removeDroppedItem(obj)
            }
            if (obj.hitbox != Hitbox.NONE)
                obj.intersectingChunks.forEach { it.data.movingOnBoundary.remove(obj) }
            chunk.removeMoving(obj)
            obj.inLevel = false
            if (obj is BrainRobot) {
                level.data.brainRobots.remove(obj)
            }
        } else if (obj is GhostLevelObject) {
            level.data.ghostObjects.remove(obj)
            obj.inLevel = false
            obj.level = LevelManager.EMPTY_LEVEL
        }
    }

    override fun actGhost(level: Level) {
        val obj = objReference.value!!
        if (obj is GhostLevelObject) {
            // ghost act on a ghost object is real
            level.data.ghostObjects.remove(obj)
            obj.inLevel = false
            obj.level = LevelManager.EMPTY_LEVEL
        } else {
            // TODO hide it
        }
    }

    override fun cancelActGhost(level: Level) {
        // TODO unhide it
    }

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is RemoveObject) {
            return false
        }

        if (objReference.value != other.objReference.value) {
            return false
        }

        return true
    }

}

class AddObject(
        @Id(2)
        val obj: LevelObject
) : LevelModification(LevelModificationType.ADD_OBJECT) {

    private constructor() : this(DefaultBlock(BlockType.ERROR, 0, 0, 0))

    override val playersToSendTo: Set<Player>?
        get() = null

    private var ghostLevelObject: GhostLevelObject? = null

    override fun canAct(level: Level) = obj.hitbox == Hitbox.NONE || level.getCollisionsWith(obj.hitbox, obj.xPixel, obj.yPixel, { it !is GhostLevelObject }).isEmpty()

    override fun act(level: Level) {
        if (obj.level != level && obj.inLevel) { // if already in another level
            obj.level.remove(obj)
        }
        if (obj !is GhostLevelObject) {
            val collidingGhosts = level.getCollisionsWith(level.data.ghostObjects, obj.hitbox, obj.xPixel, obj.yPixel)
            collidingGhosts.forEach { level.remove(it) }
        }
        if (obj is Block) {
            for (x in 0 until obj.type.widthTiles) {
                for (y in 0 until obj.type.heightTiles) {
                    level.getChunkFromTile(obj.xTile + x, obj.yTile + y).setBlock(obj, obj.xTile + x, obj.yTile + y, (x == 0 && y == 0))
                }
            }
            obj.level = level
            obj.inLevel = true
        } else if (obj is MovingObject) {
            if (obj.hitbox != Hitbox.NONE) {
                obj.intersectingChunks.forEach { it.data.movingOnBoundary.add(obj) }
            }
            if (obj is DroppedItem) {
                level.getChunkAt(obj.xChunk, obj.yChunk).addDroppedItem(obj)
            }
            obj.level = level
            obj.inLevel = true
            if (obj is BrainRobot) {
                level.data.brainRobots.add(obj)
            }
        } else if (obj is GhostLevelObject) {
            level.data.ghostObjects.add(obj)
            level.data.ghostObjects.sortWith(Comparator { o1, o2 -> o1.yPixel.compareTo(o2.yPixel) })
            obj.level = level
            obj.inLevel = true
        }
    }

    override fun actGhost(level: Level) {
        if (obj is GhostLevelObject) {
            act(level)
            return
        }
        ghostLevelObject = GhostLevelObject(obj.type, obj.xPixel, obj.yPixel, obj.rotation)
        level.add(ghostLevelObject!!)
    }

    override fun cancelActGhost(level: Level) {
        if (ghostLevelObject != null) {
            level.remove(ghostLevelObject!!)
        }
    }

    override fun equivalent(other: LevelModification): Boolean {
        if (other !is AddObject) {
            return false
        }

        if (other.obj == this.obj) {
            return true
        }

        return false
    }
}

class DefaultLevelModification : LevelModification(LevelModificationType.DEFAULT) {
    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level) = false
    override fun act(level: Level) {
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelModification) = other is DefaultLevelModification
}