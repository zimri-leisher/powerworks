package network.packet

interface PacketHandler {
    fun handleClientPacket(packet: Packet)
    fun handleServerPacket(packet: Packet)
}