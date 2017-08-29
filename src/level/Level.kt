package level

import level.block.Block
import level.moving.MovingObject
import level.tile.Tile
import screen.DebugOverlay
import screen.GUIView
import java.util.*

const val CHUNK_SIZE = 8
val CHUNK_EXP = (Math.log(CHUNK_SIZE.toDouble()) / Math.log(2.0)).toInt()

abstract class Level(seed: Long, val widthTiles: Int, val heightTiles: Int) {

    val heightPixels = heightTiles shl 4
    val widthPixels = widthTiles shl 4
    val heightChunks = heightTiles shr CHUNK_EXP
    val widthChunks = widthTiles shr CHUNK_EXP
    val rand: Random = Random(seed)
    val chunks: Array<Chunk>
    val loadedChunks = mutableListOf<Chunk>()

    init {
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
        loadedChunks.forEach { it.beingRendered = false }
        for (y in minY until maxY) {
            for (x in minX until maxX) {
                val c = getChunkFromTile(x, y)
                c.beingRendered = true
                c.getTile(x, y).render()
                count++
            }
        }
        DebugOverlay.setInfo("Level tile render count", count.toString())
    }

    fun update() {
        var count = 0
        val loadedCopy = loadedChunks.toTypedArray()
        for (c in loadedCopy) {
            /* update() already unloads if necessary */
            c.update()
            if (c.loaded)
                count++
        }
        DebugOverlay.setInfo("Loaded chunks", count.toString())
    }

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

    fun remove(l: LevelObject, xChunk: Int = l.xChunk, yChunk: Int = l.yChunk) {
        val c = getChunk(xChunk, yChunk)
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
        return getChunksFromRectangle(xTile shr 3, yTile shr 3, Math.ceil(widthTiles / 8.0).toInt(), Math.ceil(heightTiles / 8.0).toInt())
    }

    fun getChunksFromPixelRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): List<Chunk> {
        return getChunksFromRectangle(xPixel shr 7, yPixel shr 7, Math.ceil(widthPixels / 64.0).toInt(), Math.ceil(heightPixels / 64.0).toInt())
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