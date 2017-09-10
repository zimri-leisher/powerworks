package level

import graphics.Images
import graphics.RenderParams
import graphics.Renderer
import io.InputManager
import io.MouseMovementListener
import level.block.Block
import level.moving.MovingObject
import level.tile.Tile
import screen.CameraMovementListener
import screen.DebugOverlay
import screen.GUIView
import screen.HUD
import java.awt.Rectangle
import java.util.*

const val CHUNK_SIZE = 8
val CHUNK_EXP = (Math.log(CHUNK_SIZE.toDouble()) / Math.log(2.0)).toInt()

abstract class Level(seed: Long, val widthTiles: Int, val heightTiles: Int) : CameraMovementListener, MouseMovementListener {

    val heightPixels = heightTiles shl 4
    val widthPixels = widthTiles shl 4
    val heightChunks = heightTiles shr CHUNK_EXP
    val widthChunks = widthTiles shr CHUNK_EXP

    val rand: Random = Random(seed)

    val chunks: Array<Chunk>
    val loadedChunks = mutableListOf<Chunk>()

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
    private var viewBeingInteractedWith: GUIView? = null

    var ghostBlock: Block? = null
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
        var count = 0
        for (y in (maxY - 1) downTo minY) {
            for (x in minX until maxX) {
                val c = getChunkFromTile(x, y)
                c.getTile(x, y).render()
                c.getBlock(x, y)?.render()
                count++
            }
        }
        if (ghostBlock != null) {
            Renderer.renderTexture(ghostBlock!!.type.getTexture(ghostBlock!!.rotation), ghostBlock!!.xPixel, ghostBlock!!.yPixel, RenderParams(alpha = 0.4f))
            Renderer.renderTexture(if (blockPlaceable) Images.BLOCK_PLACEABLE else Images.BLOCK_NOT_PLACEABLE, ghostBlock!!.xPixel, ghostBlock!!.yPixel)
        }
        DebugOverlay.setInfo("${view.name} tile render count", count.toString())
    }

    fun update() {
        var count = 0
        val loadedCopy = loadedChunks.toTypedArray()
        for (c in loadedCopy) {
            /* update() already unloads if necessary */
            c.beingRendered = isBeingRendered(c.xChunk, c.yChunk)
            c.update()
            if (c.loaded)
                count++
        }
        updateGhostBlock()
        DebugOverlay.setInfo("Loaded chunks", count.toString())
    }

    override fun onCameraMove(view: GUIView, pXPixel: Int, pYPixel: Int) {
        println("test")
        if(view == viewBeingInteractedWith) {
            mouseMoveRelativeToLevel()
        }
    }

    // Will I know what I did at 3:16 AM later? Hopefully. Right now this seems reasonable
    fun mouseMoveRelativeToLevel() {
        println("mmoved")
        val viewRectangle = viewBeingInteractedWith!!.getViewRectangle()
        mouseLevelXPixel = viewRectangle.x + InputManager.mouseXPixel
        mouseLevelYPixel = viewRectangle.y + InputManager.mouseYPixel
    }

    override fun onMouseMove(pXPixel: Int, pYPixel: Int) {
        updateViewBeingInteractedWith()
        if(mouseOnLevel) {
            mouseMoveRelativeToLevel()
        }
    }

    fun updateGhostBlock() {
        val currentItem = HUD.Hotbar.currentItem
        val mXTile = InputManager.mouseXPixel shr 4
        val mYTile = InputManager.mouseYPixel shr 4
        if(ghostBlock == null) {
            if(currentItem != null && currentItem.isPlaceable) {
                ghostBlock = Block(mXTile, mYTile, currentItem.type.placedBlock)
            }
        } else if(currentItem == null) {
            ghostBlock = null
        } else if(currentItem.type.placedBlock != ghostBlock!!.type) {
            ghostBlock = Block(mXTile, mYTile, currentItem.type.placedBlock)
        } else if(ghostBlock!!.xTile != mXTile || ghostBlock!!.yTile != mYTile) {
            ghostBlock = Block(mXTile, mYTile, currentItem.type.placedBlock)
        }
    }

    fun updateViewBeingInteractedWith() {
        viewBeingInteractedWith = views.firstOrNull { it.mouseOn }
        mouseOnLevel = viewBeingInteractedWith != null
    }

    private fun isBeingRendered(xChunk: Int, yChunk: Int) = views.any { it.getViewRectangle().intersects(Rectangle(xChunk shl (CHUNK_EXP + 4), yChunk shl (CHUNK_EXP + 4), 128, 128)) }

    /* Generation */
    protected abstract fun genTiles(xChunk: Int, yChunk: Int): Array<Tile>

    protected abstract fun genBlocks(xChunk: Int, yChunk: Int): Array<Block?>

    /* Util */
    fun updateChunk(o: MovingObject) {
        val c = getChunk(o.xChunk, o.yChunk)
        val oldC = o.currentChunk
        if (c != oldC) {
            oldC.removeMoving(o)
            c.addMoving(o)
            o.currentChunk = c
        }
    }

    /**
     * Does everything necessary to add the object to the level
     * @return if the object was added
     */
    fun add(l: LevelObject): Boolean {
        val c = getChunk(l.xChunk, l.yChunk)
        if (l is Tile) {
            c.setTile(l)
            return true
        } else if (l is Block) {
            if (l.getCollision(0, 0))
                return false
            if (l.hitbox != Hitbox.NONE)
                c.collidables!!.add(l)
            c.setBlock(l)
            return true
        } else if (l is MovingObject) {
            if (l.getCollision(0, 0))
                return false
            if (l.hitbox != Hitbox.NONE)
                c.collidables!!.add(l)
            c.addMoving(l)
        }
        return false
    }

    fun remove(l: LevelObject) {
        val c = getChunk(l.xChunk, l.yChunk)
        if (l is Tile) {
            throw Exception("Tiles cannot be null. Use add to set a new one instead")
        } else if (l is Block) {
            if (l.hitbox != Hitbox.NONE) {
                c.collidables!!.remove(l)
            }
            c.removeBlock(l)
        } else if (l is MovingObject) {
            if (l.hitbox != Hitbox.NONE) {
                c.collidables!!.remove(l)
            }
            c.removeMoving(l)
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

    fun getChunksFromRectangle(xChunk: Int, yChunk: Int, widthChunks: Int, heightChunks: Int): List<Chunk> {
        val l = mutableListOf<Chunk>()
        for (x in xChunk until xChunk + widthChunks) {
            for (y in yChunk until yChunk + heightChunks) {
                l.add(getChunk(x, y))
            }
        }
        return l
    }

    fun getChunksFromTileRectangle(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int): List<Chunk> {
        return getChunksFromRectangle(xTile shr 3, yTile shr 3, Math.ceil(widthTiles / CHUNK_SIZE.toDouble()).toInt(), Math.ceil(heightTiles / CHUNK_SIZE.toDouble()).toInt())
    }

    fun getChunksFromPixelRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): List<Chunk> {
        return getChunksFromRectangle(xPixel shr CHUNK_EXP, yPixel shr CHUNK_EXP, Math.ceil(widthPixels / Math.pow(CHUNK_SIZE.toDouble(), 2.0)).toInt(), Math.ceil(heightPixels / Math.pow(CHUNK_SIZE.toDouble(), 2.0)).toInt())
    }

    /**
     * Will load the chunk if necessary
     */
    fun getBlock(xTile: Int, yTile: Int): Block? {
        return getChunkFromTile(xTile, yTile).getBlock(xTile, yTile)
    }

    /**
     * Will load the chunk if necessary
     */
    fun getTile(xTile: Int, yTile: Int): Tile {
        return getChunkFromTile(xTile, yTile).getTile(xTile, yTile)
    }
}