package resource

import graphics.Renderer
import misc.Coord
import misc.Geometry

enum class PipeNetworkPacketState {
    NEW, PENDING, INVALID, FINISHED
}

class PipeNetworkPacket(
    val connection: PipeNetworkConnection,
    val transaction: ResourceTransaction,
    var pos: Coord
) {
    var dir = 0
    var state = PipeNetworkPacketState.NEW
    var currentVertexIdx = 0

    fun render() {
        for ((type, quantity) in transaction.resources) {
            type.icon.render(pos.x, pos.y)
            Renderer.renderText(quantity, pos.x, pos.y)
        }
    }

    fun update() {
        if (state == PipeNetworkPacketState.NEW) {
            state = PipeNetworkPacketState.PENDING
        } else if (state == PipeNetworkPacketState.FINISHED) {
            return
        } else if (state == PipeNetworkPacketState.INVALID) {
            // do something
        }
        // check for invalid state
        if(!connection.canExecute(transaction)) {
            state = PipeNetworkPacketState.INVALID
            return
        }

        val nextVert = connection.steps[currentVertexIdx + 1]
        if (nextVert.xTile * 16 == pos.x && nextVert.yTile * 16 == pos.y) {
            currentVertexIdx++
            if (currentVertexIdx == connection.steps.lastIndex) {
                state = PipeNetworkPacketState.FINISHED
                return
            }
        }
        val currentVert = connection.steps[currentVertexIdx]
        dir = Geometry.getDir(currentVert.xTile * 16 - pos.x, currentVert.yTile * 16 - pos.y)
        pos = Coord(pos.x + Geometry.getXSign(dir), pos.y + Geometry.getYSign(dir))
    }
}