package main

enum class Version(val major: String, val minor: String, val patch: String, private val isCompatible: Version.(other: Version) -> Boolean) {
    `0`("0", "0", "0", { false }),
    `0_4_1`("0", "4", "1", { other ->
        other.major == this.major && other.minor == this.minor && other.patch == this.patch
    });

    fun isCompatible(other: Version) = this.isCompatible.invoke(this, other)

    companion object {
        fun get(major: String, minor: String, patch: String) = values().firstOrNull { it.major == major && it.minor == minor && it.patch == patch }
                ?: throw IllegalArgumentException("No version $major.$minor.$patch exists")
    }
}