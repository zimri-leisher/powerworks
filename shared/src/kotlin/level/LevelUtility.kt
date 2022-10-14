package level

import level.block.Block
import level.moving.MovingObject
import level.tile.Tile
import misc.Geometry
import resource.ResourceNode
import kotlin.math.max
import kotlin.math.min

fun Level.isChunkWithinBounds(xChunk: Int, yChunk: Int) =
    isTileWithinBounds(xChunk shl CHUNK_TILE_EXP, yChunk shl CHUNK_TILE_EXP)

fun Level.isTileWithinBounds(xTile: Int, yTile: Int) = isWithinBounds(xTile shl 4, yTile shl 4)

fun Level.isWithinBounds(x: Int, y: Int): Boolean {
    if (x < 0 || y < 0)
        return false
    if (x >= width || y >= height)
        return false
    return true
}

fun Level.isChunkRectangleInBounds(xChunk: Int, yChunk: Int, widthChunks: Int, heightChunks: Int) =
    isTileRectangleInBounds(
        xChunk shl CHUNK_TILE_EXP,
        yChunk shl CHUNK_TILE_EXP,
        widthTiles shl CHUNK_TILE_EXP,
        heightChunks shl CHUNK_TILE_EXP
    )

fun Level.isTileRectangleInBounds(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int) =
    isRectangleInBounds(xTile shl 4, yTile shl 4, widthTiles shl 4, heightTiles shl 4)

fun Level.isRectangleInBounds(x: Int, y: Int, width: Int, height: Int) =
    isWithinBounds(x, y) ||
            isWithinBounds(x + width, y + height)

/**
 * Gets the [Block] at [xTile], [yTile]
 *
 * @return the [Block] whose base is at [xTile], [yTile]. A block base does not necessarily correspond with the block's [Hitbox],
 * it is just the area in which other [Block]s are unable to be placed
 */
fun Level.getBlockAtTile(xTile: Int, yTile: Int): Block? {
    if (!isTileWithinBounds(xTile, yTile)) {
        Exception().printStackTrace()
        println("out of bounds")
        return null
    }
    return getChunkAtTile(xTile, yTile).getBlock(xTile, yTile)
}

/**
 * Gets the [Block] at [x], [y]
 *
 * @return the [Block] whose base is at [x], [y]. A block base does not necessarily correspond with the block's [Hitbox],
 * it is just the area in which other [Block]s are unable to be placed
 */
fun Level.getBlockAt(x: Int, y: Int) =
    getBlockAtTile(x shr 4, y shr 4)

/**
 * Gets collisions between the [levelObj] and [Block]s in this [Level]
 *
 * @return a sequence of [Block]s intersecting the [Hitbox] of [levelObj]
 */
fun Level.getBlockCollisions(levelObj: PhysicalLevelObject) =
    getBlockCollisions(levelObj.hitbox, levelObj.x, levelObj.y)

/**
 * Gets collisions between the [hitbox] at [x], [y] and [Block]s in this [Level]
 *
 * @param hitbox the hitbox to check collisions with
 * @param x the level x  of the [hitbox]. [Hitbox.xStart] is added to this to get the true x coordinate of the rectangle
 * @param y the level y  of the [hitbox]. [Hitbox.yStart] is added to this to get the true y coordinate of the rectangle
 * @return a sequence of [Block]s intersecting the [hitbox]
 */
fun Level.getBlockCollisions(hitbox: Hitbox, x: Int, y: Int) =
    getBlockCollisions(x + hitbox.xStart, y + hitbox.yStart, hitbox.width, hitbox.height)

fun Level.getBlockCollisions(x: Int, y: Int, width: Int, height: Int): Sequence<Block> {
    // Blocks won't have a hitbox bigger than their width/height tiles
    return getIntersectingBlocksFromRectangle(x, y, width, height).filter {
        Geometry.intersects(
            it.x + it.hitbox.xStart, it.y + it.hitbox.yStart, it.hitbox.width, it.hitbox.height,
            x, y, width, height
        )
    }
}

