package level

import audio.AudioManager
import graphics.Image
import graphics.RenderParams
import graphics.Renderer
import io.*
import item.ItemType
import level.block.Block
import level.block.BlockType
import level.block.GhostBlock
import level.moving.MovingObject
import level.tile.OreTileType
import level.tile.Tile
import level.tube.TubeBlockGroup
import main.Game
import misc.GeometryHelper
import misc.Numbers
import resource.ResourceNode
import resource.ResourceType
import screen.CameraMovementListener
import screen.DebugOverlay
import screen.Mouse
import screen.Mouse.DROPPED_ITEM_PICK_UP_RANGE
import screen.elements.GUILevelView
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

const val CHUNK_SIZE_TILES = 8
val CHUNK_TILE_EXP = (Math.log(CHUNK_SIZE_TILES.toDouble()) / Math.log(2.0)).toInt()
val CHUNK_PIXEL_EXP = CHUNK_TILE_EXP + 4
val CHUNK_SIZE_PIXELS = CHUNK_SIZE_TILES shl 4

abstract class Level(val levelName: String, val widthTiles: Int, val heightTiles: Int) : CameraMovementListener, MouseMovementListener, ControlPressHandler {

    val levelFile: File
    val infoFile: File

    val heightPixels = heightTiles shl 4
    val widthPixels = widthTiles shl 4
    val heightChunks = heightTiles shr CHUNK_TILE_EXP
    val widthChunks = widthTiles shr CHUNK_TILE_EXP

    val seed: Long
    val rand: Random

    val chunks: Array<Chunk>
    val loadedChunks = CopyOnWriteArrayList<Chunk>()

    val oreNoises = mutableMapOf<OreTileType, Noise>()

    var viewBeingInteractedWith: GUILevelView? = null
    private var lastViewInteractedWith: GUILevelView? = null
    private val _views = mutableListOf<GUILevelView>()
    val openViews = object : MutableList<GUILevelView> by _views {
        override fun add(element: GUILevelView): Boolean {
            val ret = _views.add(element)
            element.moveListeners.add(this@Level)
            updateViewBeingInteractedWith()
            return ret
        }

        override fun remove(element: GUILevelView): Boolean {
            val ret = _views.remove(element)
            element.moveListeners.remove(this@Level)
            updateViewBeingInteractedWith()
            return ret
        }
    }

    // TODO come up with final solution for this - make a new type of level object?
    var ghostBlockRotation = 0

    var ghostBlock: GhostBlock? = null

    var selectedLevelObject: LevelObject? = null

    var maxRenderSteps = 0

    var mouseOnLevel = false
    var mouseLevelXPixel = 0
    var mouseLevelYPixel = 0

    init {
        val p = Paths.get(Game.ENCLOSING_FOLDER_PATH, "data/save/$levelName/")
        if (Files.notExists(p)) {
            Files.createDirectory(p)
            seed = (Math.random() * 4096).toLong()
            rand = Random(seed)
            InputManager.mouseMovementListeners.add(this)
            println("Creating level")
            levelFile = Files.createFile(Paths.get(p.toAbsolutePath().toString(), "$levelName.level")).toFile()
            val levelOut = DataOutputStream(BufferedOutputStream(FileOutputStream(levelFile, true)))
            levelOut.writeLong(seed)
            levelOut.close()
            infoFile = Files.createFile(Paths.get(p.toAbsolutePath().toString(), "$levelName.info")).toFile()
            val infoOut = DataOutputStream(BufferedOutputStream(FileOutputStream(infoFile, true)))
            infoOut.writeChars(levelName)
            infoOut.writeChars(LocalDateTime.now().toString())
            infoOut.close()
        } else {
            InputManager.mouseMovementListeners.add(this)
            println("Loading level")
            levelFile = Paths.get(p.toAbsolutePath().toString(), "$levelName.level").toFile()
            val g = DataInputStream(BufferedInputStream(FileInputStream(levelFile)))
            seed = g.readLong()
            rand = Random(seed)
            infoFile = levelFile
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
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.ROTATE_BLOCK)
    }

