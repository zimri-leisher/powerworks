package level

import item.ItemType
import level.block.Block
import level.moving.MovingObject
import level.tile.Tile
import misc.Geometry
import resource.ResourceNode
import resource.ResourceType
import kotlin.math.max
import kotlin.math.min

fun Level.isChunkWithinBounds(xChunk: Int, yChunk: Int) = isTileWithinBounds(xChunk shl CHUNK_TILE_EXP, yChunk shl CHUNK_TILE_EXP)

fun Level.isTileWithinBounds(xTile: Int, yTile: Int) = isPixelWithinBounds(xTile shl 4, yTile shl 4)

fun Level.isPixelWithinBounds(xPixel: Int, yPixel: Int): Boolean {
    if (xPixel < 0 || yPixel < 0)
        return false
    if (xPixel >= widthPixels || yPixel >= heightPixels)
        return false
    return true
}

fun Level.isChunkRectangleInBounds(xChunk: Int, yChunk: Int, widthChunks: Int, heightChunks: Int) =
        isTileRectangleInBounds(xChunk shl CHUNK_TILE_EXP, yChunk shl CHUNK_TILE_EXP, widthTiles shl CHUNK_TILE_EXP, heightChunks shl CHUNK_TILE_EXP)

fun Level.isTileRectangleInBounds(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int) =
        isPixelRectangleInBounds(xTile shl 4, yTile shl 4, widthTiles shl 4, heightTiles shl 4)

fun Level.isPixelRectangleInBounds(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) =
        isPixelWithinBounds(xPixel, yPixel) ||
                isPixelWithinBounds(xPixel + widthPixels, yPixel + heightPixels)

/**
 * Gets the [Block] at [xTile], [yTile]
 *
 * @return the [Block] whose base is at [xTile], [yTile]. A block base does not necessarily correspond with the block's [Hitbox],
 * it is just the area in which other [Block]s are unable to be placed
 */
fun Level.getBlockAt(xTile: Int, yTile: Int): Block? {
    if (!isTileWithinBounds(xTile, yTile)) {
        println("out of bounds")
        return null
    }
    return getChunkFromTile(xTile, yTile).getBlock(xTile, yTile)
}

/**
 * Gets the [Block] at [xPixel], [yPixel]
 *
 * @return the [Block] whose base is at [xPixel], [yPixel]. A block base does not necessarily correspond with the block's [Hitbox],
 * it is just the area in which other [Block]s are unable to be placed
 */
fun Level.getBlockFromPixel(xPixel: Int, yPixel: Int) =
        getBlockAt(xPixel shr 4, yPixel shr 4)

/**
 * Gets collisions between the [levelObj] and [Block]s in this [Level]
 *
 * @return a sequence of [Block]s intersecting the [Hitbox] of [levelObj]
 */
fun Level.getBlockCollisions(levelObj: LevelObject) =
        getBlockCollisions(levelObj.hitbox, levelObj.xPixel, levelObj.yPixel)

/**
 * Gets collisions between the [hitbox] at [xPixel], [yPixel] and [Block]s in this [Level]
 *
 * @param hitbox the hitbox to check collisions with
 * @param xPixel the level x pixel of the [hitbox]. [Hitbox.xStart] is added to this to get the true x coordinate of the rectangle
 * @param yPixel the level y pixel of the [hitbox]. [Hitbox.yStart] is added to this to get the true y coordinate of the rectangle
 * @return a sequence of [Block]s intersecting the [hitbox]
 */
fun Level.getBlockCollisions(hitbox: Hitbox, xPixel: Int, yPixel: Int) =
        getBlockCollisions(xPixel + hitbox.xStart, yPixel + hitbox.yStart, hitbox.width, hitbox.height)

fun Level.getBlockCollisions(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): Sequence<Block> {
    // Blocks won't have a hitbox bigger than their width/height tiles
    return getIntersectingBlocksFromPixelRectangle(xPixel, yPixel, widthPixels, heightPixels).filter {
        Geometry.intersects(it.xPixel + it.hitbox.xStart, it.yPixel + it.hitbox.yStart, it.hitbox.width, it.hitbox.height,
                xPixel, yPixel, widthPixels, heightPixels)
    }
}

/**
 * Gets [Block]s whose base intersects the rectangle starting at [xPixel], [yPixel] with width [widthPixels] and height [heightPixels]
 *
 * @return a sequence of [Block]s whose base (not [Hitbox]) intersects the given rectangle.
 * A block base does not necessarily correspond with the block's [Hitbox], it is just the area in which other [Block]s
 * are unable to be placed. To check for [Hitbox] intersections, use [getBlockCollisions]
 */
fun Level.getIntersectingBlocksFromPixelRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) =
        getIntersectingBlocksFromTileRectangle(xPixel shr 4, yPixel shr 4, widthPixels shr 4, heightPixels shr 4)

/**
 * Gets [Block]s whose base intersects the rectangle starting at [xTile], [yTile] with width [widthTiles] and height [heightTiles]
 *
 * @return a set of [Block]s whose base (not [Hitbox]) intersects the given rectangle.
 * A block base does not necessarily correspond with the block's [Hitbox], it is just the area in which other [Block]s
 * are unable to be placed. To check for [Hitbox] intersections, use [getBlockCollisions]
 */
fun Level.getIntersectingBlocksFromTileRectangle(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int): Sequence<Block> {
    if (!isTileRectangleInBounds(xTile, yTile, widthTiles, heightTiles))
        return emptySequence()
    return (xTile..(xTile + widthTiles)).asSequence().flatMap { x ->
        (yTile..(yTile + heightTiles)).asSequence().map { x to it }
    }
            .mapNotNull { getBlockAt(it.first, it.second) }
}

/**
 * Gets collisions between the square centered on [xPixel], [yPixel] with radius [radius] and [Block]s in this [Level]
 *
 * @return a set of [Block]s whose [Hitbox] intersects the square centered at [xPixel], [yPixel] with the given [radius]
 * (which is the side length / 2). Note this does not check the base (the area other blocks
 * cannot be placed in around this block), only the [Hitbox]
 */
fun Level.getBlockCollisionsInSquareCenteredOn(xPixel: Int, yPixel: Int, radius: Int): Sequence<Block> {
    if (!isPixelRectangleInBounds(xPixel - radius, yPixel - radius, radius * 2, radius * 2))
        return emptySequence()
    return getChunksFromPixelRectangle(xPixel - radius, yPixel - radius, radius * 2, radius * 2).flatMap {
        it.data.blocks.asSequence()
    }.filterNotNull().filter {
        Geometry.intersects(xPixel - radius, yPixel - radius, radius * 2, radius * 2,
                it.xPixel + it.hitbox.xStart, it.yPixel + it.hitbox.yStart, it.hitbox.width, it.hitbox.height)
    }
}

fun Level.getBlockCollisionsWithPoint(xPixel: Int, yPixel: Int): Sequence<Block> {
    if (!isPixelWithinBounds(xPixel, yPixel))
        return emptySequence()
    return getChunkFromPixel(xPixel, yPixel).data.blocks.asSequence().filterNotNull().filter {
        it.hitbox != Hitbox.NONE &&
                Geometry.intersects(it.xPixel + it.hitbox.xStart, it.yPixel + it.hitbox.yStart, it.hitbox.width, it.hitbox.height,
                        xPixel, yPixel, 0, 0)
    }
}

/**
 * Gets collisions between the [levelObj] and [MovingObject]s in this [Level]
 *
 * @return a set of [MovingObject]s intersecting the [Hitbox] of [levelObj]
 */
fun Level.getMovingObjectCollisions(levelObj: LevelObject) =
        getMovingObjectCollisions(levelObj.type, levelObj.xPixel, levelObj.yPixel)

fun Level.getMovingObjectCollisions(levelObjType: LevelObjectType<*>, xPixel: Int, yPixel: Int) =
        getMovingObjectCollisions(levelObjType.hitbox, xPixel, yPixel)

fun Level.getMovingObjectCollisions(hitbox: Hitbox, xPixel: Int, yPixel: Int) =
        getMovingObjectCollisions(xPixel + hitbox.xStart, yPixel + hitbox.yStart, hitbox.width, hitbox.height)

/**
 * @return a sequence of [MovingObject]s in this [Level] whose [Hitbox] intersects the square centered at [xPixel], [yPixel] with the given [radius]
 * (which is the side length / 2)
 */
fun Level.getMovingObjectCollisionsInSquareCenteredOn(xPixel: Int, yPixel: Int, radius: Int) =
        getMovingObjectCollisions(xPixel - radius, yPixel - radius, radius * 2, radius * 2)

/**
 * Gets [MovingObject]s whose [Hitbox] intersects the rectangle starting at [xPixel], [yPixel] with width [widthPixels] and height [heightPixels]
 *
 * @return a set of [MovingObject]s whose [Hitbox] intersects the given rectangle
 */
