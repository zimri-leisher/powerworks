package level

import item.ItemType
import item.weapon.Projectile
import level.block.Block
import level.moving.MovingObject
import level.particle.Particle
import misc.Geometry
import resource.ResourceNode
import resource.ResourceType
import screen.mouse.Mouse
import kotlin.math.max
import kotlin.math.min

/**
 * Gets collisions between the [levelObj] and [Block]s in this [Level]
 *
 * @param predicate the selector for [Block]s to consider collisions with. Defaults to { true }
 * @return a set of [Block]s intersecting the [Hitbox] of [levelObj]
 */
fun Level.getBlockCollisions(levelObj: LevelObject, predicate: (Block) -> Boolean = { true }) =
        getBlockCollisions(levelObj.hitbox, levelObj.xPixel, levelObj.yPixel, predicate)

/**
 * Gets collisions between the [hitbox] at [xPixel], [yPixel] and [Block]s in this [Level]
 *
 * @param hitbox the hitbox to check collisions with
 * @param xPixel the level x pixel of the [hitbox]. [Hitbox.xStart] is added to this to get the true x coordinate of the rectangle
 * @param yPixel the level y pixel of the [hitbox]. [Hitbox.yStart] is added to this to get the true y coordinate of the rectangle
 * @param predicate the selector for [Block]s to consider collisions with. Defaults to { true }
 * @return a set of [Block]s intersecting the [hitbox]
 */
fun Level.getBlockCollisions(hitbox: Hitbox, xPixel: Int, yPixel: Int, predicate: (Block) -> Boolean = { true }): Set<LevelObject> {
    // Blocks won't have a hitbox bigger than their width/height tiles
    val blocks = getIntersectingBlocksFromPixelRectangle(hitbox.xStart + xPixel, hitbox.yStart + yPixel, hitbox.width, hitbox.height)
    return getCollisionsWith(blocks, hitbox, xPixel, yPixel, predicate)
}

/**
 * Gets the [Block] at [xTile], [yTile]
 *
 * @return the [Block] whose base is at [xTile], [yTile]. A block base does not necessarily correspond with the block's [Hitbox],
 * it is just the area in which other [Block]s are unable to be placed
 */
fun Level.getBlockAt(xTile: Int, yTile: Int): Block? {
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
 * Gets [Block]s whose base intersects the rectangle starting at [xPixel], [yPixel] with width [widthPixels] and height [heightPixels]
 *
 * @return a set of [Block]s whose base (not [Hitbox]) intersects the given rectangle and which match the given [predicate] (defaults to { true }).
 * A block base does not necessarily correspond with the block's [Hitbox], it is just the area in which other [Block]s
 * are unable to be placed. To check for [Hitbox] intersections, use [getBlockCollisions]
 */
fun Level.getIntersectingBlocksFromPixelRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, predicate: (Block) -> Boolean = { true }) =
        getIntersectingBlocksFromTileRectangle(xPixel shr 4, yPixel shr 4, widthPixels shr 4, heightPixels shr 4, predicate)

/**
 * Gets [Block]s whose base intersects the rectangle starting at [xTile], [yTile] with width [widthTiles] and height [heightTiles]
 *
 * @return a set of [Block]s whose base (not [Hitbox]) intersects the given rectangle and which match the given [predicate] (defaults to { true }).
 * A block base does not necessarily correspond with the block's [Hitbox], it is just the area in which other [Block]s
 * are unable to be placed. To check for [Hitbox] intersections, use [getBlockCollisions]
 */
fun Level.getIntersectingBlocksFromTileRectangle(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int, predicate: (Block) -> Boolean = { true }): Set<Block> {
    val m = mutableSetOf<Block>()
    for (x in xTile..(xTile + widthTiles)) {
        for (y in yTile..(yTile + heightTiles)) {
            val b = getBlockAt(x, y)
            if (b != null && predicate(b))
                m.add(b)
        }
    }
    return m
}

/**
 * Gets collisions between the square centered on [xPixel], [yPixel] with radius [radius] and [Block]s in this [Level]
 *
 * @return a set of [Block]s whose [Hitbox] intersects the square centered at [xPixel], [yPixel] with the given [radius]
 * (which is the side length / 2), and which match the [predicate] (defaults to { true }). Note this does not check the base (the area other blocks
 * cannot be placed in around this block), only the [Hitbox]
 */
