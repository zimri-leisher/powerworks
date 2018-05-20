package level.pipe

import fluid.FluidType
import graphics.Image
import graphics.Renderer
import level.Level
import level.block.Block
import level.block.BlockType
import level.tube.TubeBlock
import main.DebugCode
import main.Game
import misc.GeometryHelper
import misc.GeometryHelper.getOppositeAngle
import misc.GeometryHelper.getXSign
import misc.GeometryHelper.getYSign
import misc.GeometryHelper.isOppositeAngle
import resource.ResourceCategory
import resource.ResourceNode

class PipeBlock(xTile: Int, yTile: Int) : Block(BlockType.PIPE, xTile, yTile) {

    var state = PipeState.NONE
        private set

    val pipeConnections = arrayOfNulls<PipeBlock>(4)
    val nodeConnections = arrayOf<
            MutableList<ResourceNode<FluidType>>
            >(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())

    val closedEnds: Array<Boolean>
        get() = state.closedEnds

    var group = PipeBlockGroup()

    init {
        group.addPipe(this)
    }

    override fun onAddToLevel() {
        updateConnections()
        updateState()
        updateGroup()
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
        }
    }

    override fun onAdjacentBlockRemove(b: Block) {
        updateGroup()
        updateState()
    }


    private fun updateGroup() {
        for (i in 0..3) {
            val t = pipeConnections[i]
            if (t != null) {
                mergeGroups(t)
            }
        }
    }

    private fun mergeGroups(t: PipeBlock) {
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
            val new = getPipeAt(dir)
            // Don't do anything if there was no change
            if (pipeConnections[dir] != new) {
                pipeConnections[dir] = new
                if (new != null) {
                    mergeGroups(new)
                    new.pipeConnections[getOppositeAngle(dir)] = this
                    new.updateState()
                }
            }
        }
    }

    fun updateNodeConnections(dir: Int) {
        // If there is a node connection, and tubes can't have nodes connecting to other tubes, then no need to check
        if (pipeConnections[dir] == null) {
            // Get all nodes that could possibly disconnect to a node if placed here
            val nodes = Level.ResourceNodes.getAll<FluidType>(xTile + getXSign(dir), yTile + getYSign(dir), ResourceCategory.FLUID, { isOppositeAngle(it.dir, dir) })
            nodeConnections[dir] = nodes
            if (nodes.isNotEmpty()) {
                group.createCorrespondingNodes(nodes)
            }
        }
    }

    fun updateState() {
        val dirs = arrayOf(false, false, false, false)
        for (i in 0..3)
            if (hasConnection(i))
                dirs[i] = true
        state = PipeState.getState(dirs)
    }

    private fun hasConnection(dir: Int): Boolean {
        return pipeConnections[dir] != null || nodeConnections[dir].isNotEmpty()
    }

    private fun getPipeAt(dir: Int): PipeBlock? {
        val b = Level.Blocks.get(xTile + getXSign(dir), yTile + getYSign(dir))
        if (b != null && b is PipeBlock) {
            return b
        }
        return null
    }

    override fun render() {
        Renderer.renderTexture(state.texture, xPixel, yPixel)
        if (closedEnds[0])
            Renderer.renderTexture(Image.Block.PIPE_UP_CLOSE, xPixel + 4, yPixel - Image.Block.PIPE_UP_CLOSE.heightPixels)
        if (closedEnds[1])
            Renderer.renderTexture(Image.Block.PIPE_RIGHT_CLOSE, xPixel + 16, yPixel + 4)
        if (closedEnds[2])
            Renderer.renderTexture(Image.Block.PIPE_DOWN_CLOSE, xPixel + 4, yPixel + 12)
        if (closedEnds[3])
            Renderer.renderTexture(Image.Block.PIPE_LEFT_CLOSE, xPixel - Image.Block.PIPE_LEFT_CLOSE.widthPixels, yPixel + 4)
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
            renderHitbox()
    }
}