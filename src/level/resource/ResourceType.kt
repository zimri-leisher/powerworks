package level.resource

interface ResourceType {
    companion object {
        const val ITEM = 0
        const val ENERGY = 1
        const val GAS = 2
        const val FLUID = 3
        const val HEAT = 4

        const val NUM_TYPES = 5
    }
}