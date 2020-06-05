package level

import crafting.Recipe
import item.ItemType
import level.block.Block
import level.block.BlockType
import level.block.CrafterBlock
import level.block.DefaultBlock
import level.entity.robot.BrainRobot
import level.moving.MovingObject
import network.BlockReference
import network.LevelObjectReference
import network.MovingObjectReference
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
    GIVE_BRAIN_ROBOT_ITEM
}

sealed class LevelModification(
        @Id(1)
        val type: LevelModificationType) {

    abstract fun canAct(level: Level): Boolean

    abstract fun act(level: Level)

    abstract fun actGhost(level: Level)
    abstract fun cancelActGhost(level: Level)

    abstract fun equivalent(other: LevelModification): Boolean

    abstract fun synchronize(other: LevelModification)
}

class ModifyBrainRobotInv(
        @Id(4) val brainReference: LevelObjectReference,
        @Id(2) val itemType: ItemType,
        @Id(3) val quantity: Int
) : LevelModification(LevelModificationType.GIVE_BRAIN_ROBOT_ITEM) {

    private constructor() : this(MovingObjectReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), ItemType.ERROR, 0)

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
        if(quantity < 0) {
            value.inventory.remove(resources)
        } else if(quantity > 0) {
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

    override fun synchronize(other: LevelModification) {
    }
}

class SelectCrafterRecipe(
        @Id(2) val crafterReference: BlockReference,
        @Id(3) val recipe: Recipe?
) : LevelModification(LevelModificationType.SELECT_CRAFTER_RECIPE) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), null)

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

    override fun synchronize(other: LevelModification) {
    }

}


class RemoveObject(
        @Id(2)
        val objReference: LevelObjectReference
) : LevelModification(LevelModificationType.REMOVE_OBJECT) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0))

    constructor(obj: LevelObject) : this(obj.toReference())

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

    override fun synchronize(other: LevelModification) {
        other as RemoveObject

        val obj = objReference.value!!
        obj.id = other.objReference.value!!.id
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

    override fun synchronize(other: LevelModification) {
        other as AddObject

        obj.id = other.obj.id
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
    override fun canAct(level: Level) = false
    override fun act(level: Level) {
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun synchronize(other: LevelModification) {
    }

    override fun equivalent(other: LevelModification) = other is DefaultLevelModification
}