/**
 * Gets [Block]s whose base intersects the rectangle starting at [x], [y] with width [width] and height [height]
 *
 * @return a sequence of [Block]s whose base (not [Hitbox]) intersects the given rectangle.
 * A block base does not necessarily correspond with the block's [Hitbox], it is just the area in which other [Block]s
 * are unable to be placed. To check for [Hitbox] intersections, use [getBlockCollisions]
 */
fun Level.getIntersectingBlocksFromRectangle(x: Int, y: Int, width: Int, height: Int) =
    getIntersectingBlocksFromTileRectangle(x shr 4, y shr 4, width shr 4, height shr 4)

/**
 * Gets [Block]s whose base intersects the rectangle starting at [xTile], [yTile] with width [widthTiles] and height [heightTiles]
 *
 * @return a set of [Block]s whose base (not [Hitbox]) intersects the given rectangle.
 * A block base does not necessarily correspond with the block's [Hitbox], it is just the area in which other [Block]s
 * are unable to be placed. To check for [Hitbox] intersections, use [getBlockCollisions]
 */
fun Level.getIntersectingBlocksFromTileRectangle(
    xTile: Int,
    yTile: Int,
    widthTiles: Int,
    heightTiles: Int
): Sequence<Block> {
    if (!isTileRectangleInBounds(xTile, yTile, widthTiles, heightTiles))
        return emptySequence()
    return (xTile..(xTile + widthTiles)).asSequence().flatMap { x ->
        (yTile..(yTile + heightTiles)).asSequence().map { x to it }
    }
        .mapNotNull { getBlockAtTile(it.first, it.second) }
}

/**
 * Gets collisions between the square centered on [x], [y] with radius [radius] and [Block]s in this [Level]
 *
 * @return a set of [Block]s whose [Hitbox] intersects the square centered at [x], [y] with the given [radius]
 * (which is the side length / 2). Note this does not check the base (the area other blocks
 * cannot be placed in around this block), only the [Hitbox]
 */
fun Level.getBlockCollisionsInSquareCenteredOn(x: Int, y: Int, radius: Int): Sequence<Block> {
    if (!isRectangleInBounds(x - radius, y - radius, radius * 2, radius * 2))
        return emptySequence()
    return getChunksFromRectangle(x - radius, y - radius, radius * 2, radius * 2).flatMap {
        it.data.blocks.asSequence()
    }.filterNotNull().filter {
        Geometry.intersects(
            x - radius, y - radius, radius * 2, radius * 2,
            it.x + it.hitbox.xStart, it.y + it.hitbox.yStart, it.hitbox.width, it.hitbox.height
        )
    }
}

fun Level.getBlockCollisionsAt(x: Int, y: Int): Sequence<Block> {
    if (!isWithinBounds(x, y))
        return emptySequence()
    return getChunkAt(x, y).data.blocks.asSequence().filterNotNull().filter {
        it.hitbox != Hitbox.NONE &&
                Geometry.intersects(
                    it.x + it.hitbox.xStart, it.y + it.hitbox.yStart, it.hitbox.width, it.hitbox.height,
                    x, y, 0, 0
                )
    }
}

/**
 * Gets collisions between the [levelObj] and [MovingObject]s in this [Level]
 *
 * @return a set of [MovingObject]s intersecting the [Hitbox] of [levelObj]
 */
fun Level.getMovingObjectCollisions(levelObj: PhysicalLevelObject) =
    getMovingObjectCollisions(levelObj.type, levelObj.x, levelObj.y)

fun Level.getMovingObjectCollisions(levelObjType: PhysicalLevelObjectType<*>, x: Int, y: Int) =
    getMovingObjectCollisions(levelObjType.hitbox, x, y)

fun Level.getMovingObjectCollisions(hitbox: Hitbox, x: Int, y: Int) =
    getMovingObjectCollisions(x + hitbox.xStart, y + hitbox.yStart, hitbox.width, hitbox.height)

