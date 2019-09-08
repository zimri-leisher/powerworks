package network

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag

data class User(
        @Tag(1)
        val id: String)