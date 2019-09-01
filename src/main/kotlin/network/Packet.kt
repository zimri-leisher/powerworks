package network

import java.io.DataOutputStream

open class Packet(val type: PacketType) {
    open fun write(out: DataOutputStream) {
        out.writeInt(type.id)
    }
}