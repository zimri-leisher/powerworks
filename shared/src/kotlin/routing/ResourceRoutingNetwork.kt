package routing

import data.ConcurrentlyModifiableWeakMutableList
import data.WeakMutableList
import level.Level
import level.LevelManager
import misc.Geometry
import resource.*
import serialization.Id

class InvalidFunctionCallException(message: String) : Exception(message)

open class ResourceRoutingNetwork(category: ResourceCategory,
                                  @Id(9)
                                  var level: Level) : ResourceContainer(category) {

    private constructor() : this(ResourceCategory.ITEM, LevelManager.EMPTY_LEVEL)

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

    override fun add(resource: ResourceType, quantity: Int, from: ResourceNode?, checkIfAble: Boolean): Boolean {
        if (checkIfAble) {
            if (!canAdd(resource, quantity)) {
                return false
            }
        }
        onAddResource(resource, quantity, from)
        return true
    }

    override fun spaceFor(list: ResourceList): Boolean {
        for ((type, quantity) in list) {
            if (findDestinationFor(type, quantity) == null) {
                return false
            }
        }
        return true
    }

    override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode?, checkIfAble: Boolean): Boolean {
        if (checkIfAble) {
            if (!canRemove(resource, quantity)) {
                return false
            }
        }
        onRemoveResource(resource, quantity, to)
        return true
    }

    override fun expect(resource: ResourceType, quantity: Int) = throw InvalidFunctionCallException("Routing networks cannot expect resources")

    override fun cancelExpectation(resource: ResourceType, quantity: Int) = true

    override fun clear() {
        attachedNodes.getAttachedContainers().forEach { it.clear() }
    }

    override fun contains(list: ResourceList): Boolean {
        for ((resource, quantity) in list) {
            if (!contains(resource, quantity)) {
                return false
            }
        }
        return true
    }

    override fun copy(): ResourceContainer {
        return ResourceRoutingNetwork(resourceCategory, level)
    }

    override fun resourceList(): ResourceList {
        val all = attachedNodes.getAttachedContainers().toList().map { it.resourceList() }
        val final = ResourceList()
        all.forEach { final.addAll(it) }
        return final
    }

    override fun typeList() = attachedNodes.getAttachedContainers().flatMap { it.typeList() }.toSet()

    override fun contains(resource: ResourceType, quantity: Int) = attachedNodes.getAttachedContainers().sumBy { it.getQuantity(resource) } >= quantity

    override fun getQuantity(resource: ResourceType) = attachedNodes.getAttachedContainers().sumBy { it.getQuantity(resource) }

    open fun onAddResource(type: ResourceType, quantity: Int, from: ResourceNode?) {
        if (from == null) throw java.lang.IllegalArgumentException("Resource networks cannot accept resources from non-ResourceNode sources. What a tounge-twister.")

        val destination = findDestinationFor(type, quantity, { it.attachedNode != from })
        // there must be a destination because otherwise nothing would be able to be added
        destination!!
        transferResources(type, quantity, from, destination)
    }

    open fun onRemoveResource(type: ResourceType, quantity: Int, to: ResourceNode?) {}

    /**
     * @return an internal node that is attached to a node able to receive the given resources, with preference for nodes
     * which are trying to force input, which passes the [onlyTo] predicate
     */
    protected open fun findDestinationFor(type: ResourceType, quantity: Int, onlyTo: (ResourceNode) -> Boolean = { true }): ResourceNode? {
        val possibleSendToAttached = attachedNodes.getInputters(type, quantity, onlyTo)
        val sendToAttached = possibleSendToAttached.getForceInputter(type, quantity)
                ?: possibleSendToAttached.firstOrNull()
        return sendToAttached?.attachedNode
    }

    /**
     * @return an internal node that is attached to a node able to send the given resources, with preference for nodes
     * which are trying to force output, which passes the [onlyFrom] predicate
     */
    protected open fun findSourceFor(type: ResourceType, quantity: Int, onlyFrom: (ResourceNode) -> Boolean = { true }): ResourceNode? {
        val possibleTakeFromAttached = attachedNodes.getOutputters(type, quantity, onlyFrom)
        val takeFromAttached = possibleTakeFromAttached.getForceOutputter(type, quantity)
                ?: possibleTakeFromAttached.firstOrNull()
        return takeFromAttached?.attachedNode
    }

    fun attachNode(node: ResourceNode) {
        node.network = this
        attachedNodes.add(node)
        addCorrespondingInternalNode(node)
    }

    fun disattachNode(node: ResourceNode) {
        node.network = ResourceRoutingNetwork(node.resourceCategory, level)
        attachedNodes.remove(node)
        removeCorrespondingInternalNode(node)
    }

    fun addCorrespondingInternalNode(node: ResourceNode) {
        val newNode = ResourceNode(
                node.xTile + Geometry.getXSign(node.dir),
                node.yTile + Geometry.getYSign(node.dir),
                Geometry.getOppositeAngle(node.dir),
                node.resourceCategory,
                this, level)
        newNode.behavior.allowOut.setStatement(RoutingLanguageStatement.TRUE)
        newNode.behavior.allowIn.setStatement(RoutingLanguageStatement.TRUE)
        newNode.network = this
        newNode.isInternalNetworkNode = true
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
        return to.input(type, quantity)
    }

    /**
     * Sends some resources to a node in this network, if it is able to accept them and another node in this network is
     * able to output those resources. Prioritizes sending resources to a node that is forcing output
     * @param node the node in this network to send the resources to. It can either be an internal node (one created by the network)
     * or an attached node (one that corresponds to an actual block/machine/etc. and is attached to an internal node)
     */
    open fun forceSendTo(node: ResourceNode, type: ResourceType, quantity: Int) {
        val sendTo = if (!node.isInternalNetworkNode) node.attachedNode!! else node
        val takeFrom = findSourceFor(type, quantity, { it.attachedNode != sendTo })
        if (takeFrom != null) {
            if (transferResources(type, quantity, takeFrom, sendTo)) {
                takeFrom.attachedNode!!.attachedContainer.remove(type, quantity, sendTo)
            }
        }
    }

    /**
     * Takes resources from this node and sends it elsewhere in the network, if this node is able to give them up and
     * there is a node in the network which can accept them. Prioritizes sending the resources to a node that is forcing
     * input.
     * @param node the node in this network to take them from. It can either be an internal node (one created by the network)
     * or an attached node (one that corresponds to an actual block/machine/etc. and is attached to an internal node)
     */
    open fun forceTakeFrom(node: ResourceNode, type: ResourceType, quantity: Int) {
        val takeFrom = if (node.isInternalNetworkNode) node.attachedNode!! else node
        takeFrom.output(type, quantity)
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