fun Level.getBlockCollisionsInSquareCenteredOn(xPixel: Int, yPixel: Int, radius: Int, predicate: (Block) -> Boolean = { true }): Set<Block> {
    val l = mutableSetOf<Block>()
    for (c in getChunksFromPixelRectangle(xPixel - radius, yPixel - radius, radius * 2, radius * 2)) {
        for (d in c.blocks!!) {
            if (d != null && predicate(d) && Geometry.intersects(xPixel - radius, yPixel - radius, radius * 2, radius * 2, d.xPixel + d.hitbox.xStart, d.yPixel + d.hitbox.yStart, d.hitbox.width, d.hitbox.height)) {
                l.add(d)
            }
        }
    }
    return l
}

/**
 * Gets collisions between the [levelObj] and [MovingObject]s in this [Level]
 *
 * @param predicate the selector for [MovingObject]s to consider collisions with
 * @return a set of [MovingObject]s intersecting the [Hitbox] of [levelObj]
 */
fun Level.getMovingObjectCollisions(levelObj: LevelObject, predicate: (LevelObject) -> Boolean = { true }) =
        getMovingObjectCollisions(levelObj.hitbox, levelObj.xPixel, levelObj.yPixel, predicate)

/**
 * Gets collisions between the [hitbox] at [xPixel], [yPixel] and [MovingObject]s in this [Level]
 *
 * @param hitbox the hitbox to check collisions with
 * @param xPixel the level x pixel of the [hitbox]. [Hitbox.xStart] is added to this to get the true x coordinate of the rectangle
 * @param yPixel the level y pixel of the [hitbox]. [Hitbox.yStart] is added to this to get the true y coordinate of the rectangle
 * @param predicate the selector for [MovingObject]s to consider collisions with
 * @return a set of [MovingObject]s intersecting the [hitbox]
 */
fun Level.getMovingObjectCollisions(hitbox: Hitbox, xPixel: Int, yPixel: Int, predicate: (LevelObject) -> Boolean = { true }): Set<MovingObject> {
    val chunks = getChunksFromPixelRectangle(xPixel + hitbox.xStart, yPixel + hitbox.yStart, hitbox.width, hitbox.height)
    val set = mutableSetOf<LevelObject>()
    for (chunk in chunks) {
        set.addAll(getCollisionsWith(chunk.moving!!, hitbox, xPixel, yPixel, predicate))
        set.addAll(getCollisionsWith(chunk.movingOnBoundary!!, hitbox, xPixel, yPixel, predicate))
    }
    return set as Set<MovingObject>
}

/**
 * Gets collisions between the [levelObj] and [LevelObject]s in this [Level]
 *
 * @param predicate the selector for [LevelObject]s to consider collisions with
 * @return a set of [LevelObject]s intersecting the [Hitbox] of [levelObj]
 */
fun Level.getCollisionsWith(levelObj: LevelObject, predicate: (LevelObject) -> Boolean = { true }) =
        getCollisionsWith(levelObj.hitbox, levelObj.xPixel, levelObj.yPixel, predicate)

/**
 * Gets collisions between the [hitbox] at [xPixel], [yPixel] and [LevelObject]s in this [Level]
 *
 * @param hitbox the hitbox to check collisions with
 * @param xPixel the level x pixel of the [hitbox]. [Hitbox.xStart] is added to this to get the true x coordinate of the rectangle
 * @param yPixel the level y pixel of the [hitbox]. [Hitbox.yStart] is added to this to get the true y coordinate of the rectangle
 * @param predicate the selector for [LevelObject]s to consider collisions with
 * @return a set of [LevelObject]s intersecting the [hitbox]
 */
fun Level.getCollisionsWith(hitbox: Hitbox, xPixel: Int, yPixel: Int, predicate: (LevelObject) -> Boolean = { true }): Set<LevelObject> {
    val set = mutableSetOf<LevelObject>()
    set.addAll(getMovingObjectCollisions(hitbox, xPixel, yPixel, predicate))
    set.addAll(getBlockCollisions(hitbox, xPixel, yPixel, predicate))
    return set
}

/**
 * Gets collisions between the [levelObj] and all [possibleColliders]
 *
 * @param possibleColliders the [L]s to consider when checking collisions
 * @param predicate the selector for [L]s to consider collisions with
 * @return a set of [L]s intersecting the [Hitbox] of [levelObj]
 */
