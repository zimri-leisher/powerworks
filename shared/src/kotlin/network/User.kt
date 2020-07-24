package network

import serialization.Id
import java.util.*

data class User(
        @Id(1)
        val id: UUID,
        @Id(2) // todo display name fetched from server
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
