package resource

import data.WeakMutableList
import level.Level

var ResourceNetwork_nextId = 0

abstract class ResourceNetwork(val level: Level) {
    // is this even useful abstraction?
    // right now there are pipe networks
    // conceivably could have "teleporter" networks
    // they would still have vertices
    // power networks?
    // yes, let's see if we can get this abstract enough to use as a power network
    // power network transmits power resource
    // producers will add some power to their own internal batteries every tick
    // but will this

    val id = ResourceNetwork_nextId++
    val market = ResourceMarket(this)
    abstract val nodes: List<ResourceNode2>

    init {
        ALL.add(this)
    }

    abstract fun getConnection(from: ResourceContainer, to: ResourceContainer): ResourceNodeConnection?

    open fun render() {}

    open fun update() {
        val transactions = market.getTransactions()
        for(transaction in transactions) {
            // market transactions should always have a non null src and dest
            val connection = getConnection(transaction.src, transaction.dest)
            if(connection != null) {
                if(connection.canExecute(transaction)) {
                    connection.execute(transaction)
                }
            } else {
                // handle transaction that has no valid route
                // this might happen if resource container has space but no node attached to it can accept

            }
        }
    }

    companion object {
        val ALL = WeakMutableList<ResourceNetwork>()

        fun render() {
            ALL.forEach { it.render() }
        }

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}