fun <L : LevelObject> Level.getCollisionsWith(possibleColliders: Collection<L>, levelObj: LevelObject, predicate: (L) -> Boolean = { true }) =
        getCollisionsWith(possibleColliders, levelObj.hitbox, levelObj.xPixel, levelObj.yPixel, { it != levelObj && predicate(it) })

/**
 * Gets collisions between the [hitbox] at [xPixel], [yPixel] and [possibleColliders]
 *
 * @param hitbox the hitbox to check collisions with
 * @param xPixel the level x pixel of the [hitbox]. [Hitbox.xStart] is added to this to get the true x coordinate of the rectangle
 * @param yPixel the level y pixel of the [hitbox]. [Hitbox.yStart] is added to this to get the true y coordinate of the rectangle
 * @param predicate the selector for [L]s to consider collisions with
 * @return a set of [L]s intersecting the [hitbox]
 */
fun <L : LevelObject> Level.getCollisionsWith(possibleColliders: Collection<L>, hitbox: Hitbox, xPixel: Int, yPixel: Int, predicate: (L) -> Boolean = { true }) =
        getCollisionsWith(possibleColliders, xPixel + hitbox.xStart, yPixel + hitbox.yStart, hitbox.width, hitbox.height, predicate)

/**
 * Gets collisions between the rectangle at [xPixel], [yPixel], [widthPixels], [heightPixels] and [possibleColliders]
 *
 * @param hitbox the hitbox to check collisions with
 * @param predicate the selector for [L]s to consider collisions with
 * @return a set of [L]s intersecting the [hitbox]
 */
fun <L : LevelObject> Level.getCollisionsWith(possibleColliders: Collection<L>, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, predicate: (L) -> Boolean = { true }): Set<L> {
    val set = mutableSetOf<L>()
    for (l in possibleColliders) {
        if (l.hitbox != Hitbox.NONE) {
            if (predicate(l) && Geometry.intersects(xPixel, yPixel, widthPixels, heightPixels,
                            l.xPixel + l.hitbox.xStart, l.yPixel + l.hitbox.yStart, l.hitbox.width, l.hitbox.height)) {
                set.add(l)
            }
        }
    }
    return set
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
 * Gets [MovingObject]s whose [Hitbox] intersects the rectangle starting at [xPixel], [yPixel] with width [widthPixels] and height [heightPixels]
 *
 * @return a set of [MovingObject]s whose [Hitbox] intersects the given rectangle and which match the given [predicate] (defaults to { true })
 */
fun Level.getMovingObjectCollisionsFromPixelRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, predicate: (MovingObject) -> Boolean = { true }): Set<MovingObject> {
    val set = mutableSetOf<LevelObject>()
    for (chunk in getChunksFromPixelRectangle(xPixel, yPixel, widthPixels, heightPixels)) {
        set.addAll(getCollisionsWith(chunk.moving!!, xPixel, yPixel, widthPixels, heightPixels, predicate))
        set.addAll(getCollisionsWith(chunk.movingOnBoundary!!, xPixel, yPixel, widthPixels, heightPixels, predicate))
    }
    return set as Set<MovingObject>
}

/**
 * @return a set of [MovingObject]s in this [Level] whose [Hitbox] intersects the square centered at [xPixel], [yPixel] with the given [radius]
 * (which is the side length / 2), and which match the [predicate] (defaults to { true })
 */
fun Level.getMovingObjectCollisionsInSquareCenteredOn(xPixel: Int, yPixel: Int, radius: Int, predicate: (MovingObject) -> Boolean = { true }): Set<MovingObject> {
    val set = mutableSetOf<MovingObject>()
    for (chunk in getChunksFromPixelRectangle(xPixel - radius, yPixel - radius, radius * 2, radius * 2)) {
        set.addAll(getCollisionsWith(chunk.moving!!, xPixel, yPixel, widthPixels, heightPixels, predicate))
        set.addAll(getCollisionsWith(chunk.movingOnBoundary!!, xPixel, yPixel, widthPixels, heightPixels, predicate))
    }
    return set
}

/**
 * @return a set of [MovingObject]s in this [Level] whose [Hitbox] contains the given point, and which match the given
 * [predicate] (defaults to { true })
 */
