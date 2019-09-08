package network.packet

class ServerHandshakePacket(val clientTimestamp: Long, val serverTimestamp: Long) : Packet(PacketType.SERVER_HANDSHAKE)