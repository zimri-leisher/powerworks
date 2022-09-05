package level.pipe

import level.Level
import level.block.Block
import level.block.PipeBlockType
import resource.*
import serialization.Id

abstract class PipeBlock(override val type: PipeBlockType<out PipeBlock>, xTile: Int, yTile: Int) :
    Block(type, xTile, yTile, 0), PipeNetworkVertex {

    @Id(20)
    override val farEdges = arrayOfNulls<PipeNetworkVertex>(4)

    @Id(222)
    override val nearEdges = arrayOfNulls<PipeNetworkVertex>(4)

    var state = PipeState.NONE

    val closedEnds: Array<Boolean>
        get() = state.closedEnds

    override var network: PipeNetwork? = null

    override val validFarVertex get() = state in PipeState.Group.INTERSECTION

    override fun afterAddToLevel(oldLevel: Level) {
        network = PipeNetwork(oldLevel)
        network!!.add(this)
        super.afterAddToLevel(oldLevel)
    }

    override fun afterRemoveFromLevel(oldLevel: Level) {
        network?.remove(this)
        network = null
        super.afterRemoveFromLevel(oldLevel)
    }

    fun updateState() {
        // network will handle this
        state = PipeState.getState(
            nearEdges[0] != null,
            nearEdges[1] != null,
            nearEdges[2] != null,
            nearEdges[3] != null,
        )
    }
}