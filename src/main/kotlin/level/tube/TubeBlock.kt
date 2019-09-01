package level.tube

import graphics.Image
import graphics.Renderer
import level.Level
import level.block.Block
import level.block.BlockType
import level.getBlockAt
import level.getResourceNodesAt
import level.remove
import main.DebugCode
import main.Game
import main.heightPixels
import main.widthPixels
import misc.Geometry.getXSign
import misc.Geometry.getYSign
import misc.Geometry.isOppositeAngle
import resource.ResourceCategory
import resource.ResourceNode
import routing.Intersection
import routing.TubeRoutingNetwork

class TubeBlock(xTile: Int, yTile: Int) : Block(BlockType.TUBE, xTile, yTile) {

    var state = TubeState.NONE
        private set

    val tubeConnections = arrayOfNulls<TubeBlock>(4)
    val nodeConnections =
            arrayOf<MutableSet<ResourceNode>>(
                    mutableSetOf(), mutableSetOf(), mutableSetOf(), mutableSetOf())

    val closedEnds: Array<Boolean>
        get() = state.closedEnds

    var network = TubeRoutingNetwork(level)
    var intersection: Intersection? = null

    override fun onAddToLevel() {
        updateConnections()
        network.addTube(this)
        super.onAddToLevel()
    }

    override fun onRemoveFromLevel() {
        network.removeTube(this)
        if (intersection != null) {
            network.removeIntersection(intersection!!)
        }
        val toRemove = network.internalNodes.filter { it.xTile == xTile && it.yTile == yTile }
        toRemove.forEach { level.remove(it); network.internalNodes.remove(it) }
        super.onRemoveFromLevel()
    }

    override fun onAdjacentBlockAdd(b: Block) {
        updateConnections()
    }

    override fun onAdjacentBlockRemove(b: Block) {
        updateConnections()
    }

    fun onTubeConnectionChange(tube: TubeBlock?) {
        if (tube != null) {
            if (tube.network == network)
                return
            if (tube.network.size > network.size) {
                tube.network.mergeIntoThis(network)
                network = tube.network
            } else {
                network.mergeIntoThis(tube.network)
                tube.network = network
            }
        }
    }

    private fun onNodeConnectionAdd(nodes: List<ResourceNode>) {
        nodes.forEach { network.attachNode(it) }
    }

    private fun onNodeConnectionRemove(nodes: List<ResourceNode>) {
        nodes.forEach { network.disattachNode(it) }
    }

    fun updateConnections() {
        var connectionChanged = false
        for (dir in 0..3) {
            val tube = getTubeAt(dir)
            if (tubeConnections[dir] != tube) {
                tubeConnections[dir] = tube
                onTubeConnectionChange(tube)
                connectionChanged = true
            }
            val newNodes = level.getResourceNodesAt(xTile + getXSign(dir), yTile + getYSign(dir), { it.resourceCategory == ResourceCategory.ITEM && isOppositeAngle(it.dir, dir) }).toMutableSet()
            if ((newNodes.isEmpty() && nodeConnections[dir].isNotEmpty()) || (newNodes.isNotEmpty() && nodeConnections[dir].isEmpty())) {
                val addedNodes = newNodes.filter { it !in nodeConnections[dir] }
                val removedNodes = nodeConnections[dir].filter { it !in newNodes }
                nodeConnections[dir] = newNodes
                if(addedNodes.isNotEmpty()) {
                    onNodeConnectionAdd(addedNodes)
                }
                if(removedNodes.isNotEmpty()) {
                    onNodeConnectionRemove(removedNodes)
                }
                connectionChanged = true
            }
        }
        if (connectionChanged) {
            val dirs = arrayOf(false, false, false, false)
            for (i in 0..3)
                if (tubeConnections[i] != null || nodeConnections[i].isNotEmpty())
                    dirs[i] = true
            state = TubeState.getState(dirs)
            if (shouldBeIntersection()) {
                network.updateIntersection(this)
                intersection = network.getIntersection(this)
            } else {
                if (intersection != null) {
                    network.removeIntersection(intersection!!)
                }
                intersection = null
            }
        }
    }

    // Assumes state is updated
    fun shouldBeIntersection(): Boolean {
        return state in TubeState.Group.INTERSECTION || nodeConnections.any { it.isNotEmpty() }
    }

    private fun getTubeAt(dir: Int): TubeBlock? {
        val b = level.getBlockAt(xTile + getXSign(dir), yTile + getYSign(dir))
        if (b != null && b is TubeBlock) {
            return b
        }
        return null
    }

    override fun render() {
        Renderer.renderTexture(state.texture, xPixel, yPixel)
        if (closedEnds[0])
            Renderer.renderTexture(Image.Block.TUBE_UP_CLOSE, xPixel, yPixel + 16)
        if (closedEnds[1])
            Renderer.renderTexture(Image.Block.TUBE_RIGHT_CLOSE, xPixel + 16, yPixel + 20 - Image.Block.TUBE_RIGHT_CLOSE.heightPixels)
        if (closedEnds[2])
            Renderer.renderTexture(Image.Block.TUBE_DOWN_CLOSE, xPixel, yPixel + 14 - Image.Block.TUBE_DOWN_CLOSE.heightPixels)
        if (closedEnds[3])
            Renderer.renderTexture(Image.Block.TUBE_LEFT_CLOSE, xPixel - Image.Block.TUBE_LEFT_CLOSE.widthPixels, yPixel + 20 - Image.Block.TUBE_LEFT_CLOSE.heightPixels)
        if (nodeConnections[0].isNotEmpty())
            Renderer.renderTexture(Image.Block.TUBE_UP_CONNECT, xPixel + 1, yPixel + 19)
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
            renderHitbox()
    }
}