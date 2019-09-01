package network

import java.io.DataInputStream
import java.io.InvalidObjectException

private var nextId = 0

enum class PacketType(val read: PacketType.(input: DataInputStream) -> Packet) {
    ERROR({ Packet(this) }),
    CLIENT_HANDSHAKE({ input ->
        ClientHandshakePacket(input.readLong())
    }),
    SERVER_HANDSHAKE({ input ->
        ServerHandshakePacket(input.readLong(), input.readLong())
    }),
    PLACE_BLOCK({ input ->
        PlaceBlockPacket(input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readUTF())
    });
//    VERSION({ input ->
//        VersionPacket(input.read().toString())
//    });
//    MOVING_OBJECT_MOVE_TO({input ->
//        MovingObjectMoveToPacket(input.readInt(), input.readInt(), input.readInt())
//    }),
//

    val id = nextId++

    companion object {
        fun read(packetId: Int, input: DataInputStream): Packet {
            val type = values().firstOrNull { it.id == packetId }
            return type?.read?.invoke(type, input) ?: throw InvalidObjectException("Packet is badly formed")
        }
    }
}