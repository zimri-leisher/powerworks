package routing

import data.WeakMutableList
import level.Level
import misc.Geometry
import resource.*

class InvalidFunctionCallException(message: String) : Exception(message)

open class ResourceRoutingNetwork(category: ResourceCategory) : ResourceContainer(category) {

    val id = nextId++

    init {
        ALL.add(this)
    }

    /**
     * The nodes this network touches
     */
    val attachedNodes = ResourceNodeGroup()

    /**
     * The nodes that correspond with the nodes this network touches
     */
    val internalNodes = ResourceNodeGroup()

    override val totalQuantity get() = attachedNodes.getAttachedContainers().sumBy { it.totalQuantity }

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
        for ((resource, quantity) in list) {
            val destinations = attachedNodes.getAttachedContainers()
            if (destinations.none { it.spaceFor(resource, quantity) }) {
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
        return ResourceRoutingNetwork(resourceCategory)
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

    open fun onAddResource(resource: ResourceType, quantity: Int, from: ResourceNode?) {}

    open fun onRemoveResource(resource: ResourceType, quantity: Int, to: ResourceNode?) {}

    fun attachNode(node: ResourceNode) {
        node.network = this
        attachedNodes.add(node)
        addCorrespondingNode(node)
    }

    fun disattachNode(node: ResourceNode) {
        node.network = ResourceRoutingNetwork(node.resourceCategory)
        attachedNodes.remove(node)
        removeCorrespondingNode(node)
    }

    fun addCorrespondingNode(node: ResourceNode) {
        val newNode = ResourceNode(
                node.xTile + Geometry.getXSign(node.dir),
                node.yTile + Geometry.getYSign(node.dir),
                Geometry.getOppositeAngle(node.dir),
                node.resourceCategory,
                this)
        newNode.behavior.allowOut.setStatement(RoutingLanguageStatement.TRUE)
        newNode.behavior.allowIn.setStatement(RoutingLanguageStatement.TRUE)
        newNode.network = this
        newNode.isInternalNetworkNode = true
        Level.add(newNode)
        internalNodes.add(newNode)
    }

    fun removeCorrespondingNode(node: ResourceNode) {
        val toRemove = internalNodes.filter {
            it.xTile == node.xTile + Geometry.getXSign(node.dir) &&
                    it.yTile == node.yTile + Geometry.getYSign(node.dir) &&
                    it.dir == Geometry.getOppositeAngle(node.dir) &&
                    it.resourceCategory == node.resourceCategory
        }
        toRemove.forEach { it.network = ResourceRoutingNetwork(it.resourceCategory); Level.remove(it); internalNodes.remove(it) }
    }

    /**
     * Sends some resources to a node in this network, if it is able to accept them and another node in this network is
     * able to output those resources. Prioritizes sending resources to a node that is forcing output
     * @param node the node in this network to send the resources to. It can either be an internal node (one created by the network)
     * or an attached node (one that corresponds to an actual block/machine/etc. and is attached to an internal node).
     */
    open fun sendTo(node: ResourceNode, type: ResourceType, quantity: Int) {
        val sendTo = if (node.isInternalNetworkNode) node.attachedNodes.first() else node
        val outputters = attachedNodes.getOutputters(type, quantity, {it != sendTo })
        val takeFrom = outputters.getForceOutputter(type, quantity)
                ?: outputters.firstOrNull()
        if (takeFrom != null) {
            if (sendTo.input(type, quantity)) {
                takeFrom.output(type, quantity, false)
            }
        }
    }

    /**
     * Takes resources from this node and sends it elsewhere in the network, if this node is able to give them up and
     * there is a node in the network which can accept them. Prioritizes sending the resources to a node that is forcing
     * input
     * @param node the node in this network to take them from. It can either be an internal node (one created by the network)
     * or an attached node (one that corresponds to an actual block/machine/etc. and is attached to an internal node).
     */
    open fun takeFrom(node: ResourceNode, type: ResourceType, quantity: Int) {
        val takeFrom = if(node.isInternalNetworkNode) node.attachedNodes.first() else node
        val inputters = attachedNodes.getInputters(type, quantity, {it != takeFrom })
        val sendTo = inputters.getForceInputter(type, quantity)
                ?: inputters.firstOrNull()
        if (sendTo != null) {
            if (takeFrom.attachedContainer.remove(type, quantity, sendTo)) {
                sendTo.attachedContainer.add(type, quantity, node, false)
            }
        }
    }

    open fun update() {}

    open fun render() {}

    override fun toString() = "Resource routing network of $resourceCategory with id $id"

    companion object {
        var nextId = 0

        val ALL = WeakMutableList<ResourceRoutingNetwork>()

        fun render() {
            ALL.forEach { it.render() }
        }

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}

