package level

import audio.AudioManager
import graphics.Renderer
import inv.ItemType
import io.ControlPressHandler
import io.InputManager
import io.MouseMovementListener
import io.PressType
import level.block.Block
import level.block.BlockType
import level.block.GhostBlock
import level.moving.MovingObject
import level.node.InputNode
import level.node.OutputNode
import level.node.TransferNode
import level.resource.ResourceType
import level.tile.OreTileType
import level.tile.Tile
import level.tube.TubeBlockGroup
import main.Game
import misc.GeometryHelper
import misc.Numbers
import screen.CameraMovementListener
import screen.DebugOverlay
import screen.GUIView
import screen.Mouse
import screen.Mouse.DROPPED_ITEM_PICK_UP_RANGE
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

    val oreNoises = mutableMapOf<OreTileType, Noise>()

    private var viewBeingInteractedWith: GUIView? = null
    private var lastViewInteractedWith: GUIView? = null
    private val _views = mutableListOf<GUIView>()
    val openViews = object : MutableList<GUIView> by _views {
        override fun add(element: GUIView): Boolean {
            val ret = _views.add(element)
            element.moveListeners.add(this@Level)
            println("adding view")
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

    var selectedLevelObject: LevelObject? = null

    var maxRenderSteps = 0
    var lastRenderSteps = 0

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
        for (t in OreTileType.ALL) {
            oreNoises.put(t, OpenSimplexNoise(Numbers.genRandom(seed, rand.nextInt(99).toLong())))
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
        //lastRenderSteps = 1
        //if (lastRenderSteps > maxRenderSteps)
        //    return
        for (y in (maxY - 1) downTo minY) {
            for (x in minX until maxX) {
                val c = getChunkFromTile(x, y)
                c.getTile(x, y).render()
                count++
            }
        }
        for (y in minY until maxY) {
            val yChunk = y shr CHUNK_TILE_EXP
            // Render the line of blocks
            for (x in minX until maxX) {
                val c = getChunk(x shr CHUNK_TILE_EXP, yChunk)
                val b = c.getBlock(x, y)
                if (b != null) {
                    //lastRenderSteps++
                    //if (lastRenderSteps > maxRenderSteps)
                    //    return
                    b.render()
                }
                if (ghostBlock != null) {
                    val g = ghostBlock!!
                    if (g.xTile == x && g.yTile == y) {
                        //lastRenderSteps++
                        //if (lastRenderSteps > maxRenderSteps)
                        //    return
                        g.render()
                    }
                }
            }
            // Render the moving objects in sorted order
            for (xChunk in (minX shr CHUNK_TILE_EXP) until (maxX shr CHUNK_TILE_EXP)) {
                val c = getChunk(xChunk, yChunk)
                if (c.moving!!.size > 0)
                    c.moving!!.filter { it.yTile >= y && it.yTile < y + 1 }.forEach {
                        //lastRenderSteps++
                        //if (lastRenderSteps > maxRenderSteps)
                        //    return
                        it.render()
                    }
            }
        }
        if (selectedLevelObject != null && ghostBlock == null) {
            val s = selectedLevelObject!!
            if (r.contains(s.xPixel, s.yPixel)) {
                if (s is Block)
                    Renderer.renderEmptyRectangle(s.xPixel, s.yPixel, s.type.widthTiles shl 4, s.type.heightTiles shl 4, 0x1A6AF4, .45f)
                else if (s is MovingObject)
                    Renderer.renderEmptyRectangle(s.xPixel, s.yPixel, s.hitbox.width, s.hitbox.height, 0x1A6AF4, .45f)
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
        val chunksInTileRectangle = getChunksFromTileRectangle(minX, minY, maxX - minX - 1, maxY - minY - 1)
        if (Game.CHUNK_BOUNDARIES) {
            for (c in chunksInTileRectangle) {
                Renderer.renderEmptyRectangle(c.xTile shl 4, c.yTile shl 4, CHUNK_SIZE_PIXELS, CHUNK_SIZE_PIXELS)
                Renderer.renderText("x: ${c.xChunk}, y: ${c.yChunk}", (c.xTile shl 4), (c.yTile shl 4))
                Renderer.renderText("updates required: ${c.updatesRequired!!.size}", (c.xTile shl 4), (c.yTile shl 4) + 4)
                Renderer.renderText("moving objects: ${c.moving!!.size} (${c.movingOnBoundary!!.size} on boundary)", (c.xTile shl 4), (c.yTile shl 4) + 8)
            }
        }
        if (Game.DEBUG_TUBE_INFO) {
            for (c in chunksInTileRectangle) {
                for (nList in c.inputNodes!!) {
                    for (n in nList) {
                        renderNodeDebug(n)
                    }
                }
                for (nList in c.outputNodes!!) {
                    for (n in nList) {
                        renderNodeDebug(n)
                    }
                }
            }
        }
        DebugOverlay.setInfo("${view.name} tile render count", count.toString())
    }

    private fun renderNodeDebug(n: TransferNode<*>) {
        val xSign = GeometryHelper.getXSign(n.dir)
        val ySign = GeometryHelper.getYSign(n.dir)
        if (n is OutputNode<*>) {
            Renderer.renderFilledRectangle(((n.xTile shl 4) + 7) + 8 * xSign, ((n.yTile shl 4) + 7) + 8 * ySign, 2, 2, 0xFF0000, 0.25f)
        } else if (n is InputNode<*>) {
            Renderer.renderFilledRectangle(((n.xTile shl 4) + 7) + 8 * xSign, ((n.yTile shl 4) + 7) + 8 * ySign, 2, 2, 0xFFFF00, 0.25f)
        }
    }

    fun update() {
        var count = 0
        TubeBlockGroup.update()
        updateChunksBeingRendered()
        for (c in loadedChunks) {
            if (c.updatesRequired!!.size == 0 && !c.beingRendered && c.inputNodes!!.all { it.isEmpty() } && c.outputNodes!!.all { it.isEmpty() }) {
                c.unload()
            } else {
                c.update()
                count++
            }
        }
        updateGhostBlock()
        updateSelectedLevelObject()
        DebugOverlay.setInfo("Loaded chunks", count.toString())
    }

    private fun updateChunksBeingRendered() {
        for (c in chunks) {
            c.beingRendered = false
        }
        for (v in openViews) {
            for (c in getChunksFromPixelRectangle(v.viewRectangle.x, v.viewRectangle.y, v.viewRectangle.width, v.viewRectangle.height))
                c.beingRendered = true
        }
    }

    /**
     * Makes the ghost block that appears on the mouse when holding a placeable item up-to-date
     */
    fun updateGhostBlock() {
        val currentItem = Mouse.heldItem
        if (currentItem == null && ghostBlock != null) {
            ghostBlock = null
        } else if (currentItem != null) {
            val placedType = currentItem.type.placedBlock
            if (placedType == BlockType.ERROR) {
                ghostBlock = null
            } else {
                val xTile = ((mouseLevelXPixel) shr 4) - placedType.widthTiles / 2
                val yTile = ((mouseLevelYPixel) shr 4) - placedType.heightTiles / 2
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
    }

    /* Listeners and senders */
    /**
     * Called by gui views when they are clicked on, used to place the ghost block
     */
    fun onMouseAction(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, control: Boolean, alt: Boolean) {
        if (button == 1) {
            if (ghostBlock != null && ghostBlock!!.placeable) {
                println("-----placed-----")
                if (add(ghostBlock!!.type(ghostBlock!!.xTile, ghostBlock!!.yTile))) {
                    Mouse.removeHeldItem(1)
                }
                updateGhostBlock()
            }
        } else if (button == 3) {
            if (selectedLevelObject is Block) {
                //if (remove(selectedLevelObject!!)) {
                 //   if (HUD.Hotbar.items.full) {
                 //       // TODO
                //    }
                //}
            }
        }

    }

    /**
     * Called when any view moves
     */
    override fun onCameraMove(view: GUIView, pXPixel: Int, pYPixel: Int) {
        if (view == viewBeingInteractedWith) {
            updateMouseLevelLocation()
        }
    }

    // Will I know what I did at 3:16 AM later? Hopefully. Right now this seems reasonable

    override fun onMouseMove(pXPixel: Int, pYPixel: Int) {
        updateViewBeingInteractedWith()
        if (mouseOnLevel) {
            updateMouseLevelLocation()
        }
    }

    private fun updateViewBeingInteractedWith() {
        openViews.sortByDescending { it.parentWindow.layer }
        viewBeingInteractedWith = openViews.firstOrNull { it.mouseOn }
        if (viewBeingInteractedWith != null)
            lastViewInteractedWith = viewBeingInteractedWith
        mouseOnLevel = viewBeingInteractedWith != null
        updateMouseLevelLocation()
        updateAudioEars()
    }

    /**
     * Updates the control press handlers that get controls sent to them, if any exist
     */
    private fun updateSelectedLevelObject() {
        InputManager.currentLevelHandlers.clear()
        val block = getBlock(mouseLevelXPixel shr 4, mouseLevelYPixel shr 4)
        val moving = getMoving(mouseLevelXPixel, mouseLevelYPixel)
        selectedLevelObject = moving ?: block
        if (block is ControlPressHandler)
            InputManager.currentLevelHandlers.add(block)
        if (moving is ControlPressHandler)
            InputManager.currentLevelHandlers.add(moving)
    }

    /**
     * Updates the audio engine so you hear sound from last the selected view
     */
    private fun updateAudioEars() {
        if (lastViewInteractedWith != null) {
            AudioManager.ears = lastViewInteractedWith!!.camera
        }
    }

    /**
     * Keeps the mouse level x and y pixel up-to-date by comparing with the currently selected view
     */
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
                if (m.hitbox != Hitbox.NONE && ((predicate != null && predicate(m) && doesPairCollide(l, xPixel, yPixel, m)) || doesPairCollide(l, xPixel, yPixel, m))) {
                    return m
                }
            }
        }
        // In the future, use async?
        for (m in c.movingOnBoundary!!) {
            if (m != l) {
                if (m.hitbox != Hitbox.NONE && ((predicate != null && predicate(m) && doesPairCollide(l, xPixel, yPixel, m)) || doesPairCollide(l, xPixel, yPixel, m))) {
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
        val i = getInputNode<R>(o.xTile + GeometryHelper.getXSign(o.dir), o.yTile + GeometryHelper.getYSign(o.dir), o.dir, o.resourceTypeID)
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

    fun getAllTransferNodes(xTile: Int, yTile: Int, predicate: (TransferNode<*>) -> Boolean = { true }): MutableList<TransferNode<*>> {
        val l = mutableListOf<TransferNode<*>>()
        with(getChunkFromTile(xTile, yTile)) {
            inputNodes!!.forEach { l.addAll(it.filter { predicate(it) && it.xTile == xTile && it.yTile == yTile }) }
            outputNodes!!.forEach { l.addAll(it.filter { predicate(it) && it.xTile == xTile && it.yTile == yTile }) }
        }
        return l
    }

    fun removeAllTransferNodes(xTile: Int, yTile: Int, predicate: (TransferNode<*>) -> Boolean = { true }) {
        val c = getChunkFromTile(xTile, yTile)
        getAllTransferNodes(xTile, yTile, predicate).forEach { if (it is InputNode) c.removeInputNode(it) else if (it is OutputNode) c.removeOutputNode(it) }
    }

    fun getDroppedItemsInRadius(xPixel: Int, yPixel: Int, radius: Int, predicate: (DroppedItem) -> Boolean = { true }): List<DroppedItem> {
        val l = mutableListOf<DroppedItem>()
        for (c in getChunksFromPixelRectangle(xPixel - radius, yPixel - radius, radius * 2, radius * 2)) {
            for (d in c.droppedItems!!) {
                if (predicate(d) && GeometryHelper.intersects(xPixel - radius, yPixel - radius, radius * 2, radius * 2, d.xPixel - d.hitbox.xStart, d.yPixel - d.hitbox.yStart, d.hitbox.width, d.hitbox.height)) {
                    l.add(d)
                }
            }
        }
        return l
    }

    /**
     * Does everything necessary to put the object to the level
     * @return if the object was added
     */
    fun add(l: LevelObject): Boolean {
        if (l is Block) {
            if (l.getCollision(l.xPixel, l.yPixel) != null) {
                return false
            }
            for (x in 0 until l.type.widthTiles) {
                for (y in 0 until l.type.heightTiles) {
                    getChunkFromTile(l.xTile + x, l.yTile + y).setBlock(l, l.xTile + x, l.yTile + y, (x == 0 && y == 0))
                }
            }
            l.inLevel = true
            return true
        } else if (l is MovingObject) {
            if (l is DroppedItem) {
                // get nearest dropped item of the same type that is not a full stack
                val d = getDroppedItemsInRadius(l.xPixel, l.yPixel, DROPPED_ITEM_PICK_UP_RANGE, { it.type == l.type && it.quantity < it.type.maxStack }).maxBy { it.quantity }
                if (d != null) {
                    if (d.quantity + l.quantity <= l.type.maxStack) {
                        d.quantity += l.quantity
                        // dont set in level because it was never technically called into existence
                        return true
                    } else {
                        l.quantity -= (l.type.maxStack - d.quantity)
                        d.quantity = l.type.maxStack
                    }
                }
                if (l.getCollision(l.xPixel, l.yPixel) == null) {
                    if (l.hitbox != Hitbox.NONE) {
                        l.intersectingChunks.forEach { it.movingOnBoundary!!.add(l) }
                    }
                    l.currentChunk.addMoving(l)
                    l.inLevel = true
                    l.currentChunk.addDroppedItem(l)
                    return true
                }
                return false
            } else {
                if (l.getCollision(l.xPixel, l.yPixel) != null)
                    return false
                if (l.hitbox != Hitbox.NONE) {
                    l.intersectingChunks.forEach { it.movingOnBoundary!!.add(l) }
                }
                l.currentChunk.addMoving(l)
                l.inLevel = true
                return true
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
        for (x in -1..1) {
            for (y in -1..1) {
                if (Math.abs(x) != Math.abs(y))
                    getAllTransferNodes(o.xTile + x, o.yTile + y).forEach { updateNode(it) }
            }
        }
    }

    fun remove(l: LevelObject): Boolean {
        val c = getChunk(l.xChunk, l.yChunk)
        if (l.inLevel) {
            if (l is Block) {
                for (x in 0 until l.type.widthTiles) {
                    for (y in 0 until l.type.heightTiles) {
                        c.removeBlock(l, l.xTile + x, l.yTile + y, (x == 0 && y == 0)) // TODO fix removing miner
                    }
                }
                l.inLevel = false
                return true
            } else if (l is MovingObject) {
                if (l is DroppedItem) {
                    l.currentChunk.removeDroppedItem(l)
                }
                if (l.hitbox != Hitbox.NONE)
                    l.intersectingChunks.forEach { it.movingOnBoundary!!.remove(l) }
                l.currentChunk.removeMoving(l)
                l.inLevel = false
            }
            return true
        }
        return false
    }

    /**
     * Tries to 'materialize' this resource where specified. Note: most resources have no physical representation
     * other than some purely decorative particles
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
        return getChunk(xTile shr CHUNK_TILE_EXP, yTile shr CHUNK_TILE_EXP, load)
    }

    fun getChunkFromPixel(xPixel: Int, yPixel: Int, load: Boolean = true): Chunk {
        return getChunkFromTile(xPixel shr 4, yPixel shr 4, load)
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

    fun getMoving(xPixel: Int, yPixel: Int): MovingObject? {
        val chunk = getChunkFromPixel(xPixel, yPixel)
        var ret: MovingObject? = null
        ret = chunk.moving!!.firstOrNull { GeometryHelper.contains(it.xPixel + it.hitbox.xStart, it.yPixel + it.hitbox.yStart, it.hitbox.width, it.hitbox.height, xPixel, yPixel, 0, 0) }
        if (ret == null)
            ret = chunk.movingOnBoundary!!.firstOrNull { GeometryHelper.contains(it.xPixel + it.hitbox.xStart, it.yPixel + it.hitbox.yStart, it.hitbox.width, it.hitbox.height, xPixel, yPixel, 0, 0) }
        return ret
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