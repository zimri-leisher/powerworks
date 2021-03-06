package level.pipe

import level.block.Block
import level.block.PipeBlockType
import level.getBlockAtTile
import level.getResourceNodesAt
import misc.Geometry
import resource.ResourceCategory
import resource.ResourceNode
import routing.Intersection
import routing.PipeNetwork
import serialization.Id

abstract class PipeBlock(override val type: PipeBlockType<out PipeBlock>, xTile: Int, yTile: Int) : Block(type, xTile, yTile, 0) {
    @Id(20)
    var state = PipeState.NONE
        private set

    @Id(21)
    val pipeConnections = arrayOfNulls<PipeBlock>(4)

    @Id(22)
    val nodeConnections =
            arrayOf<MutableSet<ResourceNode>>(
                    mutableSetOf(), mutableSetOf(), mutableSetOf(), mutableSetOf())

    val closedEnds: Array<Boolean>
        get() = state.closedEnds

    abstract var network: PipeNetwork

    @Id(24)
    var intersection: Intersection? = null

    override fun onAddToLevel() {
        network.level = level
        updateConnections()
        network.addPipe(this)
        super.onAddToLevel()
    }

    override fun onRemoveFromLevel() {
        network.removePipe(this)
        super.onRemoveFromLevel()
    }

    override fun onAdjacentBlockAdd(b: Block) {
        updateConnections()
    }

    override fun onAdjacentBlockRemove(b: Block) {
        updateConnections()
    }

    fun onPipeConnectionChange(pipe: PipeBlock?) {
        if (pipe != null) {
            if (pipe.network.id == network.id)
                return
            if (pipe.network.size > network.size) {
                pipe.network.mergeIntoThis(network)
                network = pipe.network
            } else {
                network.mergeIntoThis(pipe.network)
                pipe.network = network
            }
        }
    }

    private fun onNodeConnectionAdd(nodes: List<ResourceNode>) {
        nodes.forEach { network.attachNode(it, this) }
    }

    private fun onNodeConnectionRemove(nodes: List<ResourceNode>) {
        nodes.forEach { network.disattachNode(it) }
    }

    fun updateConnections() {
        var connectionChanged = false
        for (dir in 0..3) {
            val pipe = getPipeAt(dir)
            if (pipeConnections[dir] != pipe) {
                pipeConnections[dir] = pipe
                onPipeConnectionChange(pipe)
                connectionChanged = true
            }
            val newNodes = level.getResourceNodesAt(xTile + Geometry.getXSign(dir), yTile + Geometry.getYSign(dir), { it.resourceCategory == ResourceCategory.ITEM && Geometry.isOppositeAngle(it.dir, dir) }).toMutableSet()
            if ((newNodes.isEmpty() && nodeConnections[dir].isNotEmpty()) || (newNodes.isNotEmpty() && nodeConnections[dir].isEmpty())) {
                val addedNodes = newNodes.filter { it !in nodeConnections[dir] }
                val removedNodes = nodeConnections[dir].filter { it !in newNodes }
                nodeConnections[dir] = newNodes
                if (addedNodes.isNotEmpty()) {
                    onNodeConnectionAdd(addedNodes)
                }
                if (removedNodes.isNotEmpty()) {
                    onNodeConnectionRemove(removedNodes)
                }
                connectionChanged = true
            }
        }
        if (connectionChanged) {
            val dirs = arrayOf(false, false, false, false)
            for (i in 0..3)
                if (pipeConnections[i] != null || nodeConnections[i].isNotEmpty())
                    dirs[i] = true
            state = PipeState.getState(dirs)
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
        return state in PipeState.Group.INTERSECTION || nodeConnections.any { it.isNotEmpty() }
    }

    private fun getPipeAt(dir: Int): PipeBlock? {
        val b = level.getBlockAtTile(xTile + Geometry.getXSign(dir), yTile + Geometry.getYSign(dir))
        if (b != null && b is PipeBlock) {
            return b
        }
        return null
    }
}