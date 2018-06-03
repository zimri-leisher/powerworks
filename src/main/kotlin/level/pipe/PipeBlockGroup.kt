package level.pipe

import fluid.FluidType
import item.ItemType
import level.Level
import resource.*

var nextId = 0

class PipeBlockGroup {
    private val pipes = mutableListOf<PipeBlock>()
    private val storage = PipeBlockInternalStorage(this)
    val id = nextId++
    val nodes = ResourceNodeGroup("Pipe block group $id")

    val size: Int
        get() = pipes.size

    init {
        ALL.add(this)
    }

    fun createCorrespondingNodes(nodes: List<ResourceNode<FluidType>>) {
        val new = nodes.map { ResourceNode.createCorresponding(it, storage) }
        new.forEach { Level.add(it) }
        this.nodes.addAll(new)
    }

    /**
     * Removes all nodes that were given to the network because of this
     */
    private fun removeCorrespondingNodes(p: PipeBlock) {
        val r = nodes.filter { it.xTile != p.xTile && it.yTile != p.yTile }
        nodes.removeAll(r)
        r.forEach { Level.remove(it) }
    }

    fun merge(other: PipeBlockGroup) {
        other.pipes.forEach {
            if (it !in pipes) {
                pipes.add(it)
                it.group = this
            }
        }
        other.nodes.forEach {
            it as ResourceNode<FluidType>
            if (it !in nodes) {
                it.attachedContainer = storage
                nodes.add(it)
            }

        }
    }

    private fun update() {
        storage.update()
    }

    fun addPipe(p: PipeBlock) {
        pipes.add(p)
    }

    fun removePipe(p: PipeBlock) {
        pipes.remove(p)
    }

    class PipeBlockInternalStorage(val parent: PipeBlockGroup) : ResourceContainer<FluidType>(ResourceCategory.FLUID) {

        // watch out, if somebody called add or remove and had checkable to false they could insert any liquid into this network and it would become the current one
        // so, a possible dupe bug

        override val totalQuantity: Int
            get() = currentAmount

        var currentFluidType: FluidType? = null
        var currentAmount = 0
            set(value) {
                field = value
                if (value == 0)
                    currentFluidType = null
            }
        val maxAmount
            get() = parent.size * STORAGE_PER_PIPE

        fun update() {
            if (currentFluidType != null) {
                val output = parent.nodes.getOutputter(currentFluidType!!, currentAmount)
                output?.output(currentFluidType!!, currentAmount)
            }
        }

        override fun add(resource: ResourceType, quantity: Int, from: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
            if (checkIfAble)
                if (!canAdd(resource, quantity))
                    return false
            // doing this so nobody can really fuck this over and put in a different resourcecategory because of checkable
            // this isn't bad code i promise, it's for performance and it's easy to have not happen!
            resource as FluidType
            if (currentFluidType == null)
                currentFluidType = resource
            currentAmount += quantity
            return true
        }

        override fun spaceFor(resource: FluidType, quantity: Int) = currentFluidType == null || (resource == currentFluidType && currentAmount + quantity <= maxAmount)

        override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
            if (checkIfAble)
                if (!canRemove(resource, quantity))
                    return false
            resource as FluidType
            currentAmount -= quantity
            if (currentAmount == 0)
                currentFluidType = null
            return true
        }

        override fun contains(resource: FluidType, quantity: Int) = resource == currentFluidType && quantity <= currentAmount

        override fun clear() {
            currentAmount = 0
            currentFluidType = null
        }

        override fun copy(): ResourceContainer<FluidType> {
            val ret = PipeBlockInternalStorage(parent)
            ret.currentFluidType = currentFluidType
            ret.currentAmount = currentAmount
            return ret
        }

        override fun getQuantity(resource: ResourceType): Int {
            if (resource == currentFluidType)
                return currentAmount
            return 0
        }

        override fun toList(): ResourceList {
            if (currentFluidType == null || currentAmount == 0)
                return ResourceList()
            return ResourceList(currentFluidType!! to currentAmount)
        }

    }

    companion object {
        val STORAGE_PER_PIPE = 5

        val ALL = mutableListOf<PipeBlockGroup>()

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}