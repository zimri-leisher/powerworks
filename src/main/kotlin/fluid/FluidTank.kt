package fluid

import resource.*

class FluidTank(val maxAmount: Int, typeRule: ResourceContainer.(ResourceType) -> Boolean = { true }) : ResourceContainer(ResourceCategory.FLUID, typeRule) {
    var currentFluidType: FluidType? = null

    var currentAmount = 0
        set(value) {
            field = value
            if (value == 0) {
                currentFluidType = null
            }
        }

    var expectedFluidType: FluidType? = null
    var expectedAmount = 0

    override val totalQuantity: Int
        get() = currentAmount

    override fun add(resource: ResourceType, quantity: Int, from: ResourceNode?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canAdd(resource, quantity))
                return false
        resource as FluidType
        if (currentFluidType == null)
            currentFluidType = resource
        currentAmount += quantity
        listeners.forEach { it.onContainerChange(this, resource, quantity) }
        return true
    }

    override fun spaceFor(list: ResourceList): Boolean {
        if (list.size != 1) {
            return false
        }
        val resource = list[0]!!.first
        val quantity = list[0]!!.second
        return currentFluidType == null || (resource == currentFluidType && currentAmount + quantity <= maxAmount)
    }

    override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canRemove(resource, quantity))
                return false
        currentAmount -= quantity
        if (currentAmount == 0)
            currentFluidType = null
        listeners.forEach { it.onContainerChange(this, resource, -quantity) }
        return true
    }

    override fun expect(resource: ResourceType, quantity: Int): Boolean {
        if (canAdd(resource, expectedAmount + quantity)) {
            expectedFluidType = resource as FluidType
            expectedAmount += quantity
            return true
        }
        return false
    }

    override fun cancelExpectation(resource: ResourceType, quantity: Int): Boolean {
        if (resource == expectedFluidType && expectedAmount != 0) {
            expectedAmount = Math.max(0, expectedAmount - quantity)
            return true
        }
        return false
    }

    override fun contains(list: ResourceList): Boolean {
        if (list.size != 1) {
            return false
        }
        val resource = list[0]!!.first
        val quantity = list[0]!!.second
        return currentFluidType == resource && quantity <= currentAmount
    }

    override fun clear() {
        currentFluidType = null
        currentAmount = 0
        listeners.forEach { it.onContainerClear(this) }
    }

    override fun copy(): ResourceContainer {
        val ret = FluidTank(maxAmount, typeRule)
        ret.additionRule = additionRule
        ret.removalRule = removalRule
        ret.currentFluidType = currentFluidType
        ret.currentAmount = currentAmount
        return ret
    }

    override fun getQuantity(resource: ResourceType): Int {
        if (resource == currentFluidType)
            return currentAmount
        return 0
    }

    override fun resourceList() = if (currentFluidType == null || currentAmount == 0) ResourceList() else ResourceList(currentFluidType!! to currentAmount)

    override fun typeList() = if (currentFluidType == null || currentAmount == 0) emptySet() else setOf(currentFluidType!!)

}