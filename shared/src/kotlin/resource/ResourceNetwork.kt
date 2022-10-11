package resource

import level.Level
import level.LevelObject
import level.LevelObjectType
import level.PhysicalLevelObject

abstract class ResourceNetwork<V : ResourceNetworkVertex<V>>(level: Level, val networkType: ResourceNetworkType) :
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

    val vertices = mutableSetOf<V>()
    val market = ResourceMarket(this)

    abstract val nodes: MutableList<ResourceNode>

    init {
        level.add(this)
    }

    abstract fun getConnection(from: ResourceNode, to: ResourceNode): ResourceNodeConnection?

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

    protected abstract fun updateEdges(vert: V)
    abstract fun canBeVertex(obj: PhysicalLevelObject): Boolean
    abstract fun makeVertex(obj: PhysicalLevelObject): V

    open fun add(obj: PhysicalLevelObject) {
        if (obj.level != level) {
            throw Exception("Tried to add a vertex in level ${obj.level} to ResourceNetwork in level $level")
        }
        if (!canBeVertex(obj)) {
            throw Exception("$obj cannot be a vertex for $this")
        }
        val vert = makeVertex(obj)
        vertices.add(vert)
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
        }
    }

    override fun update() {
        val transactions = market.getTransactions()
        for (transaction in transactions) {
            // market transactions should always have a non null src and dest
            val connection = getConnection(transaction.src, transaction.dest)
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

    private fun tryMerge(around: ResourceNetworkVertex<V>) {
        val toCombine = mutableSetOf(this)
        for (dir in 0..3) {
            val edgesNetwork = around.edges[dir]?.obj?.getNetwork(networkType)
            if (edgesNetwork != null && edgesNetwork != this) {
                toCombine.add(edgesNetwork as ResourceNetwork<V>)
            }
        }
        merge(toCombine)
    }

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

    protected abstract fun splitOff(vertices: Collection<V>): ResourceNetwork<V>

    companion object {

        private fun merge(networks: Set<ResourceNetwork<*>>) {
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