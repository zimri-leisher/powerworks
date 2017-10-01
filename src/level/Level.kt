package level

import graphics.Renderer
import io.InputManager
import io.Mouse
import io.MouseMovementListener
import io.PressType
import level.block.Block
import level.block.GhostBlock
import level.moving.MovingObject
import level.tile.Tile
import main.Game
import misc.GeometryHelper
import screen.CameraMovementListener
import screen.DebugOverlay
import screen.GUIView
import screen.HUD
import java.awt.Rectangle
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

const val CHUNK_SIZE_TILES = 8
val CHUNK_TILE_EXP = (Math.log(CHUNK_SIZE_TILES.toDouble()) / Math.log(2.0)).toInt()
val CHUNK_PIXEL_EXP = CHUNK_TILE_EXP + 4
val CHUNK_SIZE_PIXELS = CHUNK_SIZE_TILES shl 4

abstract class Level(seed: Long, val widthTiles: Int, val heightTiles: Int) : CameraMovementListener, MouseMovementListener {

    val heightPixels = heightTiles shl 4
    val widthPixels = widthTiles shl 4
    val heightChunks = heightTiles shr CHUNK_TILE_EXP
    val widthChunks = widthTiles shr CHUNK_TILE_EXP

    val rand: Random = Random(seed)

    val chunks: Array<Chunk>
    val loadedChunks = CopyOnWriteArrayList<Chunk>()

    private var viewBeingInteractedWith: GUIView? = null
    private val _views = mutableListOf<GUIView>()
    val views = object : MutableList<GUIView> by _views {
        override fun add(element: GUIView): Boolean {
            val ret = _views.add(element)
            element.moveListeners.add(this@Level)
            updateViewBeingInteractedWith()
            return ret
        }

        override fun remove(element: GUIView): Boolean {
            val ret = _views.remove(element)
            element.moveListeners.remove(this@Level)
            updateViewBeingInteractedWith()
            return ret
        }
    }

    var ghostBlock: GhostBlock? = null
    var blockPlaceable = false

    var mouseOnLevel = false
    var mouseLevelXPixel = 0
    var mouseLevelYPixel = 0

    init {
        InputManager.mouseMovementListeners.add(this)
        System.out.println("Creating level, seed: " + seed)
        val gen = arrayOfNulls<Chunk>(widthChunks * heightChunks)
        for (y in 0 until heightChunks) {
            for (x in 0 until widthChunks) {
                gen[x + y * widthChunks] = Chunk(this, x, y)
            }
        }
        chunks = gen.requireNoNulls()
    }

    fun render(view: GUIView) {
        // Assume it is already added to views list
        val r = view.getViewRectangle()
        val xPixel0 = r.minX.toInt()
        val yPixel0 = r.minY.toInt()
        val xPixel1 = r.maxX.toInt()
        val yPixel1 = r.maxY.toInt()
        val xTile0 = xPixel0 shr 4
        val yTile0 = yPixel0 shr 4
        val xTile1 = (xPixel1 shr 4) + 1
        val yTile1 = (yPixel1 shr 4) + 1
        val maxX = Math.min(xTile1, widthTiles)
        val maxY = Math.min(yTile1, heightTiles)
        val minX = Math.max(xTile0, 0)
        val minY = Math.max(yTile0, 0)
        val chunks = getChunksFromTileRectangle(minX, minY, maxX - minX, maxY - minY)
        var count = 0
        for (y in (maxY - 1) downTo minY) {
            for (x in minX until maxX) {
                val c = getChunkFromTile(x, y)
                c.getTile(x, y).render()
                count++
            }
        }
        for (y in (maxY - 1) downTo minY) {
            val yChunk = y shr CHUNK_TILE_EXP
            // Render the line of blocks
            for (x in minX until maxX) {
                val c = getChunk(x shr CHUNK_TILE_EXP, yChunk)
                c.getBlock(x, y)?.render()
                if (ghostBlock != null) {
                    val g = ghostBlock!!
                    if (g.xTile == x && g.yTile == y) {
                        g.render()
                    }
                }
            }
            // Render the moving objects in sorted order
            for (xChunk in (minX shr CHUNK_TILE_EXP)..(maxX shr CHUNK_TILE_EXP)) {
                val c = getChunk(xChunk, yChunk)
                if (c.moving!!.size > 0)
                    c.moving!!.filter { it.yTile >= y && it.yTile < y + 1 }.forEach { it.render() }
            }
        }
        /*
        for (y in (maxY - 1) downTo minY) {
            for (x in minX until maxX) {
                val c = getChunkFromTile(x, y)
                c.getTile(x, y).render()
                c.getBlock(x, y)?.render()
                count++
            }
        }
         */
        if (Game.CHUNK_BOUNDARIES) {
            for (c in getChunksFromTileRectangle(minX, minY, maxX - minX, maxY - minY)) {
                Renderer.renderEmptyRectangle(c.xTile shl 4, c.yTile shl 4, CHUNK_SIZE_PIXELS, CHUNK_SIZE_PIXELS)
                Renderer.renderText("x: ${c.xChunk}, y: ${c.yChunk}", (c.xTile shl 4) + 1, (c.yTile shl 4) + 5)
                Renderer.renderText("updates required: ${c.updatesRequired!!.size}", (c.xTile shl 4) + 1, (c.yTile shl 4) + 9)
                Renderer.renderText("moving objects: ${c.moving!!.size} (${c.movingOnBoundary!!.size} on boundary)", (c.xTile shl 4) + 1, (c.yTile shl 4) + 13)
            }
        }
        DebugOverlay.setInfo("${view.name} tile render count", count.toString())
    }

