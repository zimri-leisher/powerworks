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

    override fun getChildren(): List<LevelUpdate> {
        return obj.children.map { LevelObjectAdd(it, level) }
    }

    override fun canAct(): Boolean {
        if(obj.inLevel && obj.level == level) {
            // if it's already in this level, can't add it again
            return false
        }
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
            println("ACT $this")
            // it's removing the network added with this block
            // the removal is getting sent before the addition of the block
            // solution:
            // we cant depend on the order that packets are sent in,
            // but like we still have to just assume that they will get approximately on time
            // maybe each packet has an id that linearly increments? if the client misses one, then it just waits
            // till it gets it and runs them in order...
            // yeah let's do that.. ensure order.
            // ok turns out TCP ensures order. we're good, we just have to make sure that they're sent in the right order
            // the order should be...
            // take resources, add the new network, add the new pipe, remove the new network
            // the order is: take resources, add network, add pipe, remove network... good.
            // the order sent is:
            // but on the client: take resources, remove network
            // ok, the real problem is that the level object add generates a level object remove, which finishes before
            // the add so it gets added to the network first. what we want is if the object is acted on, it should be
            //
            // ok the REAL real problem is that we are receiving the packet and deserializing it before the add update
            // takes place. how should we prevent this...
            // wait to deserialize? allow missed references and just rerun them occasionally?
            // let's wait to deserialize.. hmmmmm but we don't want to delay non level update packets
            //
            act()
            return
        }
        if (obj is PhysicalLevelObject) {
            ghostLevelObject = GhostLevelObject(obj.type, obj.x, obj.y)
            ghostLevelObject!!.rotation = obj.rotation
            level.add(ghostLevelObject!!, true)
        }
    }

    override fun cancelActGhost() {
        if (ghostLevelObject != null) {
            level.remove(ghostLevelObject!!, true)
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
