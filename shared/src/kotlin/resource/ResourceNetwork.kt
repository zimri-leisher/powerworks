package resource

import level.*
import network.LevelObjectReference
import network.ResourceNetworkReference
import serialization.Id
import serialization.Referencable
import kotlin.math.absoluteValue

abstract class ResourceNetwork<V : ResourceNetworkVertex<V>>(
    level: Level,
    @Id(30)
    val networkType: ResourceNetworkType
) :
    LevelObject(LevelObjectType.RESOURCE_NETWORK) {

    // make these resource containers? NOPE ??? or yepP?????

    // is this even useful abstraction?
    // right now there are pipe networks
    // conceivably could have "teleporter" networks
    // they would still have vertices
    // power networks?
    // yes, let's see if we can get this abstract enough to use as a power network
    // power network transmits power resource
    // producers will add some power to their own internal batteries every tick
    // but will this

    // shit. now our problem is, if this is all between resource nodes, how do we
    // have transmission zones for power?
    // i want the vertices of resource networks to be blocks too. i want to be able to go directly to a block.
    // nope. hmm.... give each block in a large block a resource node. default behavior accepts energy
    //
    // this should also be abstract enoguh for node-node connections

    @Id(31)
    val vertices = mutableSetOf<V>()

    @Id(32)
    val market = ResourceMarket(this)

    abstract val nodes: MutableList<ResourceNode>
    abstract val containers: MutableList<ResourceContainer>

    open fun add(obj: PhysicalLevelObject) {
        if (level == LevelManager.EMPTY_LEVEL) {
            if (!obj.level.add(this)) {
                throw Exception("Resource network was in the empty level and could not join ${obj.level}")
            }
        }
        if(obj !is PotentialResourceNetworkVertex) {
            throw Exception("Tried to add $obj to ResourceNetwork but it is not a LevelObject")
        }
        if (obj.level != level) {
            throw Exception("Tried to add a vertex in level ${obj.level} to ResourceNetwork in level $level")
        }
        if (!canBeVertex(obj)) {
            throw Exception("$obj cannot be a vertex for $this")
        }
        val vert = makeVertex(obj)
        vertices.add(vert)
        if (obj is ResourceNode) {
            nodes.add(obj)
            if (obj.container !in containers) {
                containers.add(obj.container)
            }
        }
        obj.onAddToNetwork(this)
        updateEdges(vert)
        tryMerge(vert)
    }

    open fun remove(obj: PhysicalLevelObject) {
        val vert = vertices.firstOrNull { it.obj == obj }
            ?: throw Exception("Tried to remove $obj which is not in network $this")
        updateEdges(vert)
        trySplit(vert)
        vertices.remove(vert)
        if (vert.obj is ResourceNode) {
            nodes.remove(vert.obj)
            if (nodes.none { it.container == vert.obj.container }) {
                containers.remove(vert.obj.container)
            }
        }
        (obj as PotentialResourceNetworkVertex).onRemoveFromNetwork(this)
        if (vertices.isEmpty()) {
            level.remove(this)
        }
    }

    override fun update() {
        val transactions = market.getTransactions()
        for (transaction in transactions) {
            // market transactions should always have a non null src and dest
            val connection = getBestConnection(transaction.src, transaction.dest)
            if (connection != null) {
                if (connection.canExecute(transaction)) {
                    connection.execute(transaction)
                }
            } else {
                // handle transaction that has no valid route
                // this might happen if resource container has space but no node attached to it can accept

            }
        }
    }

    abstract fun getConnection(from: ResourceNode, to: ResourceNode): ResourceNodeConnection?

    protected abstract fun updateEdges(vert: V)

    abstract fun canBeVertex(obj: PhysicalLevelObject): Boolean

    abstract fun makeVertex(obj: PhysicalLevelObject): V

    abstract fun getFlowInProgress(container: ResourceContainer): List<ResourceFlow>

    fun getNecessaryFlow(container: ResourceContainer, order: ResourceOrder): ResourceFlow {
        var currentAmount = container.getQuantity(order.stack.type)

        for (currentFlow in getFlowInProgress(container)) {
            if (currentFlow.stack.type != order.stack.type) {
                continue
            }
            if (currentFlow.direction == ResourceFlowDirection.IN) {
                currentAmount += currentFlow.stack.quantity
            } else {
                currentAmount -= currentFlow.stack.quantity
            }
        }

        val difference = currentAmount - order.stack.quantity
        if (order.type == ResourceOrderType.EXACTLY) {
            return ResourceFlow(
                stackOf(order.stack.type, difference.absoluteValue),
                if (difference < 0) ResourceFlowDirection.IN else ResourceFlowDirection.OUT
            )
        } else if (order.type == ResourceOrderType.NO_LESS_THAN) {
            return ResourceFlow(
                stackOf(order.stack.type, difference.coerceAtMost(0) * -1),
                ResourceFlowDirection.IN
            )
        } else {
            // no more than
            return ResourceFlow(
                stackOf(order.stack.type, difference.coerceAtLeast(0)),
                ResourceFlowDirection.OUT
            )
        }
    }

    fun getBestConnection(
        from: ResourceContainer,
        to: ResourceContainer
    ): ResourceNodeConnection? {
        val toNodes = to.nodes
        val fromNodes = from.nodes
        // find the best (by cost) pairing
        var minCost = Int.MAX_VALUE
        var bestConnection: ResourceNodeConnection? = null
        for (toNode in toNodes) {
            for (fromNode in fromNodes) {
                val connection = getConnection(fromNode, toNode)
                if (connection != null) {
                    if (connection.cost < minCost) {
                        minCost = connection.cost
                        bestConnection = connection
                    }
                }
            }
        }
        return bestConnection
    }

    protected abstract fun tryMerge(around: ResourceNetworkVertex<V>)

    fun mergeFrom(network: ResourceNetwork<V>) {
        for (vertex in network.vertices) {
            vertex.obj.onRemoveFromNetwork(network)
            vertex.obj.onAddToNetwork(this)
        }
        vertices.addAll(network.vertices)
        nodes.addAll(network.nodes)
        network.vertices.clear()
        network.nodes.clear()
    }

    protected abstract fun trySplit(around: V)

    protected fun splitOff(vertices: Collection<V>): ResourceNetwork<V> {
        val newNetwork = vertices.first().type.makeNew(level) as ResourceNetwork<V>
        newNetwork.vertices.addAll(vertices)
        for (vert in vertices) {
            vert.obj.onRemoveFromNetwork(this)
            vert.obj.onAddToNetwork(newNetwork)
            this.vertices.remove(vert)
            if (vert.obj is ResourceNode) {
                this.nodes.remove(vert.obj)
            }
        }
        return newNetwork
    }

    override fun toReference(): LevelObjectReference<out LevelObject> {
        return ResourceNetworkReference(this)
    }

    companion object {

        fun merge(networks: Set<ResourceNetwork<*>>) {
            if (networks.isEmpty()) {
                return
            }
            if (networks.map { it.networkType }.distinct().size != 1) {
                throw Exception("Tried to merge ResourceNetworks of different types: $networks")
            }
            val dest = networks.maxBy { it.vertices.size }
            for (other in networks - dest) {
                // do some not correct casts probably. there is no reason why these might actually be PNVs, but if I don't cast it to something it doesn't work
                (dest as ResourceNetwork<PipeNetworkVertex>).mergeFrom(other as ResourceNetwork<PipeNetworkVertex>)
            }
        }

        fun split(network: ResourceNetwork<*>, groups: Set<Set<ResourceNetworkVertex<*>>>) {
            val largestGroup = groups.withIndex().maxBy { (_, value) -> value.size }.index
            for ((i, newVertices) in groups.withIndex()) {
                if (i != largestGroup) {
                    // do some not correct casts probably. there is no reason why these might actually be PNVs, but if I don't cast it to something it doesn't work
                    (network as ResourceNetwork<PipeNetworkVertex>).splitOff(newVertices as Collection<PipeNetworkVertex>)
                }
            }
        }
    }
}