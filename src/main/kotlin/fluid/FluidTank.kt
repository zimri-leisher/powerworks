package fluid

import resource.*

class FluidTank(val maxAmount: Int, typeRule: ResourceContainer<FluidType>.(ResourceType) -> Boolean = { true }) : ResourceContainer<FluidType>(ResourceCategory.FLUID, typeRule) {

    var currentFluidType: FluidType? = null
    var currentAmount = 0
        set(value) {
            field = value
            if (value == 0)
                currentFluidType = null
        }
    override val totalQuantity: Int
        get() = currentAmount

    override fun add(resource: ResourceType, quantity: Int, from: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
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

    override fun spaceFor(resource: FluidType, quantity: Int) = currentFluidType == null || (resource == currentFluidType && currentAmount + quantity <= maxAmount)

    override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canRemove(resource, quantity))
                return false
        currentAmount -= quantity
        if (currentAmount == 0)
            currentFluidType = null
        listeners.forEach { it.onContainerChange(this, resource, -quantity) }
        return true
    }

    override fun contains(resource: FluidType, quantity: Int) = currentFluidType == resource && quantity <= currentAmount

    override fun clear() {
        currentFluidType = null
        currentAmount = 0
        listeners.forEach { it.onContainerClear(this) }
    }

    override fun copy(): ResourceContainer<FluidType> {
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

    override fun toList() = if (currentFluidType == null || currentAmount == 0) ResourceList() else ResourceList(currentFluidType!! to currentAmount)

}