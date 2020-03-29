package network.packet

import level.moving.MovingObjectType
import serialization.Id
import java.util.*

class AddMovingObjectToLevel(
        @Id(2)
        val movingType: MovingObjectType<*>,
        @Id(3)
        val xPixel: Int,
        @Id(4)
        val yPixel: Int,
        @Id(5)
        val rotation: Int,
        @Id(6)
        val levelId: UUID) : Packet(PacketType.ADD_MOVING_TO_LEVEL) {
    private constructor() : this(MovingObjectType.ERROR, 0, 0, 0, UUID.randomUUID())
}