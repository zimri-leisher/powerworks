package level.pipe

import fluid.FluidType
import level.Level
import level.add
import level.remove
import resource.*
import routing.RoutingLanguageStatement

class PipeBlockGroup(val level: Level) {
    private val pipes = mutableListOf<PipeBlock>()
    private val storage = PipeBlockInternalStorage(this)
    val id = nextId++
    val nodes = mutableListOf<ResourceNode>()

    val size: Int
        get() = pipes.size

    init {
        ALL.add(this)
    }

    fun createCorrespondingNodes(nodes: Set<ResourceNode>) {
        val new = nodes.map {
            val behavior = ResourceNodeBehavior(it)
            behavior.allowOut.setStatement(RoutingLanguageStatement.TRUE, null)
            behavior.allowIn.setStatement(RoutingLanguageStatement.TRUE, null)
            ResourceNode.createCorresponding(it, storage, behavior)
        }
        for (newNode in new) {
            if (newNode !in this.nodes) {
                this.nodes.add(newNode)
                level.add(newNode)
            }
        }
    }

    /**
     * Removes all nodes that were given to the network because of this
     */
    fun removeCorrespondingNodes(p: PipeBlock) {
        val r = nodes.filter { it.xTile != p.xTile && it.yTile != p.yTile }
        nodes.removeAll(r)
        r.forEach { level.remove(it) }
    }

    fun merge(other: PipeBlockGroup) {
        other.pipes.forEach {
            if (it !in pipes) {
                pipes.add(it)
                it.group = this
            }
        }
        other.nodes.forEach {
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

    class PipeBlockInternalStorage(val parent: PipeBlockGroup) : ResourceContainer(ResourceCategory.FLUID) {
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

        override fun add(resource: ResourceType, quantity: Int, from: ResourceNode?, checkIfAble: Boolean): Boolean {
            if (checkIfAble)
                if (!canAdd(resource, quantity))
                    return false
            resource as FluidType
            if (currentFluidType == null)
                currentFluidType = resource
            currentAmount += quantity
            return true
        }

        override fun spaceFor(list: ResourceList): Boolean {
            for ((resource, quantity) in list) {
                if (!(currentFluidType == null || (resource == currentFluidType && currentAmount + quantity <= maxAmount))) {
                    return false
                }
            }
            return true
        }

        override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode?, checkIfAble: Boolean): Boolean {
            if (checkIfAble)
                if (!canRemove(resource, quantity))
                    return false
            resource as FluidType
            currentAmount -= quantity
            if (currentAmount == 0)
                currentFluidType = null
            return true
        }

        override fun contains(list: ResourceList): Boolean {
            for ((resource, quantity) in list) {
                if (!(resource == currentFluidType && quantity <= currentAmount)) {
                    return false
                }
            }
            return true
        }

        override fun expect(resource: ResourceType, quantity: Int): Boolean {
            return false
        }

        override fun cancelExpectation(resource: ResourceType, quantity: Int): Boolean {
            return false
        }

        override fun clear() {
            currentAmount = 0
            currentFluidType = null
        }

        override fun copy(): ResourceContainer {
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

        override fun resourceList(): ResourceList {
            if (currentFluidType == null || currentAmount == 0)
                return ResourceList()
            return ResourceList(currentFluidType!! to currentAmount)
        }

        override fun typeList() = if (currentFluidType == null || currentAmount == 0) emptySet() else setOf(currentFluidType!!)
    }

    companion object {

        private var nextId = 0

        val STORAGE_PER_PIPE = 5

        val ALL = mutableListOf<PipeBlockGroup>()

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}