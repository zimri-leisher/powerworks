package level.update

import level.*
import level.block.Block
import level.block.BlockType
import level.block.DefaultBlock
import level.entity.robot.BrainRobot
import level.moving.MovingObject
import player.Player
import resource.ResourceContainer
import resource.ResourceNetwork
import resource.ResourceNode
import serialization.Id


/**
 * A level update for adding a [PhysicalLevelObject] to a [Level].
 */
class LevelObjectAdd(
    /**
     * The [LevelObject] to add to the level.
     */
    @Id(2)
    val obj: LevelObject,
    level: Level
) : LevelUpdate(LevelUpdateType.LEVEL_OBJECT_ADD, level) {

    private constructor() : this(DefaultBlock(BlockType.ERROR, 0, 0), LevelManager.EMPTY_LEVEL)

    override val playersToSendTo: Set<Player>?
        get() = null

    private var ghostLevelObject: GhostLevelObject? = null

    override fun canAct(): Boolean {
        if (obj is PhysicalLevelObject) {
            return obj.hitbox == Hitbox.NONE
                    || obj is GhostLevelObject
                    || level.getCollisionsWith(obj.hitbox, obj.x, obj.y)
                .filter { it !is GhostLevelObject }.none()
        }
        return true
    }

    override fun act() {
        if (obj.level != level && obj.inLevel) { // if already in another level
            println("object $obj already in another level")
        }
        if (obj is PhysicalLevelObject) {
            if (obj !is GhostLevelObject) {
                val collidingGhosts = level.data.ghostObjects.getCollisionsWith(obj.hitbox, obj.x, obj.y).toList()
                collidingGhosts.forEach { level.remove(it) }
            }
            if (obj is Block) {
                for (x in 0 until obj.type.widthTiles) {
                    for (y in 0 until obj.type.heightTiles) {
                        level.getChunkAtTile(obj.xTile + x, obj.yTile + y)
                            .setBlock(obj, obj.xTile + x, obj.yTile + y, (x == 0 && y == 0))
                    }
                }
                obj.level = level
                obj.inLevel = true
            } else if (obj is MovingObject) {
                obj.level = level
                obj.inLevel = true
                if (obj.hitbox != Hitbox.NONE) {
                    obj.intersectingChunks.forEach { it.data.movingOnBoundary.add(obj) }
                    // TODO may be a bug with adding to level and calculating the intersecting chunks based off of that
                }
                if (obj is BrainRobot) {
                    level.data.brainRobots.add(obj)
                }
            } else if (obj is GhostLevelObject) {
                level.data.ghostObjects.add(obj)
                level.data.ghostObjects.sortWith { o1, o2 -> o1.y.compareTo(o2.y) }
                obj.level = level
                obj.inLevel = true
            } else if (obj is ResourceNode) {
                level.getChunkAtTile(obj.xTile, obj.yTile).addResourceNode(obj)
                obj.level = level
                obj.inLevel = true
            }
        } else {
            if(obj is ResourceNetwork<*>) {
                level.data.resourceNetworks.add(obj)
            } else if(obj is ResourceContainer) {
                level.data.resourceContainers.add(obj)
            }
            obj.level = level
            obj.inLevel = true
        }
    }

    override fun actGhost() {
        if (obj is GhostLevelObject) {
            act()
            return
        }
        if (obj is PhysicalLevelObject) {
            ghostLevelObject = GhostLevelObject(obj.type, obj.x, obj.y)
            ghostLevelObject!!.rotation = obj.rotation
            level.add(ghostLevelObject!!)
        }
    }

    override fun cancelActGhost() {
        if (ghostLevelObject != null) {
            level.remove(ghostLevelObject!!)
        }
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is LevelObjectAdd) {
            return false
        }

        if (other.obj == this.obj) {
            return true
        }

        return false
    }

    override fun toString(): String {
        return "LevelObjectAdd(obj=$obj)"
    }
}
