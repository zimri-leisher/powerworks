package main

import serialization.*

enum class Version(
    val major: String,
    val minor: String,
    val patch: String,
    private val isCompatible: Version.(other: Version) -> Boolean = { other -> other.major == this.major && other.minor == this.minor && other.patch == this.patch }
) {
    `0`("0", "0", "0", { false }),
    `0_4_1`("0", "4", "1"),
    `0_4_2`("0", "4", "2"),
    `0_5_0`("0", "5", "0"),
    UNKNOWN_VERSION("", "", "", { false });

    fun isCompatible(other: Version) = this.isCompatible.invoke(this, other)

    companion object {
        fun get(major: String, minor: String, patch: String) =
            values().firstOrNull { it.major == major && it.minor == minor && it.patch == patch }
                ?: throw IllegalArgumentException("No version $major.$minor.$patch exists")
    }
}

class VersionSerializer(type: Class<Version>, settings: List<SerializerSetting<*>>) :
    Serializer<Version>(type, settings) {

    override val writeStrategy = object : WriteStrategy<Version>(type) {
        override fun write(obj: Version, output: Output) {
            output.writeUTF("${obj.major}:${obj.minor}:${obj.patch}")
        }
    }

    override val createStrategy = object : CreateStrategy<Version>(type) {
        override fun create(input: Input): Version {
            val (major, minor, patch) = input.readUTF().split(":").map { it.replace(":", "") }
            return Version.values().firstOrNull { it.major == major && it.minor == minor && it.patch == patch }
                ?: Version.UNKNOWN_VERSION
        }
    }
}