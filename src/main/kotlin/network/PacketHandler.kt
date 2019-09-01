package network

interface PacketHandler {
    fun handlePacket(packet: Packet)
}