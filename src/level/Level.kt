package level

import audio.AudioManager
import graphics.Renderer
import inv.ItemType
import io.InputManager
import io.Mouse
import io.MouseMovementListener
import io.PressType
import level.block.Block
import level.block.GhostBlock
import level.moving.MovingObject
import level.node.InputNode
import level.node.OutputNode
import level.node.TransferNode
import level.resource.ResourceType
import level.tile.Tile
import main.Game
import misc.GeometryHelper
import screen.CameraMovementListener
import screen.DebugOverlay
import screen.GUIView
import screen.HUD
import java.awt.Rectangle
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

const val CHUNK_SIZE_TILES = 8
val CHUNK_TILE_EXP = (Math.log(CHUNK_SIZE_TILES.toDouble()) / Math.log(2.0)).toInt()
val CHUNK_PIXEL_EXP = CHUNK_TILE_EXP + 4
val CHUNK_SIZE_PIXELS = CHUNK_SIZE_TILES shl 4

abstract class Level(val levelName: String, val widthTiles: Int, val heightTiles: Int) : CameraMovementListener, MouseMovementListener {

    val levelFile: File

    val heightPixels = heightTiles shl 4
    val widthPixels = widthTiles shl 4
    val heightChunks = heightTiles shr CHUNK_TILE_EXP
    val widthChunks = widthTiles shr CHUNK_TILE_EXP

    val seed: Long
    val rand: Random

    val chunks: Array<Chunk>
    val loadedChunks = CopyOnWriteArrayList<Chunk>()

    private var viewBeingInteractedWith: GUIView? = null
    private var lastViewInteractedWith: GUIView? = null
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

    var mouseOnLevel = false
    var mouseLevelXPixel = 0
    var mouseLevelYPixel = 0

