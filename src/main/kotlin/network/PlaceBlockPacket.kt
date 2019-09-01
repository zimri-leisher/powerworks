package network

import java.io.DataOutputStream

class PlaceBlockPacket(val blockTypeID: Int, val xTile: Int, val yTile: Int, val clientId: Int, val levelName: String) : Packet(PacketType.PLACE_BLOCK) {

    override fun write(out: DataOutputStream) {
        super.write(out)
        out.writeInt(blockTypeID)
        out.writeInt(xTile)
        out.writeInt(yTile)
        out.writeInt(clientId)
        out.writeUTF(levelName)
    }
}