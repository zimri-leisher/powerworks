package resource

import misc.Coord

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