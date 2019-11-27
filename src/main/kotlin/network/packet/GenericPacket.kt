package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer

class GenericPacket(
        @TaggedFieldSerializer.Tag(2)
        val message: String
) : Packet(PacketType.GENERIC) {

    private constructor() : this("")
}