    init {
        val p = Paths.get(Game.JAR_PATH, "data/save/$levelName/")
        if (Files.notExists(p)) {
            Files.createDirectory(p)
            seed = (Math.random() * 4096).toLong()
            rand = Random(seed)
            InputManager.mouseMovementListeners.add(this)
            println("Creating level")
            levelFile = Files.createFile(Paths.get(p.toAbsolutePath().toString(), "$levelName.level")).toFile()
            val out = DataOutputStream(BufferedOutputStream(FileOutputStream(levelFile, true)))
            out.writeLong(seed)
            out.close()
        } else {
            InputManager.mouseMovementListeners.add(this)
            println("Loading level")
            levelFile = Paths.get(p.toAbsolutePath().toString(), "$levelName.level").toFile()
            val g = DataInputStream(BufferedInputStream(FileInputStream(levelFile)))
            seed = g.readLong()
            rand = Random(seed)
            g.close()
        }
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
        val r = view.viewRectangle
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
            val placedType = currentItem.type.placedBlock
            val xTile = ((mouseLevelXPixel + placedType.textureXPixelOffset) shr 4) - placedType.widthTiles / 2
            val yTile = ((mouseLevelYPixel + placedType.textureYPixelOffset) shr 4) - placedType.heightTiles / 2
            if (ghostBlock == null) {
                ghostBlock = GhostBlock(xTile, yTile, placedType)
            } else {
                val g = ghostBlock!!
                if (xTile != g.xTile || yTile != g.yTile || g.type != placedType) {
                    ghostBlock = GhostBlock(xTile, yTile, placedType)
                } else {
                    g.placeable = ghostBlock!!.getCollision(xTile shl 4, yTile shl 4) == null
                }
            }
        }
    }

    private fun isBeingRendered(xChunk: Int, yChunk: Int) = views.any { it.viewRectangle.intersects(Rectangle(xChunk shl (CHUNK_TILE_EXP + 4), yChunk shl (CHUNK_TILE_EXP + 4), 128, 128)) }

    /* Listeners and senders */
    fun onMouseAction(type: PressType, xPixel: Int, yPixel: Int) {
        if (ghostBlock != null && ghostBlock!!.placeable) {
            add(ghostBlock!!.type(ghostBlock!!.xTile, ghostBlock!!.yTile))
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
        updateMouseLevelLocation()
    }

    override fun onMouseMove(pXPixel: Int, pYPixel: Int) {
        updateViewBeingInteractedWith()
        if (mouseOnLevel) {
            mouseMoveRelativeToLevel()
        }
    }

    private fun updateViewBeingInteractedWith() {
        views.sortByDescending { it.parentWindow.layer }
        viewBeingInteractedWith = views.firstOrNull { it.mouseOn }
        if(viewBeingInteractedWith != null)
            lastViewInteractedWith = viewBeingInteractedWith
        mouseOnLevel = viewBeingInteractedWith != null
        updateMouseLevelLocation()
        updateAudioEars()
    }

    private fun updateAudioEars() {
        if(lastViewInteractedWith != null) {
            AudioManager.ears = lastViewInteractedWith!!.camera
        }
    }

    private fun updateMouseLevelLocation() {
        if (viewBeingInteractedWith != null) {
            val zoom = viewBeingInteractedWith!!.zoomMultiplier
            val viewRectangle = viewBeingInteractedWith!!.viewRectangle
            mouseLevelXPixel = ((Mouse.xPixel - viewBeingInteractedWith!!.xPixel) / zoom).toInt() + viewRectangle.x
            mouseLevelYPixel = ((Mouse.yPixel - viewBeingInteractedWith!!.yPixel) / zoom).toInt() + viewRectangle.y
        }
    }

    /* Generation */
    protected abstract fun genTiles(xChunk: Int, yChunk: Int): Array<Tile>

    protected abstract fun genBlocks(xChunk: Int, yChunk: Int): Array<Block?>

    /* Util */
    /** Whether or not this collides with a block */
    fun getBlockCollision(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel, predicate: ((LevelObject) -> Boolean)? = null): LevelObject? {
        // Blocks won't have a hitbox bigger than their width/height tiles
        val blocks = getIntersectingBlocksFromPixelRectangle(l.hitbox.xStart + xPixel, l.hitbox.yStart + yPixel, l.hitbox.width, l.hitbox.height)
        for (b in blocks) {
            if ((predicate != null && predicate(b) && doesPairCollide(l, xPixel, yPixel, b)) || doesPairCollide(l, xPixel, yPixel, b))
                return b
        }
        return null
    }

    /** Whether or not this collides with a moving object */
    fun getMovingObjectCollision(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel, predicate: ((MovingObject) -> Boolean)? = null): LevelObject? {
        val c = getChunk(l.xChunk, l.yChunk)
        for (m in c.moving!!) {
            if (m != l) {
                if ((predicate != null && predicate(m) && doesPairCollide(l, xPixel, yPixel, m)) || doesPairCollide(l, xPixel, yPixel, m)) {
                    return m
                }
            }
        }
        // In the future, use async?
        for (m in c.movingOnBoundary!!) {
            if (m != l) {
                if ((predicate != null && predicate(m) && doesPairCollide(l, xPixel, yPixel, m)) || doesPairCollide(l, xPixel, yPixel, m)) {
                    return m
                }
            }
        }
        return null
    }

    fun getCollision(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel, predicate: ((LevelObject) -> Boolean)? = null): LevelObject? {
        val m = getMovingObjectCollision(l, xPixel, yPixel, predicate)
        if (m != null)
            return m
        val b = getBlockCollision(l, xPixel, yPixel, predicate)
        if (b != null)
            return b
        return null
    }

    fun doesPairCollide(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel, l2: LevelObject, xPixel2: Int = l2.xPixel, yPixel2: Int = l2.yPixel): Boolean {
        return GeometryHelper.intersects(xPixel + l.hitbox.xStart, yPixel + l.hitbox.yStart, l.hitbox.width, l.hitbox.height, xPixel2 + l2.hitbox.xStart, yPixel2 + l2.hitbox.yStart, l2.hitbox.width, l2.hitbox.height)
    }

    suspend fun doesPairCollideAsync(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel, l2: LevelObject, xPixel2: Int = l2.xPixel, yPixel2: Int = l2.yPixel): Boolean {
        return GeometryHelper.intersects(xPixel + l.hitbox.xStart, yPixel + l.hitbox.yStart, l.hitbox.width, l.hitbox.height, xPixel2 + l2.hitbox.xStart, yPixel2 + l2.hitbox.yStart, l2.hitbox.width, l2.hitbox.height)
    }

    fun updateChunk(o: MovingObject) {
        val currentChunk = getChunk(o.xChunk, o.yChunk)
        if (o.hitbox != Hitbox.NONE) {
            val intersectingChunks = getChunksFromPixelRectangle(o.hitbox.xStart + o.xPixel, o.hitbox.yStart + o.yPixel, o.hitbox.width, o.hitbox.height).toMutableList()
            intersectingChunks.remove(currentChunk)
            if (o.intersectingChunks != intersectingChunks) {
                o.intersectingChunks.forEach { it.movingOnBoundary!!.remove(o) }
                intersectingChunks.forEach { it.movingOnBoundary!!.add(o) }
                o.intersectingChunks = intersectingChunks
            }
        }
        if (o.currentChunk != currentChunk) {
            o.currentChunk.removeMoving(o)
            o.currentChunk = currentChunk
            currentChunk.addMoving(o)
        }
    }

    fun updateNode(o: TransferNode<*>) {
        if (o is OutputNode) {
            attachInputToOutput(o)
        }
    }

    private fun <R : ResourceType> attachInputToOutput(o: OutputNode<R>) {
        val i = getInputNode<R>(o.xTile, o.yTile, o.dir, o.resourceTypeID)
        o.attachedInput = i
    }

    fun <R : ResourceType> getInputNode(xTile: Int, yTile: Int, dir: Int, resourceTypeID: Int): InputNode<R>? {
        // THIS IS OK - we know they are of the same type because we can assume that when you
        // initialize it, you use the corresponding ResourceType ID
        return getChunkFromTile(xTile, yTile).inputNodes!![resourceTypeID].firstOrNull { it.xTile == xTile && it.yTile == yTile && GeometryHelper.isOppositeAngle(it.dir, dir) } as InputNode<R>?
    }

    fun <R : ResourceType> getOutputNode(xTile: Int, yTile: Int, dir: Int, resourceTypeID: Int): OutputNode<R>? {
        // THIS IS OK - we know they are of the same type because we can assume that when you
        // initialize it, you use the corresponding ResourceType ID
        return getChunkFromTile(xTile, yTile).outputNodes!![resourceTypeID].firstOrNull { it.xTile == xTile && it.yTile == yTile && GeometryHelper.isOppositeAngle(it.dir, dir) } as OutputNode<R>?
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
            if (l is DroppedItem) {
                // Dropped items will automatically try and fit themselves nearest to where they are trying to be placed
                val positions = mutableListOf<Pair<Int, Int>>()
                // Find the four positions around the position it is currently in
                positions.add(Pair(l.xPixel, l.yPixel))
                positions.add(Pair(l.xPixel - 1 * Hitbox.DROPPED_ITEM.width, l.yPixel))
                positions.add(Pair(l.xPixel, l.yPixel - 1 * Hitbox.DROPPED_ITEM.height))
                positions.add(Pair(l.xPixel + 1 * Hitbox.DROPPED_ITEM.width, l.yPixel))
                positions.add(Pair(l.xPixel, l.yPixel + 1 * Hitbox.DROPPED_ITEM.height))
                for ((x, y) in positions) {
                    l.xPixel = x
                    l.yPixel = y
                    if (l.getCollision(l.xPixel, l.yPixel) == null) {
                        if (l.hitbox != Hitbox.NONE) {
                            l.intersectingChunks.forEach { it.movingOnBoundary!!.add(l) }
                        }
                        l.currentChunk.addMoving(l)
                        l.currentChunk.addDroppedItem(l)
                        return true
                    }
                }
                return false
            } else {
                if (l.getCollision(l.xPixel, l.yPixel) != null)
                    return false
                if (l.hitbox != Hitbox.NONE) {
                    l.intersectingChunks.forEach { it.movingOnBoundary!!.add(l) }
                }
                l.currentChunk.addMoving(l)
            }
        }
        return false
    }

    fun addTransferNode(o: TransferNode<*>) {
        val c = getChunkFromTile(o.xTile, o.yTile)
        if (o is InputNode) {
            c.addInputNode(o)
        } else if (o is OutputNode) {
            c.addOutputNode(o)
        }
        updateNode(o)
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

    /**
     * Tries to 'materialize' this resource where specified. Note: most resources have no physical representation
     * other than some purely decoratee particles
     * @return the quantity of the resource that was able to be materialized
     */
    fun add(xPixel: Int, yPixel: Int, r: ResourceType, quantity: Int): Int {
        if (r is ItemType) {
            add(DroppedItem(xPixel, yPixel, r))
            return 1
        }
        return 0
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

    fun save() {

    }
}