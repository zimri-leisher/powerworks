package level.tube

import graphics.Image
import graphics.Renderer
import item.ItemType
import level.Level
import level.block.Block
import level.block.BlockType
import main.DebugCode
import main.Game
import misc.GeometryHelper
import misc.GeometryHelper.getOppositeAngle
import misc.GeometryHelper.getXSign
import misc.GeometryHelper.getYSign
import misc.GeometryHelper.isOppositeAngle
import resource.ResourceCategory
import resource.ResourceNode
import resource.ResourceType

class TubeBlock(xTile: Int, yTile: Int) : Block(BlockType.TUBE, xTile, yTile) {

    var state = TubeState.NONE
        private set

    val tubeConnections = arrayOfNulls<TubeBlock>(4)
    val nodeConnections = arrayOf<
            MutableList<ResourceNode<ItemType>>
            >(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())

    val closedEnds: Array<Boolean>
        get() = state.closedEnds

    var group = TubeBlockGroup()

    override fun onAddToLevel() {
        updateConnections()
        updateState()
        group.addTube(this)
        group.convertToIntersection(this)
        super.onAddToLevel()
    }

    override fun onAdjacentBlockAdd(b: Block) {
        // If it is a tube block, its onAddToLevel() event will call updateConnections() which will do the connecting for us,
        // so no need to worry about us doing it for them
        if (b !is TubeBlock) {
            val dir = GeometryHelper.getDir(b.xTile - xTile, b.yTile - yTile)
            if (dir != -1) {
                updateNodeConnections(dir)
            } else {
                // because it might be a multi block, meaning the x and y tile of it wouldn't be adjacent to this even if it is touching
                for (i in 0..3)
                    updateNodeConnections(i)
            }
            updateState()
        } else {
            group.convertToIntersection(this)
        }
    }

    override fun onAdjacentBlockRemove(b: Block) {
        // TODO
        updateConnections()
        updateState()
        // this needs to remove the intersection if it is no longer one
        group.convertToIntersection(this)
    }

    override fun onRemoveFromLevel() {
        group.removeCorrespondingNodes(this)
        group.removeTube(this)
        super.onRemoveFromLevel()
    }

    private fun mergeGroups(t: TubeBlock) {
        if(t.group == group)
            return
        if (t.group.size > group.size) {
            t.group.merge(group)
            group = t.group
        } else {
            group.merge(t.group)
            t.group = group
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
            val nodes = Level.ResourceNodes.getAll<ItemType>(xTile + getXSign(dir), yTile + getYSign(dir), ResourceCategory.ITEM, { isOppositeAngle(it.dir, dir) })
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

    private fun hasConnection(dir: Int): Boolean {
        return tubeConnections[dir] != null || nodeConnections[dir].isNotEmpty()
    }

    private fun getTubeAt(dir: Int): TubeBlock? {
        val b = Level.Blocks.get(xTile + getXSign(dir), yTile + getYSign(dir))
        if (b != null && b is TubeBlock) {
            return b
        }
        return null
    }

    override fun render() {
        Renderer.renderTexture(state.texture, xPixel, yPixel - 4)
        if (closedEnds[0])
            Renderer.renderTexture(Image.Block.TUBE_UP_CLOSE, xPixel, yPixel - Image.Block.TUBE_UP_CLOSE.heightPixels + 2)
        if (closedEnds[1])
            Renderer.renderTexture(Image.Block.TUBE_RIGHT_CLOSE, xPixel + 16, yPixel - 5)
        if (closedEnds[2])
            Renderer.renderTexture(Image.Block.TUBE_DOWN_CLOSE, xPixel, yPixel + 2)
        if (closedEnds[3])
            Renderer.renderTexture(Image.Block.TUBE_LEFT_CLOSE, xPixel - Image.Block.TUBE_LEFT_CLOSE.widthPixels, yPixel - 5)
        if(nodeConnections[0].isNotEmpty())
            Renderer.renderTexture(Image.Block.TUBE_UP_CONNECT, xPixel + 1, yPixel - Image.Block.TUBE_UP_CONNECT.heightPixels - 4)
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
            renderHitbox()
    }
}