package routing

import data.ConcurrentlyModifiableWeakMutableList
import level.Level
import level.LevelManager
import level.pipe.PipeBlock
import misc.Geometry
import resource.*
import routing.script.RoutingLanguage
import serialization.Id
import java.lang.Integer.min
import java.util.*

class InvalidFunctionCallException(message: String) : Exception(message)

open class ResourceRoutingNetwork(category: ResourceCategory,
                                  @Id(9)
                                  var level: Level) : ResourceContainer(category) {

    private constructor() : this(ResourceCategory.ITEM, LevelManager.EMPTY_LEVEL)

    override val expected get() = throw InvalidFunctionCallException("Routing networks cannot expect resources")

    /**
     * The nodes this network touches
     */
    @Id(7)
    val attachedNodes = mutableListOf<ResourceNode>()

    /**
     * The nodes that correspond with the nodes this network touches
     */
    @Id(8)
    val internalNodes = mutableListOf<ResourceNode>()

    @Id(10)
    val containersSentTo = mutableSetOf<ResourceContainer>()

    init {
        ALL.add(this)
    }

    override val totalQuantity get() = attachedNodes.getAttachedContainers().sumBy { it.totalQuantity }

    open fun mergeIntoThis(other: ResourceRoutingNetwork) {
        other.attachedNodes.forEach {
            it.network = this
            if (it !in attachedNodes) {
                attachedNodes.add(it)
            }
        }
        other.internalNodes.forEach {
            it.network = this
            it.attachedContainer = this
            if (it !in internalNodes) {
                internalNodes.add(it)
            }
        }
    }

    override fun add(resources: ResourceList, from: ResourceNode?, checkIfAble: Boolean): Boolean {
        if (checkIfAble) {
            if (!canAddAll(resources)) {
                return false
            }
        }
        onAddResources(resources, from)
        return true
    }

    override fun remove(resources: ResourceList, to: ResourceNode?, checkIfAble: Boolean): Boolean {
        println("checking if can remove $resources")
        if (checkIfAble) {
            if (!canRemoveAll(resources)) {
                return false
            }
        }
        onRemoveResource(resources, to)
        return true
    }

    override fun expect(resources: ResourceList) = throw InvalidFunctionCallException("Routing networks cannot expect resources")

    override fun cancelExpectation(resources: ResourceList) = true

    override fun clear() {
        attachedNodes.getAttachedContainers().forEach { it.clear() }
    }

    override fun mostPossibleToAdd(list: ResourceList): ResourceList {
        // the most possible to add is dictated by how much can flow through each node to each container
        // if two nodes go to the same container, their max possible add is potentially mutually exclusive
        // so we just check both of them and go with whichever is bigger
        val possibleToContainer = mutableMapOf<ResourceContainer, ResourceList>()
        for (attachedNode in attachedNodes) {
            val mostPossibleThroughThisNode = attachedNode.mostPossibleToInput(list)
            val alreadyPossibleThroughOtherNode = possibleToContainer[attachedNode.attachedContainer]
            if (alreadyPossibleThroughOtherNode != null) {
                // if a possible addition to this container has already been found
                if (alreadyPossibleThroughOtherNode.totalQuantity < mostPossibleThroughThisNode.totalQuantity) {
                    // keep the one with higher quantity
                    possibleToContainer[attachedNode.attachedContainer] = mostPossibleThroughThisNode
                }
            } else {
                possibleToContainer[attachedNode.attachedContainer] = mostPossibleThroughThisNode
            }
        }
        val possibleOverall = mutableResourceListOf()
        // now that we've found the max possible flow to each resource ocntainer, we need to figure out which ones to
        // include and which ones to ignore
        val possibleIter = possibleToContainer.iterator()
        while (possibleOverall.totalQuantity < list.totalQuantity && possibleIter.hasNext()) {
            val (_, resources) = possibleIter.next()
            possibleOverall.putAll(resources)
        }
        return possibleOverall
    }

    override fun mostPossibleToRemove(list: ResourceList): ResourceList {
        // just like above
        val possibleToContainer = mutableMapOf<ResourceContainer, ResourceList>()
        for (attachedNode in attachedNodes) {
            val mostPossibleThroughThisNode = attachedNode.mostPossibleToOutput(list)
            val alreadyPossibleThroughOtherNode = possibleToContainer[attachedNode.attachedContainer]
            if (alreadyPossibleThroughOtherNode != null) {
                if (alreadyPossibleThroughOtherNode.totalQuantity < mostPossibleThroughThisNode.totalQuantity) {
                    possibleToContainer[attachedNode.attachedContainer] = mostPossibleThroughThisNode
                }
            } else {
                possibleToContainer[attachedNode.attachedContainer] = mostPossibleThroughThisNode
            }
        }
        val possibleOverall = mutableResourceListOf()
        val possibleIter = possibleToContainer.iterator()
        while (possibleOverall.totalQuantity < list.totalQuantity && possibleIter.hasNext()) {
            val (_, resources) = possibleIter.next()
            possibleOverall.putAll(resources)
        }
        return possibleOverall
    }

    override fun copy(): ResourceContainer {
        return ResourceRoutingNetwork(resourceCategory, level)
    }

    override fun toResourceList(): ResourceList {
        val all = attachedNodes.getAttachedContainers().toList().map { it.toResourceList() }
        val final = mutableResourceListOf()
        all.forEach { final.putAll(it) }
        return final
    }

    override fun getQuantity(resource: ResourceType) = attachedNodes.getAttachedContainers().sumBy { it.getQuantity(resource) }

    open fun onAddResources(resources: ResourceList, from: ResourceNode?) {
        if (from == null) throw java.lang.IllegalArgumentException("Resource networks cannot accept resources from non-ResourceNode sources. What a tounge-twister.")
        for ((resource, quantity) in resources) {
            val destinations = findDestinationsFor(resource, quantity, { it.attachedNode != from && it != from })
            for ((destination, specificQuantity) in destinations) {
                transferResources(resource, specificQuantity, from, destination)
            }
        }
    }

    open fun onRemoveResource(resources: ResourceList, to: ResourceNode?) {
        println("removing resourcers to $to $resources")
        if (to == null) throw java.lang.IllegalArgumentException("Resource networks cannot send resources to non-ResourceNode sources. What a tounge-twister.")
        for ((resource, quantity) in resources) {
            val sources = findSourcesFor(resource, quantity, { it.attachedNode != to && it != to })
            for ((source, specificQuantity) in sources) {
                source.attachedNode!!.attachedContainer.remove(resource, specificQuantity, to, false)
                transferResources(resource, specificQuantity, source, to)
            }
        }
    }

    /**
     * @return an internal node that is attached to a node able to receive the given resources, with preference for nodes
     * which are trying to force input, which passes the [onlyTo] predicate
     */
    protected open fun findDestinationFor(type: ResourceType, quantity: Int, onlyTo: (ResourceNode) -> Boolean = { true }): ResourceNode? {
        val possibleSendToAttached = attachedNodes.filter(onlyTo).getInputters(type, quantity, accountForExpected = true)
        val possibleForceInputter = possibleSendToAttached.getForceInputter(type, quantity, accountForExpected = true)
        return (possibleForceInputter
                ?: possibleSendToAttached.minBy { if (it.attachedContainer in containersSentTo) 1 else 0 })?.attachedNode
    }

    open fun findDestinationsFor(type: ResourceType, quantity: Int, onlyTo: (ResourceNode) -> Boolean = { true }): Map<ResourceNode, Int> {
        val actualQuantity = if (quantity != -1) quantity else Int.MAX_VALUE

        val possibleSendTo = attachedNodes.filter(onlyTo).getPartialInputters(resourceListOf(type to quantity))

        val actualSendTo = mutableMapOf<ResourceNode, Int>()
        // if the quantity is -1, we want to fill this node with as much possible
        // TODO maybe we could try to balance which resources get sent where
        var remainingQuantity = actualQuantity
        for ((from, resources) in possibleSendTo) {
            if (from.behavior.forceIn.check(type)) {
                val quantityToSend = min(resources[0].value, remainingQuantity)
                actualSendTo[from] = quantityToSend
                remainingQuantity -= quantityToSend
                if (remainingQuantity <= 0) {
                    return actualSendTo.mapKeys { it.key.attachedNode!! }
                }
            }
        }
        for ((from, resources) in possibleSendTo
                .entries.sortedBy { it.key.attachedContainer.totalQuantity }) {
            val quantityToTake = min(resources[0].value, remainingQuantity)
            actualSendTo[from] = quantityToTake
            remainingQuantity -= quantityToTake
            if (remainingQuantity <= 0) {
                return actualSendTo.mapKeys { it.key.attachedNode!! }
            }
        }
        return actualSendTo.mapKeys { it.key.attachedNode!! }
    }

    /**
     * @return an internal node that is attached to a node able to send the given resources, with preference for nodes
     * which are trying to force output, which passes the [onlyFrom] predicate
     */
    protected open fun findSourceFor(type: ResourceType, quantity: Int, onlyFrom: (ResourceNode) -> Boolean = { true }): ResourceNode? {
        val possibleTakeFromAttached = attachedNodes.filter(onlyFrom).getOutputters(type, quantity)
        val takeFromAttached = possibleTakeFromAttached.getForceOutputter(type, quantity)
                ?: possibleTakeFromAttached.firstOrNull()
        return takeFromAttached?.attachedNode
    }

    protected open fun findSourcesFor(type: ResourceType, quantity: Int, onlyFrom: (ResourceNode) -> Boolean = { true }): Map<ResourceNode, Int> {
        val actualQuantity = if (quantity != -1) quantity else Int.MAX_VALUE

        val possibleTakeFrom = attachedNodes.filter(onlyFrom).getPartialOutputters(resourceListOf(type to actualQuantity))

        val actualTakeFrom = mutableMapOf<ResourceNode, Int>()
        // if the quantity is -1, we want to fill this node with as much possible
        // TODO maybe we could try to balance which resources get sent where
        var remainingQuantity = actualQuantity
        for ((from, resources) in possibleTakeFrom) {
            if (from.behavior.forceOut.check(type)) {
                val quantityToTake = min(resources[0].value, remainingQuantity)
                actualTakeFrom[from] = quantityToTake
                remainingQuantity -= quantityToTake
                if (remainingQuantity <= 0) {
                    return actualTakeFrom.mapKeys { it.key.attachedNode!! }
                }
            }
        }
        for ((from, resources) in possibleTakeFrom.entries.sortedByDescending { it.key.attachedContainer.totalQuantity }) {
            val quantityToTake = min(resources[0].value, remainingQuantity)
            actualTakeFrom[from] = quantityToTake
            remainingQuantity -= quantityToTake
            if (remainingQuantity <= 0) {
                return actualTakeFrom.mapKeys { it.key.attachedNode!! }
            }
        }
        return actualTakeFrom.mapKeys { it.key.attachedNode!! }
    }

    fun attachNode(node: ResourceNode, fromBlock: PipeBlock) {
        node.network = this
        attachedNodes.add(node)
        addCorrespondingInternalNode(node, fromBlock.id)
    }

    fun disattachNode(node: ResourceNode) {
        node.network = ResourceRoutingNetwork(node.resourceCategory, level)
        attachedNodes.remove(node)
        removeCorrespondingInternalNode(node)
    }

    fun addCorrespondingInternalNode(node: ResourceNode, seedId: UUID) {
        val newNode = ResourceNode(
                node.xTile + Geometry.getXSign(node.dir),
                node.yTile + Geometry.getYSign(node.dir),
                Geometry.getOppositeAngle(node.dir),
                node.resourceCategory,
                this, level)
        val random = Random(seedId.mostSignificantBits)
        val byteArray = ByteArray(36)
        random.nextBytes(byteArray)
        val nodeId = UUID.nameUUIDFromBytes(byteArray)
        newNode.behavior.allowOut.setStatement(RoutingLanguage.TRUE)
        newNode.behavior.allowIn.setStatement(RoutingLanguage.TRUE)
        newNode.network = this
        newNode.isInternalNetworkNode = true
        newNode.id = nodeId
        level.add(newNode)
        internalNodes.add(newNode)
    }

    fun removeCorrespondingInternalNode(node: ResourceNode) {
        val toRemove = internalNodes.filter {
            it.xTile == node.xTile + Geometry.getXSign(node.dir) &&
                    it.yTile == node.yTile + Geometry.getYSign(node.dir) &&
                    it.dir == Geometry.getOppositeAngle(node.dir) &&
                    it.resourceCategory == node.resourceCategory
        }
        toRemove.forEach { it.network = ResourceRoutingNetwork(it.resourceCategory, level); level.remove(it); internalNodes.remove(it) }
    }

    /**
     * Send resources of the given [type] and [quantity] from the internal [from] node to the internal [to] node. This method
     * should only add resources, not take them away, as removal will be handled by the caller of this method
     * @return true if the resources were able to be sent
     */
    protected open fun transferResources(type: ResourceType, quantity: Int, from: ResourceNode, to: ResourceNode): Boolean {
        val success = to.input(type, quantity)
        if (success) {
            containersSentTo.add(to.attachedNode!!.attachedContainer)
            if (containersSentTo.size == attachedNodes.filter { it.behavior.allowIn.possible() != null }.map { it.attachedContainer }.distinct().size) {
                // if we've sent to all internal containers
                containersSentTo.clear()
            }
        }
        return success
    }

    open fun update() {}

    open fun render() {}

    override fun toString() = "Resource routing network of $resourceCategory with id $id"

    companion object {

        val ALL = ConcurrentlyModifiableWeakMutableList<ResourceRoutingNetwork>()

        fun render() {
            ALL.forEach { it.render() }
        }

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}

