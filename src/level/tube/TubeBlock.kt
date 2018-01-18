package level.tube

import graphics.Image
import graphics.Renderer
import level.block.Block
import level.block.BlockType
import level.node.TransferNode
import main.Game
import misc.GeometryHelper
import misc.GeometryHelper.getOppositeAngle
import misc.GeometryHelper.getXSign
import misc.GeometryHelper.getYSign
import misc.GeometryHelper.isOppositeAngle

class TubeBlock(xTile: Int, yTile: Int) : Block(BlockType.TUBE, yTile, xTile) {

    var state = TubeState.NONE

    val tubeConnections = arrayOfNulls<TubeBlock>(4)
    val nodeConnections = arrayOf<
            MutableList<TransferNode<*>>
            >(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())

    val closedEnds: Array<Boolean>
        get() = state.closedEnds

    var group = TubeBlockGroup()

    init {
        group.addTube(this)
    }

    override fun onAddToLevel() {
        updateConnections()
        updateState()
        updateGroup()
        group.convertToIntersection(this)
        super.onAddToLevel()
    }

    override fun onAdjacentBlockAdd(b: Block) {
        // If it is a tube block, its onAddToLevel() event will call updateConnections() which will do the connecting for us,
        // so no need to worry about us doing it for them
        if (b !is TubeBlock) {
            updateNodeConnections(GeometryHelper.getDir(b.xTile - xTile, b.yTile - yTile))
            updateState()
        } else {
            group.convertToIntersection(this)
        }
    }

    override fun onAdjacentBlockRemove(b: Block) {
        updateGroup()
        updateState()
        group.convertToIntersection(this)
    }


    fun updateGroup() {
        for (i in 0..3) {
            val t = tubeConnections[i]
            if (t != null) {
                mergeGroups(t)
            }
        }
    }

    private fun mergeGroups(t: TubeBlock) {
        if (t.group.size > group.size)
            t.group.combine(group)
        else
            group.combine(t.group)
    }

    fun updateConnections() {
        for (i in 0..3)
            updateConnection(i)
    }

    fun updateConnection(dir: Int) {
        updateTubeConnection(dir)
        updateNodeConnections(dir)
    }

    fun updateTubeConnection(dir: Int) {
        // If there is a node connection, and tubes can't have nodes connecting to other tubes, then no need to check
        if (nodeConnections[dir].isEmpty()) {
            val new = getTubeAt(dir)
            // Don't do anything if there was no change
            if (tubeConnections[dir] != new) {
                tubeConnections[dir] = new
                if (new != null) {
                    mergeGroups(new)
                    new.tubeConnections[getOppositeAngle(dir)] = this
                    new.updateState()
                }
            }
        }
    }

    fun updateNodeConnections(dir: Int) {
        // If there is a node connection, and tubes can't have nodes connecting to other tubes, then no need to check
        if (tubeConnections[dir] == null) {
            // Get all nodes that could possibly disconnect to a node if placed here
            val nodes = Game.currentLevel.getAllTransferNodes(xTile + getXSign(dir), yTile + getYSign(dir), { isOppositeAngle(it.dir, dir) })
            nodeConnections[dir] = nodes
            if (nodes.isNotEmpty()) {
                group.createCorrespondingNodes(nodes)
                group.convertToIntersection(this)
            }
        }
    }

    fun updateState() {
        val dirs = arrayOf(false, false, false, false)
        for (i in 0..3)
            if (hasConnection(i))
                dirs[i] = true
        state = TubeState.getState(dirs)
    }

    fun hasConnection(dir: Int): Boolean {
        return tubeConnections[dir] != null || nodeConnections[dir].isNotEmpty()
    }

    fun getTubeAt(dir: Int): TubeBlock? {
        val b = Game.currentLevel.getBlock(xTile + getXSign(dir), yTile + getYSign(dir))
        if (b != null && b is TubeBlock) {
            return b
        }
        return null
    }

    override fun render() {
        Renderer.renderTexture(state.texture, xPixel - type.textureXPixelOffset, yPixel - type.textureYPixelOffset)
        if (closedEnds[0])
            Renderer.renderTexture(Image.Block.TUBE_UP_CLOSE, xPixel, yPixel - Image.Block.TUBE_UP_CLOSE.heightPixels + 4)
        if (closedEnds[1])
            Renderer.renderTexture(Image.Block.TUBE_RIGHT_CLOSE, xPixel + 16, yPixel)
        if (closedEnds[2])
            Renderer.renderTexture(Image.Block.TUBE_DOWN_CLOSE, xPixel, yPixel + 8)
        if (closedEnds[3])
            Renderer.renderTexture(Image.Block.TUBE_LEFT_CLOSE, xPixel - Image.Block.TUBE_LEFT_CLOSE.widthPixels, yPixel)
        if (Game.RENDER_HITBOXES)
            renderHitbox()
    }

    // state = getState(connections)
    // connections = for each dir, if there is a tube connection or a node connection
    //
    // updateTubeConnection(dir)
    // if there is a tube at dir
    //   tubeconnections[dir] = that tube
    //   if it has changed
    //     updateIntersectionsAround(this)
    //     if our groups are different
    //       merge them
    //
    // updateNodeConnections(dir)
    // if there is a node at dir
    //   nodeconnections[dir] = all the nodes at dir
    //   if it has changed
    //      add corresponding nodes to group
    //      updateIntersectionsAround(this)

    /*
     * on add tube block
     *   if not combining
     *
     */
}