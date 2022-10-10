package resource

import misc.Coord
import misc.Geometry

class PipeNetworkConnection(
    val network: PipeNetwork,
    val steps: List<PipeNetworkVertex>
) : ResourceNodeConnection(steps.first() as ResourceNode, steps.last() as ResourceNode) {
    val currentPackets = mutableListOf<PipeNetworkPacket>()

    override fun execute(transaction: ResourceTransaction) {
        val packet = PipeNetworkPacket(this, transaction, Coord(from.xTile * 16, from.yTile * 16))
        currentPackets.add(packet)
        transaction.start()
    }

    override fun getExecutionCost(transaction: ResourceTransaction): Int {
        var dist = 0.0
        var idx = 0
        while(idx < steps.size - 1) {
            val thisStep = steps[idx]
            val nextStep = steps[idx + 1]
            dist += Geometry.distance(thisStep.xTile, thisStep.yTile, nextStep.xTile, nextStep.yTile)
            idx++
        }
        // do something to dist based on network speed
        return dist.toInt() * 16
    }

    fun update() {
        val completed = mutableListOf<PipeNetworkPacket>()
        for(packet in currentPackets) {
            if(packet.state == PipeNetworkPacketState.FINISHED) {
                completed.add(packet)
                packet.transaction.finish()
            } else if(packet.state == PipeNetworkPacketState.INVALID) {
                // try solve invalid state

            } else {
                packet.update()
            }
        }
        currentPackets.removeAll(completed)
    }

    fun render() {
        currentPackets.forEach { it.render() }
    }
}