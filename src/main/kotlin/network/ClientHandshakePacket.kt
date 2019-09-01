package network

import java.io.DataOutputStream

class ClientHandshakePacket(val timestamp: Long) : Packet(PacketType.CLIENT_HANDSHAKE) {
    override fun write(out: DataOutputStream) {
        super.write(out)
        out.writeLong(timestamp)
    }

    override fun toString() = "ClientHandshakePacket: $timestamp"
}

class ServerHandshakePacket(val clientTimestamp: Long, val serverTimestamp: Long) : Packet(PacketType.SERVER_HANDSHAKE) {
    override fun write(out: DataOutputStream) {
        super.write(out)
        out.writeLong(clientTimestamp)
        out.writeLong(serverTimestamp)
    }

    override fun toString() = "ServerHandshakePacket: client time=$clientTimestamp, server time=$serverTimestamp"
}