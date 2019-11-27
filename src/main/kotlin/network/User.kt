package network

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import java.util.*

data class User(
        @Tag(1)
        val id: UUID,
        @Tag(2)
        val displayName: String) {
    private constructor() : this(UUID.randomUUID(), "")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}