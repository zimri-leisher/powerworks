package level.pipe

import level.Level
import level.block.Block
import level.block.PipeBlockType
import level.update.ResourceNetworkAddVertices
import level.update.ResourceNetworkRemoveVertices
import resource.*
import serialization.Id

abstract class PipeBlock(override val type: PipeBlockType<out PipeBlock>, xTile: Int, yTile: Int) :
    Block(type, xTile, yTile), PotentialPipeNetworkVertex {

    @Id(20)
    override var vertex: PipeNetworkVertex? = null

    @Id(21)
    var state = PipeState.NONE

    val closedEnds: Array<Boolean>
        get() = state.closedEnds

    @Id(22)
    override val networks = mutableSetOf<ResourceNetwork<*>>()

    @Id(230)
    private var currentNetwork: PipeNetwork? = PipeNetwork()

    init {
        children.add(currentNetwork!!)
    }

    override fun getNetwork(type: ResourceNetworkType): ResourceNetwork<*>? {
        if (type == ResourceNetworkType.PIPE) {
            return currentNetwork
        }
        return null
    }

    override fun onAddToNetwork(network: ResourceNetwork<*>) {
        if (network.networkType == ResourceNetworkType.PIPE) {
            if (networks.isNotEmpty()) {
                throw Exception("Cannot add $this to $network, this block is already in network $currentNetwork")
            }
            networks.add(network)
            currentNetwork = network as PipeNetwork
        } else {
            throw Exception("Unable to add $this to $network, it is not a pipe network")
        }
    }

    override fun onRemoveFromNetwork(network: ResourceNetwork<*>) {
        if (network.networkType == ResourceNetworkType.PIPE) {
            if (!networks.remove(network)) {
                throw Exception("Can't remove $this from $network because it is not in that network")
            }
            currentNetwork = null
        } else {
            throw Exception("Unable to remove $this from non-pipe network")
        }
    }

    override val validFarVertex get() = state in PipeState.Group.INTERSECTION

    override fun afterAddToLevel(oldLevel: Level) {
        // problem is that if we add network to level and then immediately add this to it, it don't work :(
        // solution is to have children level objects that are guaranteed to be added before this object is
        if (currentNetwork != null) {
            level.modify(ResourceNetworkAddVertices(currentNetwork!!, listOf(this), level))
        }
        super.afterAddToLevel(oldLevel)
    }

    override fun afterRemoveFromLevel(oldLevel: Level) {
        if (currentNetwork != null) {
            level.modify(ResourceNetworkRemoveVertices(currentNetwork!!, listOf(this), level))
        }
        super.afterRemoveFromLevel(oldLevel)
    }

    fun updateState() {
        // network will handle this
        state = PipeState.getState(
            vertex?.edges?.get(0) != null,
            vertex?.edges?.get(1) != null,
            vertex?.edges?.get(2) != null,
            vertex?.edges?.get(3) != null,
        )
    }
}