    fun render(view: GUILevelView) {
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
                val c = Chunks.getFromTile(x, y)
                c.getTile(x, y).render()
                count++
            }
        }
        for (y in minY until maxY) {
            val yChunk = y shr CHUNK_TILE_EXP
            // Render the line of blocks
            for (x in minX until maxX) {
                val c = Chunks.get(x shr CHUNK_TILE_EXP, yChunk)
                val b = c.getBlock(x, y)
                if (b != null) {
                    b.render()
                }
                if (ghostBlock != null) {
                    val g = ghostBlock!!
                    if (g.xTile == x && g.yTile == y) {
                        g.render()
                    }
                }
            }
            // Render the moving objects in sorted order
            for (xChunk in (minX shr CHUNK_TILE_EXP) until (maxX shr CHUNK_TILE_EXP)) {
                val c = Chunks.get(xChunk, yChunk)
                if (c.moving!!.size > 0)
                    c.moving!!.filter { it.yTile >= y && it.yTile < y + 1 }.forEach {
                        it.render()
                    }
            }
        }
        if (selectedLevelObject != null && ghostBlock == null) {
            val s = selectedLevelObject!!
            if (r.contains(s.xPixel, s.yPixel)) {
                if (s is Block)
                    Renderer.renderEmptyRectangle(s.xPixel, s.yPixel, s.type.widthTiles shl 4, s.type.heightTiles shl 4, 0x1A6AF4, RenderParams(alpha = .45f))
                else if (s is MovingObject)
                    Renderer.renderEmptyRectangle(s.xPixel, s.yPixel, s.hitbox.width, s.hitbox.height, 0x1A6AF4, RenderParams(alpha = .45f))
            }
        }
        TubeBlockGroup.render()
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
        val chunksInTileRectangle = Chunks.getFromTileRectangle(minX, minY, maxX - minX - 1, maxY - minY - 1)
        if (Game.CHUNK_BOUNDARIES) {
            for (c in chunksInTileRectangle) {
                Renderer.renderEmptyRectangle(c.xTile shl 4, c.yTile shl 4, CHUNK_SIZE_PIXELS, CHUNK_SIZE_PIXELS)
                Renderer.renderText("x: ${c.xChunk}, y: ${c.yChunk}", (c.xTile shl 4), (c.yTile shl 4))
                Renderer.renderText("updates required: ${c.updatesRequired!!.size}", (c.xTile shl 4), (c.yTile shl 4) + 4)
                Renderer.renderText("moving objects: ${c.moving!!.size} (${c.movingOnBoundary!!.size} on boundary)", (c.xTile shl 4), (c.yTile shl 4) + 8)
            }
        }
        if (Game.DEBUG_TUBE_INFO || Game.RESOURCE_NODES_INFO) {
            for (c in chunksInTileRectangle) {
                for (nList in c.resourceNodes!!) {
                    for (n in nList) {
                        renderNodeDebug(n)
                    }
                }
            }
        }
        DebugOverlay.setInfo("${view.name} tile render count", count.toString())
    }

    private fun renderNodeDebug(n: ResourceNode<*>) {
        val xSign = GeometryHelper.getXSign(n.dir)
        val ySign = GeometryHelper.getYSign(n.dir)
        if (n.allowOut) {
            Renderer.renderTexture(Image.Misc.THIN_ARROW, (n.xTile shl 4) + 4 + 8 * xSign, (n.yTile shl 4) + 4 + 8 * ySign, RenderParams(rotation = 90f * n.dir))
        }
        if (n.allowIn) {
            Renderer.renderTexture(Image.Misc.THIN_ARROW, (n.xTile shl 4) + 4 + 8 * xSign, (n.yTile shl 4) + 4 + 8 * ySign, RenderParams(rotation = 90f * GeometryHelper.getOppositeAngle(n.dir)))
        }
    }

    fun update() {
        var count = 0
        TubeBlockGroup.update()
        updateChunksBeingRendered()
        for (c in loadedChunks) {
            if (c.updatesRequired!!.size == 0 && !c.beingRendered && c.resourceNodes!!.all { it.isEmpty() }) {
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
            for (c in Chunks.getFromPixelRectangle(v.viewRectangle.x, v.viewRectangle.y, v.viewRectangle.width, v.viewRectangle.height))
                c.beingRendered = true
        }
    }

    /**
     * Makes the ghost block that appears on the mouse when holding a placeable item up-to-date
     */
    private fun updateGhostBlock() {
        val currentItem = Mouse.heldItemType
        if (currentItem == null || (Game.mainInv.getQuantity(currentItem) == 0)) {
            ghostBlock = null
        } else if (currentItem.placedBlock != BlockType.ERROR) {
            val placedType = currentItem.placedBlock
            val xTile = ((mouseLevelXPixel) shr 4) - placedType.widthTiles / 2
            val yTile = ((mouseLevelYPixel) shr 4) - placedType.heightTiles / 2
            ghostBlock = GhostBlock(placedType, xTile, yTile, ghostBlockRotation)
        }
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.ROTATE_BLOCK && p.pressType == PressType.PRESSED) {
            ghostBlockRotation = (ghostBlockRotation + 1) % 4
        }
    }

    /**
     * Called when any view moves
     */
    override fun onCameraMove(view: GUILevelView, pXPixel: Int, pYPixel: Int) {
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

    fun updateViewBeingInteractedWith() {
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
        val block = Blocks.get(mouseLevelXPixel shr 4, mouseLevelYPixel shr 4)
        val moving = MovingObjects.get(mouseLevelXPixel, mouseLevelYPixel)
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

    object Blocks {
        /** @return whether or not this collides with a block */
        fun getCollision(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel, predicate: ((LevelObject) -> Boolean)? = null): LevelObject? {
            // Blocks won't have a hitbox bigger than their width/height tiles
            val blocks = getIntersectingFromPixelRectangle(l.hitbox.xStart + xPixel, l.hitbox.yStart + yPixel, l.hitbox.width, l.hitbox.height)
            for (b in blocks) {
                if ((predicate != null && predicate(b) && doesPairCollide(l, xPixel, yPixel, b)) || doesPairCollide(l, xPixel, yPixel, b))
                    return b
            }
            return null
        }

        /**
         * Will load the chunk if necessary
         */
        fun get(xTile: Int, yTile: Int): Block? {
            return Chunks.getFromTile(xTile, yTile).getBlock(xTile, yTile)
        }

        fun getIntersectingFromRectangle(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int): Set<Block> {
            val m = mutableSetOf<Block>()
            for (x in xTile..(xTile + widthTiles)) {
                for (y in yTile..(yTile + heightTiles)) {
                    val b = get(x, y)
                    if (b != null)
                        m.add(b)
                }
            }
            return m
        }

        fun getIntersectingFromPixelRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): Set<Block> {
            return getIntersectingFromRectangle(xPixel shr 4, yPixel shr 4, widthPixels shr 4, heightPixels shr 4)
        }
    }

    object MovingObjects {
        /** Whether or not this collides with a moving object */
        fun getCollision(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel, predicate: ((MovingObject) -> Boolean)? = null): LevelObject? {
            val c = Chunks.get(l.xChunk, l.yChunk)
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

        fun getInRadius(xPixel: Int, yPixel: Int, radius: Int, predicate: (MovingObject) -> Boolean = { true }): List<MovingObject> {
            val l = mutableListOf<MovingObject>()
            for (c in Chunks.getFromPixelRectangle(xPixel - radius, yPixel - radius, radius * 2, radius * 2)) {
                for (d in c.moving!!) {
                    if (predicate(d) && GeometryHelper.intersects(xPixel - radius, yPixel - radius, radius * 2, radius * 2, d.xPixel - d.hitbox.xStart, d.yPixel - d.hitbox.yStart, d.hitbox.width, d.hitbox.height)) {
                        l.add(d)
                    }
                }
            }
            return l
        }

        fun get(xPixel: Int, yPixel: Int): MovingObject? {
            val chunk = Chunks.getFromPixel(xPixel, yPixel)
            var ret = chunk.moving!!.firstOrNull { GeometryHelper.contains(it.xPixel + it.hitbox.xStart, it.yPixel + it.hitbox.yStart, it.hitbox.width, it.hitbox.height, xPixel, yPixel, 0, 0) }
            if (ret == null)
                ret = chunk.movingOnBoundary!!.firstOrNull { GeometryHelper.contains(it.xPixel + it.hitbox.xStart, it.yPixel + it.hitbox.yStart, it.hitbox.width, it.hitbox.height, xPixel, yPixel, 0, 0) }
            return ret
        }
    }

    object Chunks {
        fun updateChunkOf(o: MovingObject) {
            val currentChunk = get(o.xChunk, o.yChunk)
            if (o.hitbox != Hitbox.NONE) {
                val intersectingChunks = getFromPixelRectangle(o.hitbox.xStart + o.xPixel, o.hitbox.yStart + o.yPixel, o.hitbox.width, o.hitbox.height).toMutableList()
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

        /* Getting and setting */
        /**
         * Loads and returns the specified chunk
         */
        fun load(xChunk: Int, yChunk: Int): Chunk {
            val c = Game.currentLevel.chunks[xChunk + yChunk * Game.currentLevel.widthChunks]
            c.load(Game.currentLevel.genBlocks(xChunk, yChunk), Game.currentLevel.genTiles(xChunk, yChunk))
            return c
        }

        /**
         * If load is true, it will load the chunk if necessary
         */
        fun get(xChunk: Int, yChunk: Int, load: Boolean = true): Chunk {
            val c = Game.currentLevel.chunks[xChunk + yChunk * Game.currentLevel.widthChunks]
            if (load && !c.loaded) {
                load(xChunk, yChunk)
            }
            return c
        }

        /**
         * If load is true, it will load the chunk if necessary
         */
        fun getFromTile(xTile: Int, yTile: Int, load: Boolean = true): Chunk {
            return get(xTile shr CHUNK_TILE_EXP, yTile shr CHUNK_TILE_EXP, load)
        }

        fun getFromPixel(xPixel: Int, yPixel: Int, load: Boolean = true): Chunk {
            return getFromTile(xPixel shr 4, yPixel shr 4, load)
        }

        fun getFromRectangle(xChunk: Int, yChunk: Int, xChunk2: Int, yChunk2: Int): List<Chunk> {
            val l = mutableListOf<Chunk>()
            for (x in xChunk..xChunk2) {
                for (y in yChunk..yChunk2) {
                    l.add(get(x, y))
                }
            }
            return l
        }

        fun getFromTileRectangle(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int): List<Chunk> {
            return getFromRectangle(xTile shr CHUNK_TILE_EXP, yTile shr CHUNK_TILE_EXP, (xTile + widthTiles) shr CHUNK_TILE_EXP, (yTile + heightTiles) shr CHUNK_TILE_EXP)
        }

        fun getFromPixelRectangle(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int): List<Chunk> {
            return getFromRectangle(xPixel shr CHUNK_PIXEL_EXP, yPixel shr CHUNK_PIXEL_EXP, (xPixel + widthPixels) shr CHUNK_PIXEL_EXP, (yPixel + heightPixels) shr CHUNK_PIXEL_EXP)
        }
    }

    object ResourceNodes {
        fun <R : ResourceType> updateAttachments(o: ResourceNode<R>) {
            val attached = get<R>(o.xTile + GeometryHelper.getXSign(o.dir), o.yTile + GeometryHelper.getYSign(o.dir), o.resourceTypeID).filter { it.dir == GeometryHelper.getOppositeAngle(o.dir) }
            if (o.allowOut && o.allowIn) {
                o.attachedNode = attached.firstOrNull { it.allowIn && it.allowOut }
            } else if (o.allowOut) {
                o.attachedNode = attached.firstOrNull { it.allowIn }
            } else if (o.allowIn) {
                o.attachedNode = attached.firstOrNull { it.allowOut }
            } else {
                o.attachedNode = null
            }
        }

        fun get(xTile: Int, yTile: Int): List<ResourceNode<*>> {
            val ret = mutableListOf<ResourceNode<*>>()
            Chunks.getFromTile(xTile, yTile).resourceNodes!!.forEach { it.filter { it.xTile == xTile && it.yTile == yTile }.forEach { ret.add(it) } }
            return ret
        }

        fun <R : ResourceType> get(xTile: Int, yTile: Int, resourceTypeID: Int): List<ResourceNode<R>> {
            return get(xTile, yTile).filter { it.resourceTypeID == resourceTypeID } as List<ResourceNode<R>>
        }

        fun <R : ResourceType> getOutputs(xTile: Int, yTile: Int, resourceTypeID: Int): List<ResourceNode<R>> {
            return get<R>(xTile, yTile, resourceTypeID).filter { it.allowOut }
        }

        fun <R : ResourceType> getInputs(xTile: Int, yTile: Int, resourceTypeID: Int): List<ResourceNode<R>> {
            return get<R>(xTile, yTile, resourceTypeID).filter { it.allowIn }
        }

        fun getAll(xTile: Int, yTile: Int, predicate: (ResourceNode<*>) -> Boolean = { true }): MutableList<ResourceNode<*>> {
            val l = mutableListOf<ResourceNode<*>>()
            with(Chunks.getFromTile(xTile, yTile)) {
                resourceNodes!!.forEach { l.addAll(it.filter { predicate(it) && it.xTile == xTile && it.yTile == yTile }) }
            }
            return l
        }

        fun <R : ResourceType> getAll(xTile: Int, yTile: Int, resourceTypeID: Int, predicate: (ResourceNode<*>) -> Boolean = { true }): MutableList<ResourceNode<R>> {
            val l = mutableListOf<ResourceNode<R>>()
            with(Chunks.getFromTile(xTile, yTile)) {
                resourceNodes!!.forEach { l.addAll(it.filter { predicate(it) && it.resourceTypeID == resourceTypeID && it.xTile == xTile && it.yTile == yTile } as List<ResourceNode<R>>) }
            }
            return l
        }

        fun removeAll(xTile: Int, yTile: Int, predicate: (ResourceNode<*>) -> Boolean = { true }) {
            val c = Chunks.getFromTile(xTile, yTile)
            getAll(xTile, yTile, predicate).forEach { remove(it) }
        }
    }

    object DroppedItems {
        fun getInRadius(xPixel: Int, yPixel: Int, radius: Int, predicate: (DroppedItem) -> Boolean = { true }): List<DroppedItem> {
            val l = mutableListOf<DroppedItem>()
            for (c in Chunks.getFromPixelRectangle(xPixel - radius, yPixel - radius, radius * 2, radius * 2)) {
                for (d in c.droppedItems!!) {
                    if (predicate(d) && GeometryHelper.intersects(xPixel - radius, yPixel - radius, radius * 2, radius * 2, d.xPixel - d.hitbox.xStart, d.yPixel - d.hitbox.yStart, d.hitbox.width, d.hitbox.height)) {
                        l.add(d)
                    }
                }
            }
            return l
        }
    }


    object Tiles {
        /**
         * Will load the chunk if necessary
         */
        fun get(xTile: Int, yTile: Int): Tile {
            return Chunks.getFromTile(xTile, yTile).getTile(xTile, yTile)
        }
    }

    companion object {
        fun getCollision(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel, predicate: ((LevelObject) -> Boolean)? = null): LevelObject? {
            val m = MovingObjects.getCollision(l, xPixel, yPixel, predicate)
            if (m != null)
                return m
            val b = Blocks.getCollision(l, xPixel, yPixel, predicate)
            if (b != null)
                return b
            return null
        }

        // TODO do this async
        fun doesPairCollide(l: LevelObject, xPixel: Int = l.xPixel, yPixel: Int = l.yPixel, l2: LevelObject, xPixel2: Int = l2.xPixel, yPixel2: Int = l2.yPixel): Boolean {
            return GeometryHelper.intersects(xPixel + l.hitbox.xStart, yPixel + l.hitbox.yStart, l.hitbox.width, l.hitbox.height, xPixel2 + l2.hitbox.xStart, yPixel2 + l2.hitbox.yStart, l2.hitbox.width, l2.hitbox.height)
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
                        Chunks.getFromTile(l.xTile + x, l.yTile + y).setBlock(l, l.xTile + x, l.yTile + y, (x == 0 && y == 0))
                    }
                }
                l.inLevel = true
                return true
            } else if (l is MovingObject) {
                if (l is DroppedItem) {
                    // get nearest dropped item of the same type that is not a full stack
                    val d = DroppedItems.getInRadius(l.xPixel, l.yPixel, DROPPED_ITEM_PICK_UP_RANGE, { it.itemType == l.itemType && it.quantity < it.itemType.maxStack }).maxBy { it.quantity }
                    if (d != null) {
                        if (d.quantity + l.quantity <= l.itemType.maxStack) {
                            d.quantity += l.quantity
                            // dont set in level because it was never technically called into existence
                            return true
                        } else {
                            l.quantity -= (l.itemType.maxStack - d.quantity)
                            d.quantity = l.itemType.maxStack
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

        fun remove(l: LevelObject): Boolean {
            val c = Chunks.get(l.xChunk, l.yChunk)
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
                if (!add(DroppedItem(xPixel, yPixel, r, quantity)))
                    return 0
                return quantity
            }
            return 0
        }

        fun add(resourceNode: ResourceNode<*>) {
            if (resourceNode.inLevel)
                return
            val c = Chunks.getFromTile(resourceNode.xTile, resourceNode.yTile)
            c.addResourceNode(resourceNode)
            resourceNode.inLevel = true
            ResourceNodes.updateAttachments(resourceNode)
            for (x in -1..1) {
                for (y in -1..1) {
                    if (Math.abs(x) != Math.abs(y))
                        ResourceNodes.get(resourceNode.xTile + x, resourceNode.yTile + y).forEach { ResourceNodes.updateAttachments(it) }
                }
            }
        }

        fun remove(resourceNode: ResourceNode<*>) {
            if (!resourceNode.inLevel)
                return
            val c = Chunks.getFromTile(resourceNode.xTile, resourceNode.yTile)
            c.removeResourceNode(resourceNode)
            resourceNode.inLevel = false
            for (x in -1..1) {
                for (y in -1..1) {
                    if (Math.abs(x) != Math.abs(y))
                        ResourceNodes.get(resourceNode.xTile + x, resourceNode.yTile + y).forEach { ResourceNodes.updateAttachments(it) }
                }
            }
        }

        fun save() {

        }

    }
}