    fun update() {
        var count = 0
        for (c in loadedChunks) {
            /* update() already unloads if necessary */
            c.beingRendered = isBeingRendered(c.xChunk, c.yChunk)
            c.update()
            if (c.loaded)
                count++
        }
        updateGhostBlock()
        DebugOverlay.setInfo("Loaded chunks", count.toString())
    }

    private fun updateGhostBlock() {
        val currentItem = HUD.Hotbar.currentItem
        if (currentItem == null && ghostBlock != null) {
            ghostBlock = null
        } else if (currentItem != null) {
            val xTile = mouseLevelXPixel shr 4
            val yTile = mouseLevelYPixel shr 4
            if (ghostBlock == null) {
                ghostBlock = GhostBlock(xTile, yTile, currentItem.type.placedBlock)
            } else if (xTile != ghostBlock!!.xTile || yTile != ghostBlock!!.yTile || ghostBlock!!.type != currentItem.type.placedBlock) {
                ghostBlock = GhostBlock(xTile, yTile, currentItem.type.placedBlock)
            } else {
                ghostBlock!!.placeable = ghostBlock!!.getCollision(xTile shl 4, yTile shl 4) == null
            }
        }
    }

    private fun isBeingRendered(xChunk: Int, yChunk: Int) = views.any { it.getViewRectangle().intersects(Rectangle(xChunk shl (CHUNK_TILE_EXP + 4), yChunk shl (CHUNK_TILE_EXP + 4), 128, 128)) }

    /* Listeners and senders */
    fun onMouseAction(type: PressType, xPixel: Int, yPixel: Int) {
        if (ghostBlock != null && ghostBlock!!.placeable) {
            add(Block(ghostBlock!!.xTile, ghostBlock!!.yTile, ghostBlock!!.type))
            updateGhostBlock()
        }
    }

    override fun onCameraMove(view: GUIView, pXPixel: Int, pYPixel: Int) {
        if (view == viewBeingInteractedWith) {
            mouseMoveRelativeToLevel()
        }
    }

    // Will I know what I did at 3:16 AM later? Hopefully. Right now this seems reasonable
    private fun mouseMoveRelativeToLevel() {
        val viewRectangle = viewBeingInteractedWith!!.getViewRectangle()
        val zoom = viewBeingInteractedWith!!.zoomMultiplier
        mouseLevelXPixel = (Mouse.xPixel / zoom).toInt() + viewRectangle.x
        mouseLevelYPixel = (Mouse.yPixel / zoom).toInt() + viewRectangle.y
    }

