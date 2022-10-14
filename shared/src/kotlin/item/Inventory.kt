package item

import resource.*
import serialization.Id
import java.lang.Integer.min
import java.lang.Math.ceil

class Inventory(
    @Id(11)
    val width: Int,
    @Id(7)
    val height: Int
) : ResourceContainer() {

    private constructor() : this(0, 0)

    val maxStackCount get() = width * height

    @Id(151)
    val resources = mutableResourceListOf()

    @Id(10)
    override var totalQuantity = 0
        private set

    val stackCount: Int
        get() {
            var count = 0
            for ((type, quantity) in resources) {
                count += ceil(quantity.toDouble() / (type as ItemType).maxStack.toDouble()).toInt()
            }
            return count
        }

    override fun add(resources: ResourceList): Boolean {
        if (!canAdd(resources))
            return false

        this.resources.putAll(resources)
        listeners.forEach { it.onAddToContainer(this, resources) }
        return true
    }

    override fun mostPossibleToAdd(list: ResourceList): ResourceList {
        val possible = mutableResourceListOf()
        var remainingStacks = maxStackCount - stackCount
        for ((type, quantity) in list.sortedByDescending { it.quantity }) {
            type as ItemType
            val extraSpace = resources[type] % type.maxStack
            val requiredStacks = ceil((quantity - extraSpace).toDouble() / type.maxStack.toDouble()).toInt()
            val stacksAbleToAdd = min(requiredStacks, remainingStacks)
            remainingStacks -= stacksAbleToAdd
            possible[type] = min(extraSpace + stacksAbleToAdd * type.maxStack, quantity)
        }
        return possible
    }

    override fun remove(resources: ResourceList): Boolean {
        if (!canRemove(resources))
            return false
        this.resources.takeAll(resources)
        listeners.forEach { it.onRemoveFromContainer(this, resources) }
        return true
    }

    override fun mostPossibleToRemove(list: ResourceList): ResourceList {
        return resources.intersection(list)
    }

    override fun getQuantity(type: ResourceType) = resources[type]

    override fun toResourceList() = resources

    override fun clear() {
        resources.clear()
        listeners.forEach { it.onContainerClear(this) }
    }

    override fun copy(): Inventory {
        val inv = Inventory(width, height)
        inv.resources.putAll(resources)
        return inv
    }

    operator fun iterator() = resources.iterator()

    operator fun get(i: Int): ResourceStack? {
        var currentStack = 0
        for ((type, quantity) in resources) {
            type as ItemType
            val stacks = ceil(quantity.toDouble() / type.maxStack.toDouble()).toInt()
            if (currentStack + stacks > i + 1) {
                return stackOf(type, type.maxStack)
            } else if (currentStack + stacks == i + 1) {
                return stackOf(type, quantity % type.maxStack)
            }
            currentStack += stacks
        }
        return null
    }

    override fun toString() = "Inventory width: $width, height: $height, $totalQuantity items"
}