/**
 * @return a sequence of [MovingObject]s in this [Level] whose [Hitbox] intersects the square centered at [x], [y] with the given [radius]
 * (which is the side length / 2)
 */
fun Level.getMovingObjectCollisionsInSquareCenteredOn(x: Int, y: Int, radius: Int) =
    getMovingObjectCollisions(x - radius, y - radius, radius * 2, radius * 2)

/**
 * Gets [MovingObject]s whose [Hitbox] intersects the rectangle starting at [x], [y] with width [width] and height [height]
 *
 * @return a set of [MovingObject]s whose [Hitbox] intersects the given rectangle
 */
fun Level.getMovingObjectCollisions(x: Int, y: Int, width: Int, height: Int): Sequence<MovingObject> {
    if (!isRectangleInBounds(x, y, width, height))
        return emptySequence()
    return getChunksFromRectangle(x, y, width, height).flatMap {
        it.data.moving.getCollisionsWith(x, y, width, height) +
                it.data.movingOnBoundary.getCollisionsWith(x, y, width, height)
    }
}

/**
 * @return a sequence of [MovingObject]s in this [Level] whose [Hitbox] contains the given point
 */
fun Level.getMovingObjectCollisionsAt(x: Int, y: Int): Sequence<MovingObject> {
    if (!isWithinBounds(x, y))
        return emptySequence()
    return getChunkAt(x, y).let {
        it.data.moving.getCollisionsWith(x, y, 0, 0) +
                it.data.movingOnBoundary.getCollisionsWith(x, y, 0, 0)
    }
}

/**
 * Gets collisions between the [levelObj] and [PhysicalLevelObject]s in this [Level]
 *
 * @param predicate the selector for [PhysicalLevelObject]s to consider collisions with
 * @return a sequence of [PhysicalLevelObject]s intersecting the [Hitbox] of [levelObj]
 */
fun Level.getCollisionsWith(levelObj: PhysicalLevelObject) =
    getCollisionsWith(levelObj.hitbox, levelObj.x, levelObj.y)

/**
 * Gets collisions between the [hitbox] at [x], [y] and [PhysicalLevelObject]s in this [Level]
 *
 * @param hitbox the hitbox to check collisions with
 * @param x the level x  of the [hitbox]. [Hitbox.xStart] is added to this to get the true x coordinate of the rectangle
 * @param y the level y  of the [hitbox]. [Hitbox.yStart] is added to this to get the true y coordinate of the rectangle
 * @return a sequence of [PhysicalLevelObject]s intersecting the [hitbox]
 */
fun Level.getCollisionsWith(hitbox: Hitbox, x: Int, y: Int) =
    getCollisionsWith(x + hitbox.xStart, y + hitbox.yStart, hitbox.width, hitbox.height)

fun Level.getCollisionsWith(x: Int, y: Int, width: Int, height: Int): Sequence<PhysicalLevelObject> =
    getBlockCollisions(x, y, width, height) + getMovingObjectCollisions(x, y, width, height)

/**
 * Gets collisions between the [levelObj] and all [this@getCollisionsWith]
 *
 * @param this@getCollisionsWith the [L]s to consider when checking collisions
 * @param predicate the selector for [L]s to consider collisions with
 * @return a sequence of [L]s intersecting the [Hitbox] of [levelObj]
 */
fun <L : PhysicalLevelObject> Iterable<L>.getCollisionsWith(levelObj: PhysicalLevelObject) =
    getCollisionsWith(levelObj.hitbox, levelObj.x, levelObj.y).filter { it != levelObj }

/**
 * Gets collisions between the [hitbox] at [x], [y] and [this@getCollisionsWith]
 *
 * @param hitbox the hitbox to check collisions with
 * @param x the level x  of the [hitbox]. [Hitbox.xStart] is added to this to get the true x coordinate of the rectangle
 * @param y the level y  of the [hitbox]. [Hitbox.yStart] is added to this to get the true y coordinate of the rectangle
 * @return a sequence of [L]s intersecting the [hitbox]
 */
