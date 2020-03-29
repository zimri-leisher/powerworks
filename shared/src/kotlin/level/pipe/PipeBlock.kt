package level.pipe

import graphics.Image
import graphics.Renderer
import level.block.Block
import level.block.BlockType
import level.getBlockAt
import level.getResourceNodesAt
import main.DebugCode
import main.Game
import main.heightPixels
import main.widthPixels
import misc.Geometry
import misc.Geometry.getOppositeAngle
import misc.Geometry.getXSign
import misc.Geometry.getYSign
import misc.Geometry.isOppositeAngle
import resource.ResourceCategory
import resource.ResourceNode
import serialization.Id

class PipeBlock(xTile: Int, yTile: Int) : Block(BlockType.PIPE, xTile, yTile) {

    private constructor() : this(0, 0)

    @Id(20)
    var state = PipeState.NONE
        private set

    @Id(21)
    val pipeConnections = arrayOfNulls<PipeBlock>(4)

    @Id(22)
    val nodeConnections = arrayOf<
            MutableSet<ResourceNode>
            >(mutableSetOf(), mutableSetOf(), mutableSetOf(), mutableSetOf())

    val closedEnds: Array<Boolean>
        get() = state.closedEnds

    @Id(23)
    var group = PipeBlockGroup(level)

    override fun onAddToLevel() {
        updateConnections()
        updateState()
        group.addPipe(this)
        super.onAddToLevel()
    }

    override fun onAdjacentBlockAdd(b: Block) {
        // If it is a pipe block, its onAddToLevel() event will call updateConnections() which will do the connecting for us,
        // so no need to worry about us doing it for them
        if (b !is PipeBlock) {
            val dir = Geometry.getDir(b.xTile - xTile, b.yTile - yTile)
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
        updateConnections()
        updateState()
    }

    override fun onRemoveFromLevel() {
        group.removeCorrespondingNodes(this)
        group.removePipe(this)
        super.onRemoveFromLevel()
    }

    private fun mergeGroups(t: PipeBlock) {
        if (t.group == group)
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
        updatePipeConnection(dir)
        updateNodeConnections(dir)
    }

    fun updatePipeConnection(dir: Int) {
        // If there is a node connection, and pipes can't have nodes connecting to other pipes, then no need to check
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
        // If there is a node connection, and pipes can't have nodes connecting to other pipes, then no need to check
        if (pipeConnections[dir] == null) {
            // Get all nodes that could possibly disconnect to a node if placed here
            val nodes = level.getResourceNodesAt(xTile + getXSign(dir), yTile + getYSign(dir), { it.resourceCategory == ResourceCategory.FLUID && isOppositeAngle(it.dir, dir) }).toMutableSet()
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
        val newState = PipeState.getState(dirs)
        if (newState != state) {
            state = PipeState.getState(dirs)
        }
    }

    private fun hasConnection(dir: Int): Boolean {
        return pipeConnections[dir] != null || nodeConnections[dir].isNotEmpty()
    }

    private fun getPipeAt(dir: Int): PipeBlock? {
        val b = level.getBlockAt(xTile + getXSign(dir), yTile + getYSign(dir))
        if (b != null && b is PipeBlock) {
            return b
        }
        return null
    }

    override fun render() {
        Renderer.renderTexture(state.texture, xPixel, yPixel + 1)
        if (closedEnds[0])
            Renderer.renderTexture(Image.Block.PIPE_UP_CLOSE, xPixel + 4, yPixel + 17)
        if (closedEnds[1])
            Renderer.renderTexture(Image.Block.PIPE_RIGHT_CLOSE, xPixel + 16, yPixel + (18 - Image.Block.PIPE_RIGHT_CLOSE.heightPixels) / 2)
        if (closedEnds[2])
            Renderer.renderTexture(Image.Block.PIPE_DOWN_CLOSE, xPixel + 4, yPixel - 5)
        if (closedEnds[3])
            Renderer.renderTexture(Image.Block.PIPE_LEFT_CLOSE, xPixel - Image.Block.PIPE_LEFT_CLOSE.widthPixels, yPixel + (18 - Image.Block.PIPE_LEFT_CLOSE.heightPixels) / 2)
        if (nodeConnections[0].isNotEmpty())
            Renderer.renderTexture(Image.Block.PIPE_UP_CONNECT, xPixel + 4, yPixel + 17)
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
            renderHitbox()
    }
}