fun Level.getMovingObjectCollisions(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): Sequence<MovingObject> {
    if (!isPixelRectangleInBounds(xPixel, yPixel, widthPixels, heightPixels))
        return emptySequence()
    return getChunksFromPixelRectangle(xPixel, yPixel, widthPixels, heightPixels).flatMap {
        it.data.moving.getCollisionsWith(xPixel, yPixel, widthPixels, heightPixels) +
                it.data.movingOnBoundary.getCollisionsWith(xPixel, yPixel, widthPixels, heightPixels)
    }
}

/**
 * @return a sequence of [MovingObject]s in this [Level] whose [Hitbox] contains the given point
 */
fun Level.getMovingObjectCollisionsWithPoint(xPixel: Int, yPixel: Int): Sequence<MovingObject> {
    if (!isPixelWithinBounds(xPixel, yPixel))
        return emptySequence()
    return getChunkFromPixel(xPixel, yPixel).let {
        it.data.moving.getCollisionsWith(xPixel, yPixel, 0, 0) +
                it.data.movingOnBoundary.getCollisionsWith(xPixel, yPixel, 0, 0)
    }
}

fun Level.getDroppedItemCollisions(levelObj: LevelObject) =
        getDroppedItemCollisions(levelObj.type, levelObj.xPixel, levelObj.yPixel)

fun Level.getDroppedItemCollisions(levelObjType: LevelObjectType<*>, xPixel: Int, yPixel: Int) =
        getDroppedItemCollisions(levelObjType.hitbox, xPixel, yPixel)

fun Level.getDroppedItemCollisions(hitbox: Hitbox, xPixel: Int, yPixel: Int) =
        getDroppedItemCollisions(xPixel + hitbox.xStart, yPixel + hitbox.yStart, hitbox.width, hitbox.height)

fun Level.getDroppedItemCollisions(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): Sequence<DroppedItem> {
    if (!isPixelRectangleInBounds(xPixel, yPixel, widthPixels, heightPixels))
        return emptySequence()
    return getChunksFromPixelRectangle(xPixel, yPixel, widthPixels, heightPixels).asSequence().flatMap { it.data.droppedItems.getCollisionsWith(xPixel, yPixel, widthPixels, heightPixels) }
}

/**
 * @return a sequence of [DroppedItem]s in this [Level] whose [Hitbox] intersects the rectangle at [xPixel], [yPixel] with radius
 * [radius]
 */
fun Level.getDroppedItemCollisionsInSquareCenteredOn(xPixel: Int, yPixel: Int, radius: Int) =
        getDroppedItemCollisions(xPixel - radius, yPixel - radius, radius * 2, radius * 2)

fun Level.getDroppedItemCollisionsWithPoint(xPixel: Int, yPixel: Int): Sequence<DroppedItem> {
    if (!isPixelWithinBounds(xPixel, yPixel))
        return emptySequence()
    return getChunkFromPixel(xPixel, yPixel).data.droppedItems.getCollisionsWith(xPixel, yPixel, 0, 0)
}

/**
 * Gets collisions between the [levelObj] and [LevelObject]s in this [Level]
 *
 * @param predicate the selector for [LevelObject]s to consider collisions with
 * @return a sequence of [LevelObject]s intersecting the [Hitbox] of [levelObj]
 */
fun Level.getCollisionsWith(levelObj: LevelObject) =
        getCollisionsWith(levelObj.hitbox, levelObj.xPixel, levelObj.yPixel)

/**
 * Gets collisions between the [hitbox] at [xPixel], [yPixel] and [LevelObject]s in this [Level]
 *
 * @param hitbox the hitbox to check collisions with
 * @param xPixel the level x pixel of the [hitbox]. [Hitbox.xStart] is added to this to get the true x coordinate of the rectangle
 * @param yPixel the level y pixel of the [hitbox]. [Hitbox.yStart] is added to this to get the true y coordinate of the rectangle
 * @return a sequence of [LevelObject]s intersecting the [hitbox]
 */
fun Level.getCollisionsWith(hitbox: Hitbox, xPixel: Int, yPixel: Int) =
        getCollisionsWith(xPixel + hitbox.xStart, yPixel + hitbox.yStart, hitbox.width, hitbox.height)

fun Level.getCollisionsWith(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): Sequence<LevelObject> =
        getBlockCollisions(xPixel, yPixel, widthPixels, heightPixels) + getMovingObjectCollisions(xPixel, yPixel, widthPixels, heightPixels) + getDroppedItemCollisions(xPixel, yPixel, widthPixels, heightPixels)