fun <L : PhysicalLevelObject> Iterable<L>.getCollisionsWith(hitbox: Hitbox, x: Int, y: Int) =
    getCollisionsWith(x + hitbox.xStart, y + hitbox.yStart, hitbox.width, hitbox.height)

fun <L : PhysicalLevelObject> Iterable<L>.getCollisionsWith(x: Int, y: Int, width: Int, height: Int): Sequence<L> {
    if (none()) {
        return emptySequence()
    }
    return asSequence().filter {
        it.hitbox != Hitbox.NONE && Geometry.intersects(
            x, y, width, height,
            it.x + it.hitbox.xStart, it.y + it.hitbox.yStart, it.hitbox.width, it.hitbox.height
        )
    }
}

fun Level.getCollisionsAt(x: Int, y: Int): Sequence<PhysicalLevelObject> {
    return getBlockCollisionsAt(x, y) + getMovingObjectCollisionsAt(x, y)
}

/**
 * @return true if the [hitbox] at [x], [y] collides with the hitbox of the [levelObj]
 */
fun Level.doHitboxesCollide(hitbox: Hitbox, x: Int, y: Int, levelObj: PhysicalLevelObject) =
    doHitboxesCollide(hitbox, x, y, levelObj.hitbox, levelObj.x, levelObj.y)

/**
 * @return true if the [hitbox1] at [x1], [y1] collides with the [hitbox2] at [x2], [y2]
 */
fun Level.doHitboxesCollide(
    hitbox1: Hitbox, x1: Int, y1: Int,
    hitbox2: Hitbox, x2: Int, y2: Int
): Boolean {
    return Geometry.intersects(
        x1 + hitbox1.xStart, y1 + hitbox1.yStart, hitbox1.width, hitbox1.height,
        x2 + hitbox2.xStart, y2 + hitbox2.yStart, hitbox2.width, hitbox2.height
    )
}

/**
 * Updates the [ResourceNodeOld.attachedNode] of the given [node]
 */
fun Level.updateResourceNodeAttachments(node: ResourceNodeOld) {
    val attached = getResourceNodesAt(
        node.xTile + Geometry.getXSign(node.dir),
        node.yTile + Geometry.getYSign(node.dir),
        { it.resourceCategory == node.resourceCategory })
        .filter { it.dir == Geometry.getOppositeAngle(node.dir) }.firstOrNull()
    node.attachedNode = attached
    if (attached != null && node.network.id != attached.network.id) {
        // merge networks
        if (node.network.attachedNodes.size >= attached.network.attachedNodes.size) {
            node.network.mergeIntoThis(attached.network)
        } else {
            attached.network.mergeIntoThis(node.network)
        }
    }
    // TODO combine networks
}

/**
 * @return a set of [ResourceNodeOld]s in this [Level] at [xTile], [yTile] which match the given [predicate] (defaults to { true })
 */
fun Level.getResourceNodesAt(
    xTile: Int,
    yTile: Int,
    predicate: (ResourceNodeOld) -> Boolean = { true }
): Set<ResourceNodeOld> {
    val set = mutableSetOf<ResourceNodeOld>()
    if (!isTileWithinBounds(xTile, yTile))
        return emptySet()
    val chunk = getChunkAtTile(xTile, yTile)
//    chunk.data.resourceNodes.forEach {
//        it.filter { predicate(it) && it.xTile == xTile && it.yTile == yTile }.forEach { set.add(it) }
//    }
    return set
}

fun Level.getResourceNodeAt(xTile: Int, yTile: Int): ResourceNode? {
    if (!isTileWithinBounds(xTile, yTile))
        return null
    val chunk = getChunkAtTile(xTile, yTile)
    return chunk.getResourceNode(xTile, yTile)
}

/**
 * @return the [Tile] at [xTile], [yTile]
 */
