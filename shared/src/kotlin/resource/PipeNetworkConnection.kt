package resource

import level.LevelManager
import misc.Coord
import misc.Geometry
import serialization.AsReference
import serialization.Id

class PipeNetworkConnection(
    @Id(1)
    @AsReference
    val network: PipeNetwork,
    @Id(2)
    val steps: List<PipeNetworkVertex>
) : ResourceNodeConnection(steps.first().obj as ResourceNode, steps.last().obj as ResourceNode) {

    private constructor() : this(PipeNetwork(LevelManager.EMPTY_LEVEL), listOf())

    @Id(3)
    val currentPackets = mutableListOf<PipeNetworkPacket>()

    override val cost: Int

    init {
        var dist = 0.0
        var idx = 0
        while (idx < steps.size - 1) {
            val thisStep = steps[idx]
            val nextStep = steps[idx + 1]
            dist += Geometry.distance(thisStep.xTile, thisStep.yTile, nextStep.xTile, nextStep.yTile)
            idx++
        }
        // do something to dist based on network speed
        cost = dist.toInt() * 16
    }

    override fun execute(transaction: ResourceTransaction) {
        val packet = PipeNetworkPacket(this, transaction, Coord(from.xTile * 16, from.yTile * 16))
        currentPackets.add(packet)
        transaction.start()
    }

    fun update() {
        val completed = mutableListOf<PipeNetworkPacket>()
        for (packet in currentPackets) {
            if (packet.state == PipeNetworkPacketState.FINISHED) {
                completed.add(packet)
                packet.transaction.finish()
            } else if (packet.state == PipeNetworkPacketState.INVALID) {
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