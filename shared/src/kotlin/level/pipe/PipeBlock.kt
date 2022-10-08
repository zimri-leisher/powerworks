package level.pipe

import level.Level
import level.block.Block
import level.block.PipeBlockType
import resource.*
import serialization.Id
import kotlin.math.PI

abstract class PipeBlock(override val type: PipeBlockType<out PipeBlock>, xTile: Int, yTile: Int) :
    Block(type, xTile, yTile), PotentialPipeNetworkVertex {

    override var vertex: PipeNetworkVertex? = null

    var state = PipeState.NONE

    val closedEnds: Array<Boolean>
        get() = state.closedEnds

    private var network: PipeNetwork? = null
    override fun getNetwork(type: ResourceNetworkType): ResourceNetwork<*>? {
        if(type == ResourceNetworkType.PIPE) {
            return network
        }
        return null
    }

    override fun onAddToNetwork(network: ResourceNetwork<*>) {
        if(network.networkType != ResourceNetworkType.PIPE) {
            this.network = network as PipeNetwork
        } else {
            throw Exception("Unable to add $this to $network, it is not a pipe network")
        }
    }

    override fun onRemoveFromNetwork(network: ResourceNetwork<*>) {
        if(network.networkType == ResourceNetworkType.PIPE) {
            if(this.network == network) {
                this.network = null
            } else {
                throw Exception("Can't remove $this from $network because it is not in that network")
            }
        } else {
            throw Exception("Unable to remove $this from non-pipe network")
        }
    }

    override val validFarVertex get() = state in PipeState.Group.INTERSECTION

    override fun afterAddToLevel(oldLevel: Level) {
        val network = PipeNetwork(oldLevel)
        network.add(this)
        super.afterAddToLevel(oldLevel)
    }

    override fun afterRemoveFromLevel(oldLevel: Level) {
        network?.remove(this)
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