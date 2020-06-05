package fluid

import resource.*
import serialization.Id

class FluidTank(
        @Id(7)
        val maxAmount: Int) : ResourceContainer(ResourceCategory.FLUID) {

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

    @Id(10)
    var expectedFluidType: FluidType? = null

    @Id(11)
    var expectedAmount = 0

    override val expected get() = if (expectedFluidType == null) ResourceList() else ResourceList(expectedFluidType!! to expectedAmount)

    override val totalQuantity: Int
        get() = currentAmount

    override fun add(resources: ResourceList, from: ResourceNode?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
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

    override fun spaceFor(list: ResourceList): Boolean {
        if (list.size != 1) {
            return false
        }
        val resource = list[0]!!.key
        val quantity = list[0]!!.value
        return currentFluidType == null || (resource == currentFluidType && currentAmount + quantity <= maxAmount)
    }

    override fun getSpaceForType(type: ResourceType): Int {
        if (currentFluidType == null) {
            return maxAmount
        }
        if (type == currentFluidType) {
            return maxAmount - currentAmount
        }
        return 0
    }

    override fun remove(resources: ResourceList, to: ResourceNode?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canRemove(resources))
                return false
        val (resource, quantity) = resources[0]!!
        currentAmount -= quantity
        if (currentAmount == 0)
            currentFluidType = null
        listeners.forEach { it.onRemoveFromContainer(this, resources) }
        return true
    }

    override fun expect(resources: ResourceList): Boolean {
        val currentExpected = if (expectedFluidType == null) resources else resources + ResourceList(expectedFluidType!! to expectedAmount)
        if (!canAdd(currentExpected)) {
            return false
        }
        expectedFluidType = resources[0]!!.key as FluidType
        expectedAmount += resources[0]!!.value
        return true
    }

    override fun cancelExpectation(resources: ResourceList): Boolean {
        if (resources[0]?.key == expectedFluidType && expectedAmount != 0) {
            expectedAmount = Math.max(0, expectedAmount - resources[0]!!.value)
            return true
        }
        return false
    }

    override fun contains(list: ResourceList): Boolean {
        if (list.size != 1) {
            return false
        }
        val resource = list[0]!!.key
        val quantity = list[0]!!.value
        return currentFluidType == resource && quantity <= currentAmount
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

    override fun getQuantity(resource: ResourceType): Int {
        if (resource == currentFluidType)
            return currentAmount
        return 0
    }

    override fun toResourceList() = if (currentFluidType == null || currentAmount == 0) ResourceList() else ResourceList(currentFluidType!! to currentAmount)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FluidTank

        if (maxAmount != other.maxAmount) return false
        if (currentFluidType != other.currentFluidType) return false
        if (currentAmount != other.currentAmount) return false
        if (expectedFluidType != other.expectedFluidType) return false
        if (expectedAmount != other.expectedAmount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxAmount
        result = 31 * result + (currentFluidType?.hashCode() ?: 0)
        result = 31 * result + currentAmount
        result = 31 * result + (expectedFluidType?.hashCode() ?: 0)
        result = 31 * result + expectedAmount
        return result
    }
}