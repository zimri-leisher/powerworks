package network.packet

open class Packet(val type: PacketType) {

    private constructor() : this(PacketType.UNKNOWN)

    var connectionId = 0
}