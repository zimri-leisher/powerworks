package fluid

import resource.*
import serialization.Id
import java.lang.Integer.min

class FluidTank(
    @Id(7)
    val maxAmount: Int
) : ResourceContainer() {

    private constructor() : this(0)

    @Id(8)
    var currentFluidType: FluidType? = null

    @Id(9)
    var currentAmount = 0
        set(value) {
            field = value
            if (value == 0) {
                currentFluidType = null
            }
        }

    override val totalQuantity: Int
        get() = currentAmount

    override fun add(resources: ResourceList): Boolean {
        if (!canAdd(resources))
            return false
        list@ for ((resource, quantity) in resources) {
            if (currentFluidType == null)
                currentFluidType = resource as FluidType
            currentAmount += quantity
        }
        listeners.forEach { it.onAddToContainer(this, resources) }
        return true
    }

    override fun mostPossibleToAdd(list: ResourceList): ResourceList {
        if (currentFluidType != null) {
            if (currentAmount == maxAmount) {
                return emptyResourceList()
            } else {
                val desiredAddition = list[currentFluidType!!]
                return resourceListOf(currentFluidType!! to min(maxAmount - currentAmount, desiredAddition))
            }
        } else {
            val biggestEntry =
                list.filterKeys { it.category == ResourceCategory.FLUID }.maxByOrNull { (_, value) -> value }
                    ?: return emptyResourceList()
            return resourceListOf(biggestEntry.key to min(maxAmount, biggestEntry.value))
        }
    }

    override fun remove(resources: ResourceList): Boolean {
        if (!canRemove(resources))
            return false
        val (_, quantity) = resources[0]
        currentAmount -= quantity
        if (currentAmount == 0)
            currentFluidType = null
        listeners.forEach { it.onRemoveFromContainer(this, resources) }
        return true
    }

    override fun mostPossibleToRemove(list: ResourceList): ResourceList {
        if (currentFluidType == null || currentAmount == 0) {
            return emptyResourceList()
        }
        val desiredRemoval = list[currentFluidType!!]
        return resourceListOf(currentFluidType!! to min(currentAmount, desiredRemoval))
    }

    override fun clear() {
        currentFluidType = null
        currentAmount = 0
        listeners.forEach { it.onContainerClear(this) }
    }

    override fun copy(): ResourceContainer {
        val ret = FluidTank(maxAmount)
        ret.currentFluidType = currentFluidType
        ret.currentAmount = currentAmount
        return ret
    }

    override fun getQuantity(type: ResourceType): Int {
        if (type == currentFluidType)
            return currentAmount
        return 0
    }

    override fun toResourceList() =
        if (currentFluidType == null || currentAmount == 0) ResourceList() else ResourceList(currentFluidType!! to currentAmount)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FluidTank

        if (maxAmount != other.maxAmount) return false
        if (currentFluidType != other.currentFluidType) return false
        if (currentAmount != other.currentAmount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxAmount
        result = 31 * result + (currentFluidType?.hashCode() ?: 0)
        result = 31 * result + currentAmount
        return result
    }
}