/**
 * Gets collisions between the [levelObj] and all [this@getCollisionsWith]
 *
 * @param this@getCollisionsWith the [L]s to consider when checking collisions
 * @param predicate the selector for [L]s to consider collisions with
 * @return a sequence of [L]s intersecting the [Hitbox] of [levelObj]
 */
fun <L : LevelObject> Iterable<L>.getCollisionsWith(levelObj: LevelObject) =
        getCollisionsWith(levelObj.hitbox, levelObj.xPixel, levelObj.yPixel).filter { it != levelObj }

/**
 * Gets collisions between the [hitbox] at [xPixel], [yPixel] and [this@getCollisionsWith]
 *
 * @param hitbox the hitbox to check collisions with
 * @param xPixel the level x pixel of the [hitbox]. [Hitbox.xStart] is added to this to get the true x coordinate of the rectangle
 * @param yPixel the level y pixel of the [hitbox]. [Hitbox.yStart] is added to this to get the true y coordinate of the rectangle
 * @return a sequence of [L]s intersecting the [hitbox]
 */
fun <L : LevelObject> Iterable<L>.getCollisionsWith(hitbox: Hitbox, xPixel: Int, yPixel: Int) =
        getCollisionsWith(xPixel + hitbox.xStart, yPixel + hitbox.yStart, hitbox.width, hitbox.height)

fun <L : LevelObject> Iterable<L>.getCollisionsWith(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): Sequence<L> {
    if (none()) {
        return emptySequence()
    }
    return asSequence().filter {
        it.hitbox != Hitbox.NONE && Geometry.intersects(xPixel, yPixel, widthPixels, heightPixels,
                it.xPixel + it.hitbox.xStart, it.yPixel + it.hitbox.yStart, it.hitbox.width, it.hitbox.height)
    }
}

fun Level.getCollisionsWithPoint(xPixel: Int, yPixel: Int): Sequence<LevelObject> {
    return getBlockCollisionsWithPoint(xPixel, yPixel) + getMovingObjectCollisionsWithPoint(xPixel, yPixel) + getDroppedItemCollisionsWithPoint(xPixel, yPixel)
}

/**
 * @return true if the [hitbox] at [xPixel], [yPixel] collides with the hitbox of the [levelObj]
 */
fun Level.doHitboxesCollide(hitbox: Hitbox, xPixel: Int, yPixel: Int, levelObj: LevelObject) =
        doHitboxesCollide(hitbox, xPixel, yPixel, levelObj.hitbox, levelObj.xPixel, levelObj.yPixel)

/**
 * @return true if the [hitbox1] at [xPixel1], [yPixel1] collides with the [hitbox2] at [xPixel2], [yPixel2]
 */
fun Level.doHitboxesCollide(hitbox1: Hitbox, xPixel1: Int, yPixel1: Int,
                            hitbox2: Hitbox, xPixel2: Int, yPixel2: Int): Boolean {
    return Geometry.intersects(xPixel1 + hitbox1.xStart, yPixel1 + hitbox1.yStart, hitbox1.width, hitbox1.height,
            xPixel2 + hitbox2.xStart, yPixel2 + hitbox2.yStart, hitbox2.width, hitbox2.height)
}

/**
 * Updates the [ResourceNode.attachedNode] of the given [node]
 */