fun Level.getMovingObjectCollisionsWithPoint(xPixel: Int, yPixel: Int, predicate: (MovingObject) -> Boolean = { true }): Set<MovingObject> {
    val set = mutableSetOf<MovingObject>()
    val chunk = getChunkFromPixel(xPixel, yPixel)
    set.addAll(getCollisionsWith(chunk.moving!!, xPixel, yPixel, 0, 0, predicate))
    set.addAll(getCollisionsWith(chunk.movingOnBoundary!!, xPixel, yPixel, 0, 0, predicate))
    return set
}

/**
 * Updates the [ResourceNode.attachedNodes] of the given [node]
 */
fun Level.updateResourceNodeAttachments(node: ResourceNode) {
    val attached = getResourceNodesAt(node.xTile + Geometry.getXSign(node.dir), node.yTile + Geometry.getYSign(node.dir), { it.resourceCategory == node.resourceCategory })
            .filter { it.dir == Geometry.getOppositeAngle(node.dir) }
    node.attachedNodes = attached
    val networks = attached.map { it.network }.distinct()
    for(network in networks) {
        node.network
    }
}

/**
 * @return a set of [ResourceNode]s in this [Level] at [xTile], [yTile] which match the given [predicate] (defaults to { true })
 */
fun Level.getResourceNodesAt(xTile: Int, yTile: Int, predicate: (ResourceNode) -> Boolean = { true }): Set<ResourceNode> {
    val set = mutableSetOf<ResourceNode>()
    val chunk = getChunkFromTile(xTile, yTile)
    chunk.resourceNodes!!.forEach { it.filter { predicate(it) && it.xTile == xTile && it.yTile == yTile }.forEach { set.add(it) } }
    return set
}

/**
 * @return a set of [DroppedItem]s in this [Level] whose [Hitbox] intersects the rectangle at [xPixel], [yPixel] with radius
 * [radius] and which matches the given [predicate] (defaults to { true })
 */
fun Level.getDroppedItemCollisionsInSquareCenteredOn(xPixel: Int, yPixel: Int, radius: Int, predicate: (DroppedItem) -> Boolean = { true }): Set<DroppedItem> {
    val set = mutableSetOf<DroppedItem>()
    for (chunk in getChunksFromPixelRectangle(xPixel - radius, yPixel - radius, radius * 2, radius * 2)) {
        set.addAll(getCollisionsWith(chunk.droppedItems!!, xPixel, yPixel, widthPixels, heightPixels, predicate))
    }
    return set
}

/**
 * @return the [Tile] at [xTile], [yTile]
 */
fun Level.getTileAt(xTile: Int, yTile: Int) = getChunkFromTile(xTile, yTile).getTile(xTile, yTile)

/**
 * Gets the [LevelObject]s at [xPixel], [yPixel] matching the given [predicate] (defaults to { true })
 *
 * @return a set of [LevelObject]s in this [Level] at [xPixel], [yPixel] which match the given [predicate]. Note, for [Block]s,
 * this checks the base of the [Block] (i.e. the tiles around the block in which no other block may be), and for [MovingObject]s,
 * it checks the [Hitbox]. The [MovingObject]s will be first in the order of the set, then the [Block]s
 */
fun Level.getLevelObjectsAt(xPixel: Int, yPixel: Int, predicate: (LevelObject) -> Boolean = { true }): Set<LevelObject> {
    val set = mutableSetOf<LevelObject>()
    getMovingObjectCollisionsWithPoint(xPixel, yPixel, predicate).let { set.addAll(it) }
    getBlockFromPixel(xPixel, yPixel)?.let { if (predicate(it)) set.add(it) }
    return set
}

/**
 * Updates the [Chunk]s the given [moving] thinks it is in. All [MovingObject] store the chunk they are in and any chunks
 * that its [Hitbox] intersects, and they need to be updated when it moves or its [Hitbox] changes
 */
fun Level.updateChunkOf(moving: MovingObject) {
    val currentChunk = getChunkAt(moving.xChunk, moving.yChunk)
    if (moving.hitbox != Hitbox.NONE) {
        val intersectingChunks = getChunksFromPixelRectangle(
                moving.hitbox.xStart + moving.xPixel, moving.hitbox.yStart + moving.yPixel, moving.hitbox.width, moving.hitbox.height).toMutableSet()
        intersectingChunks.remove(currentChunk)
        if (moving.intersectingChunks != intersectingChunks) {
            moving.intersectingChunks.forEach { it.movingOnBoundary!!.remove(moving) }
            intersectingChunks.forEach { it.movingOnBoundary!!.add(moving) }
            moving.intersectingChunks = intersectingChunks
        }
    }
    if (moving.currentChunk != currentChunk) {
        moving.currentChunk?.removeMoving(moving)
        moving.currentChunk = currentChunk
        currentChunk.addMoving(moving)
    }
}