    override fun onMouseMove(pXPixel: Int, pYPixel: Int) {
        updateViewBeingInteractedWith()
        if (mouseOnLevel) {
            mouseMoveRelativeToLevel()
        }
    }

    private fun updateViewBeingInteractedWith() {
        viewBeingInteractedWith = views.firstOrNull { it.mouseOn }
        mouseOnLevel = viewBeingInteractedWith != null
        if (viewBeingInteractedWith != null) {
            val zoom = viewBeingInteractedWith!!.zoomMultiplier
            val viewRectangle = viewBeingInteractedWith!!.getViewRectangle()
            mouseLevelXPixel = (Mouse.xPixel / zoom).toInt() + viewRectangle.x
            mouseLevelYPixel = (Mouse.yPixel / zoom).toInt() + viewRectangle.y
        }
    }

    /* Generation */
    protected abstract fun genTiles(xChunk: Int, yChunk: Int): Array<Tile>

    protected abstract fun genBlocks(xChunk: Int, yChunk: Int): Array<Block?>

    /* Util */
    /** Whether or not this collides with a block */
    fun getBlockCollision(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel): LevelObject? {
        // Blocks won't have a hitbox bigger than their width/height tiles
        val blocks = getIntersectingBlocksFromPixelRectangle(l.hitbox.xStart + xPixel, l.hitbox.yStart + yPixel, l.hitbox.width, l.hitbox.height)
        for (b in blocks) {
            if (doesPairCollide(l, xPixel, yPixel, b)) {
                return b
            }
        }
        return null
    }

    /** Whether or not this collides with a moving object */
    fun getMovingObjectCollision(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel, predicate: ((MovingObject) -> Boolean)? = null): LevelObject? {
        val c = getChunk(l.xChunk, l.yChunk)
        for (m in c.moving!!) {
            if (m != l) {
                if (predicate != null) {
                    if (predicate(m) && doesPairCollide(l, xPixel, yPixel, m)) {
                        return m
                    }
                } else if (doesPairCollide(l, xPixel, yPixel, m)) {
                    return m
                }
            }
        }
        for (m in c.movingOnBoundary!!) {
            if (m != l) {
                if (predicate != null) {
                    if (predicate(m) &&
                            doesPairCollide(l, xPixel, yPixel, m)) {
                        return m
                    }
                } else if (doesPairCollide(l, xPixel, yPixel, m)) {
                    return m
                }
            }
        }
        return null
    }

    fun getCollision(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel): LevelObject? {
        val m = getMovingObjectCollision(l, xPixel, yPixel)
        if (m != null)
            return m
        val b = getBlockCollision(l, xPixel, yPixel)
        if (b != null)
            return b
        return null
    }

    fun doesPairCollide(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel, l2: LevelObject, xPixel2: Int = l2.xPixel, yPixel2: Int = l2.yPixel): Boolean {
        return GeometryHelper.intersects(xPixel + l.hitbox.xStart, yPixel + l.hitbox.yStart, l.hitbox.width, l.hitbox.height, xPixel2 + l2.hitbox.xStart, yPixel2 + l2.hitbox.yStart, l2.hitbox.width, l2.hitbox.height)
    }

    fun updateChunk(o: MovingObject) {
        val intersectingChunks = getChunksFromPixelRectangle(o.hitbox.xStart + o.xPixel, o.hitbox.yStart + o.yPixel, o.hitbox.width, o.hitbox.height).toMutableList()
        val currentChunk = getChunk(o.xChunk, o.yChunk)
        intersectingChunks.remove(currentChunk)
        if (o.intersectingChunks != intersectingChunks) {
            o.intersectingChunks.forEach { it.movingOnBoundary!!.remove(o) }
            intersectingChunks.forEach { it.movingOnBoundary!!.add(o) }
            o.intersectingChunks = intersectingChunks
        }
        if (o.currentChunk != currentChunk) {
            o.currentChunk.removeMoving(o)
            o.currentChunk = currentChunk
            currentChunk.addMoving(o)
        }
    }