fun Level.getTileAtTile(xTile: Int, yTile: Int): Tile {
    if (!isTileWithinBounds(xTile, yTile))
        throw Exception("No tile at $xTile, $yTile in $this")
    return getChunkAtTile(xTile, yTile).getTile(xTile, yTile)
}

/**
 * Gets the [PhysicalLevelObject]s at [x], [y] matching the given [predicate] (defaults to { true })
 *
 * @return a sequence of [PhysicalLevelObject]s in this [Level] at [x], [y]. Note, for [Block]s,
 * this checks the base of the [Block] (i.e. the tiles around the block in which no other block may be), and for [MovingObject]s,
 * it checks the [Hitbox]. The [MovingObject]s will be first in the order of the set, then the [Block]s
 */
fun Level.getLevelObjectsAt(x: Int, y: Int): Sequence<PhysicalLevelObject> =
    (getBlockAt(x, y)?.let { sequenceOf(it) } ?: emptySequence<PhysicalLevelObject>()) +
            getMovingObjectCollisionsAt(x, y)

fun Level.getChunkAtChunk(xChunk: Int, yChunk: Int): Chunk {
    if (!isChunkWithinBounds(xChunk, yChunk)) {
        throw Exception("No chunk at $xChunk, $yChunk in $this")
    }
    return data.chunks[xChunk + yChunk * widthChunks]
}

fun Level.getChunkAtTile(xTile: Int, yTile: Int) =
    getChunkAtChunk(xTile shr CHUNK_TILE_EXP, yTile shr CHUNK_TILE_EXP)

fun Level.getChunkAt(x: Int, y: Int) =
    getChunkAtTile(x shr 4, y shr 4)

fun Level.getChunksFromTileRectangle(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int) =
    getChunksFromChunkRectangle(
        xTile shr CHUNK_TILE_EXP,
        yTile shr CHUNK_TILE_EXP,
        (xTile + widthTiles) shr CHUNK_TILE_EXP,
        (yTile + heightTiles) shr CHUNK_TILE_EXP
    )

fun Level.getChunksFromRectangle(x: Int, y: Int, width: Int, height: Int) =
    getChunksFromChunkRectangle(x shr CHUNK_EXP, y shr CHUNK_EXP, (x + width) shr CHUNK_EXP, (y + height) shr CHUNK_EXP)

fun Level.getChunksFromChunkRectangle(xChunk: Int, yChunk: Int, xChunk2: Int, yChunk2: Int): Sequence<Chunk> {
    val nXChunk = max(0, xChunk)
    val nYChunk = max(0, yChunk)
    val nXChunk2 = min(widthChunks - 1, xChunk2)
    val nYChunk2 = min(heightChunks - 1, yChunk2)
    return (nXChunk..nXChunk2).asSequence().flatMap { x ->
        (nYChunk..nYChunk2).asSequence().map { x to it }
    }.map { getChunkAtChunk(it.first, it.second) }
}

/**
 * @return true if [l] is not already in this level and it is able to be added
 */
fun Level.canAdd(l: LevelObject): Boolean {
    if (l.inLevel && l.level == this)
        return false
    return l !is PhysicalLevelObject || canAdd(l.hitbox, l.x, l.y)
}

/**
 * @return true if a [PhysicalLevelObject] with type [type] at [x], [y] would be able to be added by [add]
 */
fun Level.canAdd(type: PhysicalLevelObjectType<*>, x: Int, y: Int) = canAdd(type.hitbox, x, y)

/**
 * @return true if a [LevelObject] with hitbox [hitbox] at [x], [y] would be able to be added by [add]
 */
fun Level.canAdd(hitbox: Hitbox, x: Int, y: Int): Boolean {
    // TODO check rotation too
    return hitbox == Hitbox.NONE || getCollisionsWith(hitbox, x, y).none()
}

fun Level.canRemove(l: LevelObject): Boolean {
    return l.inLevel && l.level == this
}