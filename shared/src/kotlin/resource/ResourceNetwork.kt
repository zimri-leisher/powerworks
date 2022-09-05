package resource

import data.WeakMutableList
import level.Level

var ResourceNetwork_nextId = 0

abstract class ResourceNetwork(val level: Level) {

    val id = ResourceNetwork_nextId++
    val market = ResourceMarket(this)
    abstract val nodes: List<ResourceNode2>

    init {
        ALL.add(this)
    }

    abstract fun getConnection(from: ResourceNode2, to: ResourceNode2): ResourceNodeConnection?

    open fun render() {}

    open fun update() {
        val transactions = market.getTransactions()
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