fun Level.loadChunkAt(xChunk: Int, yChunk: Int): Chunk {
    val c = chunks[xChunk + yChunk * widthChunks]
    c.load(genBlocks(xChunk, yChunk), genTiles(xChunk, yChunk))
    return c
}

fun Level.getChunkAt(xChunk: Int, yChunk: Int, load: Boolean = true): Chunk {
    val c = chunks[xChunk + yChunk * this.widthChunks]
    if (load && !c.loaded) {
        loadChunkAt(xChunk, yChunk)
    }
    return c
}

fun Level.getChunkFromTile(xTile: Int, yTile: Int, load: Boolean = true) =
        getChunkAt(xTile shr CHUNK_TILE_EXP, yTile shr CHUNK_TILE_EXP, load)

fun Level.getChunkFromPixel(xPixel: Int, yPixel: Int, load: Boolean = true) =
        getChunkFromTile(xPixel shr 4, yPixel shr 4, load)

fun Level.getChunksFromChunkRectangle(xChunk: Int, yChunk: Int, xChunk2: Int, yChunk2: Int, load: Boolean = true): Set<Chunk> {
    val l = mutableSetOf<Chunk>()
    val nXChunk = max(0, xChunk)
    val nYChunk = max(0, yChunk)
    val nXChunk2 = min(widthChunks - 1, xChunk2)
    val nYChunk2 = min(heightChunks - 1, yChunk2)
    for (x in nXChunk..nXChunk2) {
        for (y in nYChunk..nYChunk2) {
            l.add(getChunkAt(x, y, load))
        }
    }
    return l
}

fun Level.getChunksFromTileRectangle(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int) =
        getChunksFromChunkRectangle(xTile shr CHUNK_TILE_EXP, yTile shr CHUNK_TILE_EXP, (xTile + widthTiles) shr CHUNK_TILE_EXP, (yTile + heightTiles) shr CHUNK_TILE_EXP)

fun Level.getChunksFromPixelRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): Set<Chunk> =
        getChunksFromChunkRectangle(xPixel shr CHUNK_PIXEL_EXP, yPixel shr CHUNK_PIXEL_EXP, (xPixel + widthPixels) shr CHUNK_PIXEL_EXP, (yPixel + heightPixels) shr CHUNK_PIXEL_EXP)

/**
 * Does everything necessary to put the object to the level
 * @return if the object was added
 */
fun Level.add(l: LevelObject): Boolean {
    if (l is Block) {
        if (l.getCollisions(l.xPixel, l.yPixel, level = this).isNotEmpty()) {
            return false
        }
        for (x in 0 until l.type.widthTiles) {
            for (y in 0 until l.type.heightTiles) {
                getChunkFromTile(l.xTile + x, l.yTile + y).setBlock(l, l.xTile + x, l.yTile + y, (x == 0 && y == 0))
            }
        }
        l.level = this
        l.inLevel = true
        return true
    } else if (l is MovingObject) {
        if (l is DroppedItem) {
            // get nearest dropped item of the same type that is not a full stack
            val d = getDroppedItemCollisionsInSquareCenteredOn(l.xPixel, l.yPixel, Mouse.DROPPED_ITEM_PICK_UP_RANGE) { it.itemType == l.itemType && it.quantity < it.itemType.maxStack }.maxBy { it.quantity }
            if (d != null) {
                if (d.quantity + l.quantity <= l.itemType.maxStack) {
                    d.quantity += l.quantity
                    // dont set in level because it was never technically called into existence
                    return true
                } else {
                    // if we won't be able to finish off the rest of the stack
                    if (l.getCollisions(l.xPixel, l.yPixel, level = this).isNotEmpty()) {
                        return false
                    }
                    l.quantity -= (l.itemType.maxStack - d.quantity)
                    d.quantity = l.itemType.maxStack
                }
            }
            if (l.getCollisions(l.xPixel, l.yPixel, level = this).isEmpty()) {
                if (l.hitbox != Hitbox.NONE) {
                    l.intersectingChunks.forEach { it.movingOnBoundary!!.add(l) }
                }
                updateChunkOf(l)
                l.level = this
                l.inLevel = true
                l.currentChunk!!.addDroppedItem(l)
                return true
            }
            return false
        } else {
            if (l.getCollisions(l.xPixel, l.yPixel, level = this).isNotEmpty())
                return false
            if (l.hitbox != Hitbox.NONE) {
                l.intersectingChunks.forEach { it.movingOnBoundary!!.add(l) }
            }
            updateChunkOf(l)
            l.level = this
            l.inLevel = true
            return true
        }
    }
    return false
}

