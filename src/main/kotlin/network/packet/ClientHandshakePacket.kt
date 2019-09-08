package network.packet

import network.User

class ClientHandshakePacket(val timestamp: Long, val user: User) : Packet(PacketType.CLIENT_HANDSHAKE)