    /**
     * Does everything necessary to add the object to the level
     * @return if the object was added
     */
    fun add(l: LevelObject): Boolean {
        if (l is Block) {
            if (l.getCollision(l.xPixel, l.yPixel) != null) {
                return false
            }
            for (x in 0 until l.type.widthTiles) {
                for (y in 0 until l.type.heightTiles) {
                    getChunkFromTile(l.xTile + x, l.yTile + y).setBlock(l, l.xTile + x, l.yTile + y)
                }
            }
            return true
        } else if (l is MovingObject) {
            if (l.getCollision(l.xPixel, l.yPixel) != null)
                return false
            if (l.hitbox != Hitbox.NONE) {
                l.intersectingChunks.forEach { it.movingOnBoundary!!.add(l) }
                l.currentChunk.addMoving(l)
            }
        }
        return false
    }

    fun remove(l: LevelObject) {
        val c = getChunk(l.xChunk, l.yChunk)
        if (l is Block) {

            for (x in 0 until l.type.widthTiles) {
                for (y in 0 until l.type.heightTiles) {
                    c.removeBlock(l, l.xTile + x, l.yTile + y)
                }
            }
        } else if (l is MovingObject) {
            l.intersectingChunks.forEach { it.movingOnBoundary!!.remove(l) }
            l.currentChunk.removeMoving(l)
        }
    }

    /* Getting and setting */
    /**
     * Loads and returns the specified chunk
     */
    fun loadChunk(xChunk: Int, yChunk: Int): Chunk {
        val c = chunks[xChunk + yChunk * widthChunks]
        c.load(genBlocks(xChunk, yChunk), genTiles(xChunk, yChunk))
        return c
    }

    /**
     * If load is true, it will load the chunk if necessary
     */
    fun getChunk(xChunk: Int, yChunk: Int, load: Boolean = true): Chunk {
        val c = chunks[xChunk + yChunk * widthChunks]
        if (load && !c.loaded) {
            loadChunk(xChunk, yChunk)
        }
        return c
    }

    /**
     * If load is true, it will load the chunk if necessary
     */
    fun getChunkFromTile(xTile: Int, yTile: Int, load: Boolean = true): Chunk {
        return getChunk(xTile shr 3, yTile shr 3, load)
    }

    fun getChunksFromRectangle(xChunk: Int, yChunk: Int, xChunk2: Int, yChunk2: Int): List<Chunk> {
        val l = mutableListOf<Chunk>()
        for (x in xChunk..xChunk2) {
            for (y in yChunk..yChunk2) {
                l.add(getChunk(x, y))
            }
        }
        return l
    }

    fun getChunksFromTileRectangle(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int): List<Chunk> {
        return getChunksFromRectangle(xTile shr CHUNK_TILE_EXP, yTile shr CHUNK_TILE_EXP, (xTile + widthTiles) shr CHUNK_TILE_EXP, (yTile + heightTiles) shr CHUNK_TILE_EXP)
    }

    fun getChunksFromPixelRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): List<Chunk> {
        return getChunksFromRectangle(xPixel shr CHUNK_PIXEL_EXP, yPixel shr CHUNK_PIXEL_EXP, (xPixel + widthPixels) shr CHUNK_PIXEL_EXP, (yPixel + heightPixels) shr CHUNK_PIXEL_EXP)
    }

    /**
     * Will load the chunk if necessary
     */
    fun getBlock(xTile: Int, yTile: Int): Block? {
        return getChunkFromTile(xTile, yTile).getBlock(xTile, yTile)
    }

    fun getIntersectingBlocksFromRectangle(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int): Set<Block> {
        val m = mutableSetOf<Block>()
        for (x in xTile..(xTile + widthTiles)) {
            for (y in yTile..(yTile + heightTiles)) {
                val b = getBlock(x, y)
                if (b != null)
                    m.add(b)
            }
        }
        return m
    }

    fun getIntersectingBlocksFromPixelRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): Set<Block> {
        return getIntersectingBlocksFromRectangle(xPixel shr 4, yPixel shr 4, widthPixels shr 4, heightPixels shr 4)
    }

    /**
     * Will load the chunk if necessary
     */
    fun getTile(xTile: Int, yTile: Int): Tile {
        return getChunkFromTile(xTile, yTile).getTile(xTile, yTile)
    }
}