/**
 * Tries to 'materialize' this resource where specified. Note: most resources have no physical representation
 * other than some purely decorative particles
 * @return the quantity of the resource that was able to be materialized
 */
fun Level.add(xPixel: Int, yPixel: Int, r: ResourceType, quantity: Int): Int {
    if (r is ItemType) {
        if (!add(DroppedItem(xPixel, yPixel, r, quantity)))
            return 0
        return quantity
    }
    return 0
}

/**
 * Tries to add a resource node to the level.
 * If there was already a node at the same position with the same direction, attached container and resource
 * category, it will remove the previous one before finishing addition
 */
fun Level.add(resourceNode: ResourceNode) {
    if (resourceNode.inLevel)
        return
    val c = getChunkFromTile(resourceNode.xTile, resourceNode.yTile)
    val previousNode: ResourceNode?
    previousNode = c.resourceNodes!![resourceNode.resourceCategory.ordinal].firstOrNull {
        it.xTile == resourceNode.xTile && it.yTile == resourceNode.yTile && it.dir == resourceNode.dir && it.attachedContainer == resourceNode.attachedContainer
    }
    if (previousNode != null) {
        remove(previousNode)
    }
    c.addResourceNode(resourceNode)
    resourceNode.inLevel = true
    updateResourceNodeAttachments(resourceNode)
    for (x in -1..1) {
        for (y in -1..1) {
            if (Math.abs(x) != Math.abs(y))
                getResourceNodesAt(resourceNode.xTile + x, resourceNode.yTile + y).forEach { updateResourceNodeAttachments(it) }
        }
    }
}

fun Level.remove(l: LevelObject): Boolean {
    if (l.inLevel) {
        if (l is Block) {
            for (x in 0 until l.type.widthTiles) {
                for (y in 0 until l.type.heightTiles) {
                    getChunkFromTile(l.xTile + x, l.yTile + y).removeBlock(l, l.xTile + x, l.yTile + y, (x == 0 && y == 0))
                }
            }
            l.inLevel = false
            return true
        } else if (l is MovingObject) {
            if (l is DroppedItem) {
                l.currentChunk!!.removeDroppedItem(l)
            }
            if (l.hitbox != Hitbox.NONE)
                l.intersectingChunks.forEach { it.movingOnBoundary!!.remove(l) }
            l.currentChunk!!.removeMoving(l)
            l.inLevel = false
        }
        return true
    }
    return false
}

/**
 * Removes a [Projectile] from this [Level]
 *
 * @return false if there was no matching projectile
 */
fun Level.remove(projectile: Projectile) = projectiles.remove(projectile)

/**
 * Adds a [Projectile] to this [Level]
 */
fun Level.add(projectile: Projectile) {
    projectiles.add(projectile)
}

/**
 * Adds a particle to the level. Particles are temporary and purely decorative, they do not get saved
 */
fun Level.add(particle: Particle) {
    particles.add(particle)
}

/**
 * Removes a particle from the level
 */
fun Level.remove(particle: Particle) {
    particles.remove(particle)
}

fun Level.remove(resourceNode: ResourceNode) {
    if (!resourceNode.inLevel)
        return
    val c = getChunkFromTile(resourceNode.xTile, resourceNode.yTile)
    c.removeResourceNode(resourceNode)
    resourceNode.inLevel = false
    for (x in -1..1) {
        for (y in -1..1) {
            if (Math.abs(x) != Math.abs(y))
                getResourceNodesAt(resourceNode.xTile + x, resourceNode.yTile + y).forEach { updateResourceNodeAttachments(it) }
        }
    }
}