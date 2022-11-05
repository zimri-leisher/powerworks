package network

import level.*
import level.block.Block
import level.entity.robot.BrainRobot
import level.moving.MovingObject
import resource.ResourceContainer
import resource.ResourceNetwork
import resource.ResourceNode
import serialization.AsReference
import serialization.Id
import serialization.Reference
import java.util.*

abstract class LevelObjectReference<T : LevelObject>(
    @Id(1)
    @AsReference
    val level: Level,
    @Id(2)
    val objectId: UUID
) : Reference<T>()

class ResourceNetworkReference(
    level: Level,
    objectId: UUID
) : LevelObjectReference<ResourceNetwork<*>>(level, objectId) {
    constructor(obj: ResourceNetwork<*>) : this(obj.level, obj.id)

    private constructor() : this(LevelManager.EMPTY_LEVEL, UUID.randomUUID())

    override fun resolve(): ResourceNetwork<*>? {
        return level.data.resourceNetworks.firstOrNull { it.id == objectId }
    }

    override fun toString(): String {
        return "ResourceNetworkReference($level, $objectId)"
    }
}

class ResourceContainerReference(
    level: Level,
    objectId: UUID
) : LevelObjectReference<ResourceContainer>(level, objectId) {

    constructor(obj: ResourceContainer) : this(obj.level, obj.id)

    private constructor() : this(LevelManager.EMPTY_LEVEL, UUID.randomUUID())

    override fun resolve(): ResourceContainer? {
        return level.data.resourceContainers.firstOrNull { it.id == objectId }
    }

    override fun toString(): String {
        return "ResourceContainerReference($level, $objectId)"
    }
}

abstract class PhysicalLevelObjectReference<T : PhysicalLevelObject>(level: Level, objectId: UUID) :
    LevelObjectReference<T>(level, objectId) {
    constructor(obj: PhysicalLevelObject) : this(obj.level, obj.id)
}

class ResourceNodeReference(
    level: Level, objectId: UUID,
    @Id(3)
    val xTile: Int,
    @Id(4)
    val yTile: Int,
) : PhysicalLevelObjectReference<ResourceNode>(level, objectId) {

    constructor(node: ResourceNode) : this(node.level, node.id, node.xTile, node.yTile) {
        value = node
    }

    private constructor() : this(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0)

    override fun resolve(): ResourceNode? {
        return level.getResourceNodeAt(xTile, yTile)
    }

    override fun toString(): String {
        return "ResourceNodeReference($level, $objectId, $xTile, $yTile)"
    }
}

class GhostLevelObjectReference(val obj: GhostLevelObject) :
    PhysicalLevelObjectReference<GhostLevelObject>(obj.level, obj.id) {

    init {
        value = obj
    }

    override fun resolve() = obj
}

open class MovingObjectReference<T : MovingObject>(
    level: Level, objectId: UUID,
    @Id(3)
    val x: Int,
    @Id(4)
    val y: Int
) : PhysicalLevelObjectReference<T>(level, objectId) {

    constructor(movingObject: T) : this(
        movingObject.level,
        movingObject.id,
        movingObject.x,
        movingObject.y
    ) {
        value = movingObject
    }

    private constructor() : this(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0)

    override fun resolve(): T? {
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
                    return moving as T
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
        return "MovingObjectReference($level, $objectId, $x, $y)"
    }
}

class BrainRobotReference(
    @Id(5)
    val brainRobotId: UUID
) : MovingObjectReference<BrainRobot>(LevelManager.loadedLevels.firstOrNull { it.data.brainRobots.any { it.id == brainRobotId } }
    ?: LevelManager.EMPTY_LEVEL, brainRobotId, 0, 0) {

    private constructor() : this(UUID.randomUUID())

    constructor(brainRobot: BrainRobot) : this(brainRobot.id) {
        value = brainRobot
    }

    override fun resolve(): BrainRobot? {
        return level.data.brainRobots.firstOrNull { it.id == brainRobotId }
    }

    override fun toString(): String {
        return "BrainRobotReference($level, $brainRobotId)"
    }
}

class BlockReference(
    level: Level, objectId: UUID,
    @Id(3)
    val xTile: Int,
    @Id(4)
    val yTile: Int
) : PhysicalLevelObjectReference<Block>(level, objectId) {

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
        return "BlockReference($level, $objectId, $xTile, $yTile)"
    }
}