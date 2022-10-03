package network

import level.*
import level.block.Block
import level.entity.robot.BrainRobot
import level.moving.MovingObject
import resource.ResourceNode
import resource.ResourceNode2
import serialization.Id
import serialization.Input
import serialization.Reference
import serialization.Serializer
import java.util.*

class ResourceNodeReference(
    @Id(1)
    val xTile: Int,
    @Id(2)
    val yTile: Int,
    @Id(3)
    val level: Level,
    @Id(4)
    val id: UUID
) : Reference<ResourceNode>() {
    constructor(node: ResourceNode) : this(node.xTile, node.yTile, node.level, node.id) {
        value = node
    }

    private constructor() : this(0, 0, LevelManager.EMPTY_LEVEL, UUID.randomUUID())

    override fun resolve(): ResourceNode? {
        val nodes = level.getResourceNodesAt(xTile, yTile)
        return nodes.firstOrNull { it.id == id }
    }

}

abstract class LevelObjectReference(
    @Id(1)
    val level: Level,
    @Id(2)
    val objectId: UUID
) : Reference<LevelObject>()

class ResourceNode2Reference(
    level: Level, objectId: UUID,
    @Id(1)
    val xTile: Int,
    @Id(2)
    val yTile: Int,
) : LevelObjectReference(level, objectId) {

    constructor(node: ResourceNode2) : this(node.level, node.id, node.xTile, node.yTile) {
        value = node
    }

    private constructor() : this(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0)

    override fun resolve(): ResourceNode2? {
        return level.getResourceNodeAt(xTile, yTile)
    }
}

class GhostLevelObjectReference(val obj: GhostLevelObject) : LevelObjectReference(obj.level, obj.id) {

    init {
        value = obj
    }

    override fun resolve() = obj
}

open class MovingObjectReference(
    level: Level, objectId: UUID,
    @Id(3)
    val x: Int,
    @Id(4)
    val y: Int
) : LevelObjectReference(level, objectId) {

    constructor(movingObject: MovingObject) : this(
        movingObject.level,
        movingObject.id,
        movingObject.x,
        movingObject.y
    ) {
        value = movingObject
    }

    private constructor() : this(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0)

    override fun resolve(): MovingObject? {
        val xChunk = x shr CHUNK_EXP
        val yChunk = y shr CHUNK_EXP
        var currentChunkRange = 0
        var currentChunks = setOf(level.getChunkAt(x, y))
        while (currentChunkRange < Math.max(level.widthChunks - xChunk - 1, xChunk + 1) || currentChunkRange < Math.max(
                level.heightChunks - yChunk - 1,
                yChunk + 1
            )
        ) {
            // while we still have range to go
            for (chunk in currentChunks) {
                val moving = chunk.data.moving.firstOrNull { it.id == objectId }
                if (moving != null) {
                    return moving
                }
            }
            currentChunkRange++
            if (currentChunkRange > 3) {
                println("Resolving reference $this taking abnormally long, possible desync")
            }
            currentChunks = level.getChunksFromChunkRectangle(
                xChunk - currentChunkRange,
                yChunk - currentChunkRange,
                xChunk + currentChunkRange,
                yChunk + currentChunkRange
            ).toSet()
        }
        return null
    }

    override fun toString(): String {
        return "MovingObjectReference: $level, $objectId, $x, $y"
    }
}

class BrainRobotReference(
    @Id(5)
    val brainRobotId: UUID
) : MovingObjectReference(LevelManager.loadedLevels.firstOrNull { it.data.brainRobots.any { it.id == brainRobotId } }
    ?: LevelManager.EMPTY_LEVEL, brainRobotId, 0, 0) {

    private constructor() : this(UUID.randomUUID())

    constructor(brainRobot: BrainRobot) : this(brainRobot.id) {
        value = brainRobot
    }

    override fun resolve(): MovingObject? {
        return level.data.brainRobots.firstOrNull { it.id == brainRobotId }
    }

    override fun toString(): String {
        return "BrainRobotReference: $level, $brainRobotId"
    }
}

class BlockReference(
    level: Level, objectId: UUID,
    @Id(3)
    val xTile: Int,
    @Id(4)
    val yTile: Int
) : LevelObjectReference(level, objectId) {

    constructor(block: Block) : this(block.level, block.id, block.xTile, block.yTile) {
        value = block
    }

    private constructor() : this(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0)

    override fun resolve(): Block? {
        val block = level.getBlockAtTile(xTile, yTile)
        if (block == null) {
            println("no block at $xTile, $yTile")
            //println("blocks in that chunk: ${level.getChunkFromTile(xTile, yTile).data.blocks.joinToString()}")
            return null
        }
        if (block.id != objectId) {
            println("block has wrong id")
            return null
        }
        return block
    }

    override fun toString(): String {
        return "Reference to block at $xTile, $yTile: $value"
    }
}