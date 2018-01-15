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

    var group: TubeBlockGroup? = null
        set(value) {
            if (field != value) {
                if (field != null) {
                    field!!.tubes.remove(this)
                }
                field = value
                if (value != null) {
                    value.tubes.add(this)
                }
            }
        }

    override fun onAddToLevel() {
        updateConnections()
        updateState()
        group?.convertToIntersection(this)
        super.onAddToLevel()
    }

    override fun onRemoveFromLevel() {
        for (i in 0..3) {
            tubeConnections[i]?.updateTubeConnection(getOppositeAngle(i))
            tubeConnections[i]?.updateState()
        }
        group = null
        super.onRemoveFromLevel()
    }

    override fun onAdjacentBlockAdd(b: Block) {
        // If it is a tube block, its onAddToLevel() event will call updateConnections() which will do the connecting for us,
        // so no need to worry about us doing it for them
        if (b !is TubeBlock) {
            updateNodeConnections(GeometryHelper.getDir(b.xTile - xTile, b.yTile - yTile))
            updateState()
        }
    }

    override fun onAdjacentBlockRemove(b: Block) {
        // This is an optimization to skip the unnecessary getting of nodes when calling updateNodeConnection(dir) - we know there aren't any because there can't be any blocks there,
        // and we assume no blocks = no nodes
        if (b !is TubeBlock) {
            nodeConnections[GeometryHelper.getDir(b.xTile - xTile, b.yTile - yTile)].clear()
            updateState()
        }
    }

    fun updateTubeConnection(dir: Int) {
        // If there is a node connection, and tubes can't have nodes connecting to other tubes, then no need to check
        if (nodeConnections[dir].isEmpty()) {
            val new = getTubeAt(dir)
            // Don't do anything if there was no change
            if (tubeConnections[dir] != new) {
                tubeConnections[dir] = getTubeAt(dir)
                if (new != null) {
                    mergeGroups(new)
                    new.tubeConnections[getOppositeAngle(dir)] = this
                    new.updateState()
                    group?.convertToIntersection(new)
                }
            }
        }
    }

    /**
     * Combine the groups of this and another tube block.
     * If neither have a group, a new one is made.
     * If only one has a group, the other one gets that one
     * If both have a group, the pre-existing group with the lowest tube count gets merged into the other
     */
    private fun mergeGroups(t: TubeBlock) {
        if (t.group == null) {
            // Neither
            if (group == null) {
                group = TubeBlockGroup()
                t.group = this.group
                // Only 1
            } else {
                t.group = group
            }
        } else {
            // Only 1
            if (group == null)
                group = t.group
            // Both
            else {
                if (group!!.size > t.group!!.size)
                    group!!.combine(t.group!!)
                else
                    t.group!!.combine(group!!)
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
                if (group != null)
                    group!!.createCorrespondingNodes(nodes)
            }
        }
    }

    fun updateConnections() {
        for (i in 0..3)
            updateConnection(i)
    }

    fun updateConnection(dir: Int) {
        updateTubeConnection(dir)
        updateNodeConnections(dir)
    }

    fun updateState() {
        val dirs = arrayOf(false, false, false, false)
        for (i in 0..3)
        // If it is connected to a tube or at least one node at that dir, add it
            if (tubeConnections[i] != null || nodeConnections[i].isNotEmpty())
                dirs[i] = true
        state = TubeState.getState(dirs)
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
}