fun Level.updateResourceNodeAttachments(node: ResourceNode) {
    val attached = getResourceNodesAt(node.xTile + Geometry.getXSign(node.dir), node.yTile + Geometry.getYSign(node.dir), { it.resourceCategory == node.resourceCategory })
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
 * @return a set of [ResourceNode]s in this [Level] at [xTile], [yTile] which match the given [predicate] (defaults to { true })
 */
fun Level.getResourceNodesAt(xTile: Int, yTile: Int, predicate: (ResourceNode) -> Boolean = { true }): Set<ResourceNode> {
    val set = mutableSetOf<ResourceNode>()
    if (!isTileWithinBounds(xTile, yTile))
        return emptySet()
    val chunk = getChunkFromTile(xTile, yTile)
    chunk.data.resourceNodes.forEach { it.filter { predicate(it) && it.xTile == xTile && it.yTile == yTile }.forEach { set.add(it) } }
    return set
}

/**
 * @return the [Tile] at [xTile], [yTile]
 */
fun Level.getTileAt(xTile: Int, yTile: Int): Tile {
    if (!isTileWithinBounds(xTile, yTile))
        throw Exception("No tile at $xTile, $yTile in $this")
    return getChunkFromTile(xTile, yTile).getTile(xTile, yTile)
}

/**
 * Gets the [LevelObject]s at [xPixel], [yPixel] matching the given [predicate] (defaults to { true })
 *
 * @return a sequence of [LevelObject]s in this [Level] at [xPixel], [yPixel]. Note, for [Block]s,
 * this checks the base of the [Block] (i.e. the tiles around the block in which no other block may be), and for [MovingObject]s,
 * it checks the [Hitbox]. The [MovingObject]s will be first in the order of the set, then the [Block]s
 */
fun Level.getLevelObjectsAt(xPixel: Int, yPixel: Int): Sequence<LevelObject> =
        (getBlockFromPixel(xPixel, yPixel)?.let { sequenceOf(it) } ?: emptySequence<LevelObject>()) +
                getMovingObjectCollisionsWithPoint(xPixel, yPixel) +
                getDroppedItemCollisionsWithPoint(xPixel, yPixel)

fun Level.getChunkAt(xChunk: Int, yChunk: Int): Chunk {
    if (!isChunkWithinBounds(xChunk, yChunk)) {
        throw Exception("No chunk at $xChunk, $yChunk in $this")
    }
    return data.chunks[xChunk + yChunk * widthChunks]
}

fun Level.getChunkFromTile(xTile: Int, yTile: Int) =
        getChunkAt(xTile shr CHUNK_TILE_EXP, yTile shr CHUNK_TILE_EXP)

fun Level.getChunkFromPixel(xPixel: Int, yPixel: Int) =
        getChunkFromTile(xPixel shr 4, yPixel shr 4)

fun Level.getChunksFromTileRectangle(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int) =
        getChunksFromChunkRectangle(xTile shr CHUNK_TILE_EXP, yTile shr CHUNK_TILE_EXP, (xTile + widthTiles) shr CHUNK_TILE_EXP, (yTile + heightTiles) shr CHUNK_TILE_EXP)

fun Level.getChunksFromPixelRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) =
        getChunksFromChunkRectangle(xPixel shr CHUNK_PIXEL_EXP, yPixel shr CHUNK_PIXEL_EXP, (xPixel + widthPixels) shr CHUNK_PIXEL_EXP, (yPixel + heightPixels) shr CHUNK_PIXEL_EXP)

fun Level.getChunksFromChunkRectangle(xChunk: Int, yChunk: Int, xChunk2: Int, yChunk2: Int): Sequence<Chunk> {
    val nXChunk = max(0, xChunk)
    val nYChunk = max(0, yChunk)
    val nXChunk2 = min(widthChunks - 1, xChunk2)
    val nYChunk2 = min(heightChunks - 1, yChunk2)
    return (nXChunk..nXChunk2).asSequence().flatMap { x ->
        (nYChunk..nYChunk2).asSequence().map { x to it }
    }.map { getChunkAt(it.first, it.second) }
}

/**
 * @return true if [l] is not already in this level and it is able to be added
 */
fun Level.canAdd(l: LevelObject): Boolean {
    if (l.inLevel && l.level == this)
        return false
    return canAdd(l.hitbox, l.xPixel, l.yPixel)
}

/**
 * @return true if a [LevelObject] with type [type] at [xPixel], [yPixel] would be able to be added by [add]
 */
fun Level.canAdd(type: LevelObjectType<*>, xPixel: Int, yPixel: Int) = canAdd(type.hitbox, xPixel, yPixel)

/**
 * @return true if a [LevelObject] with hitbox [hitbox] at [xPixel], [yPixel] would be able to be added by [add]
 */
fun Level.canAdd(hitbox: Hitbox, xPixel: Int, yPixel: Int): Boolean {
    // TODO check rotation too
    return hitbox == Hitbox.NONE || getCollisionsWith(hitbox, xPixel, yPixel).none()
}

fun Level.canRemove(l: LevelObject): Boolean {
    return l.inLevel && l.level == this
}

/**
 * Tries to 'materialize' this resource where specified. Note: most resources have no physical representation
 * other than some purely decorative particles
 * @return the quantity of the resource that was able to be materialized
 */
fun Level.add(xPixel: Int, yPixel: Int, r: ResourceType, quantity: Int): Int {
    // TODO make this materialize it within a certain range so the coords dont have to be precise
    if (r is ItemType) {
        if (!add(DroppedItem(xPixel, yPixel, r, quantity)))
            return 0
        return quantity
    }
    return 0
}