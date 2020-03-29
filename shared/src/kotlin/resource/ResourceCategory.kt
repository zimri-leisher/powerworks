package resource

enum class ResourceCategory {
    // don't change these without changing the lists in the Chunk class. They are allocated without looking at this for
    // performance reasons
    ITEM, ENERGY, GAS, FLUID, HEAT;

    companion object {
        // hoo yeah kotlin
        operator fun iterator() = values().iterator()
    }
}