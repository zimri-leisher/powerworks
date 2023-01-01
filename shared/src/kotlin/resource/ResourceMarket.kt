package resource

import item.ItemType
import level.LevelManager
import serialization.Id
import java.lang.Integer.min
import kotlin.math.ceil

// resource container is connected to network
// transactions between nodes
//              between inventories
//              between resource generators (ore tiles) and inventories
// node to node is just inventory to inventory?
// onFinishWork() -> ResourceTransaction(null, internalInv, <miner resources>)
// do we want to just call start/finish raw?
// probably not, there should be some middleman
// obviously, a transactionexecutor
// what function does it provide?


enum class ResourceOrderPriority {
    DEMAND, REQUEST
}

enum class ResourceFlowDirection {
    IN, OUT
}

enum class ResourceOrderType {
    NO_MORE_THAN, NO_LESS_THAN, EXACTLY
}

data class ResourceOrder(
    @Id(1)
    val stack: ResourceStack,
    @Id(2)
    val type: ResourceOrderType = ResourceOrderType.NO_MORE_THAN,
    @Id(3)
    val priority: ResourceOrderPriority
) {
    private constructor() : this(stackOf(ItemType.ERROR, 1), ResourceOrderType.EXACTLY, ResourceOrderPriority.REQUEST)
}

data class ResourceFlow(
    @Id(1)
    val stack: ResourceStack,
    @Id(2)
    val direction: ResourceFlowDirection
) {
    private constructor() : this(stackOf(ItemType.ERROR, 1), ResourceFlowDirection.IN)
}

sealed class ResourceDistributor(
    @Id(1)
    val network: ResourceNetwork<*>
) {

    abstract fun distribute(
        quantity: Int,
        inputs: Map<ResourceContainer, Int>,
    ): Map<ResourceContainer, Int>

}

class EqualizeContainers(network: ResourceNetwork<*>) : ResourceDistributor(network) {

    private constructor() : this(PipeNetwork())

    @Id(2)
    val lastTimeSentTo = mutableMapOf<ResourceContainer, Int>()

    override fun distribute(
        quantity: Int,
        inputs: Map<ResourceContainer, Int>,
    ): Map<ResourceContainer, Int> {
        val results = inputs.keys.associateWith { 0 }.toMutableMap()
        val remainingDemands = inputs.toMutableMap()
        val orderedContainers = inputs.keys.sortedBy { lastTimeSentTo[it] ?: 0 }
        var remainingQuantity = quantity
        var remainingDemand = remainingDemands.values.sum()
        outer@ while (remainingDemand > 0 && remainingQuantity > 0) {
            val amountToDistribute = ceil(remainingQuantity.toDouble() / remainingDemands.size.toDouble()).toInt()
            for (container in orderedContainers) {
                val demand = remainingDemands[container]!!
                val allotment = min(demand, amountToDistribute)
                remainingQuantity -= allotment
                remainingDemand -= allotment
                if (demand - allotment == 0) {
                    remainingDemands.remove(container)
                } else {
                    remainingDemands[container] = demand - allotment
                }
                results[container] = results[container]!! + allotment
                if (remainingQuantity == 0 || remainingDemand == 0) {
                    break@outer
                }
            }
        }
        for (container in results.keys) {
            lastTimeSentTo[container] = network.level.updatesCount
        }
        return results
    }
}

class ResourceMarket(
    @Id(1)
    val network: ResourceNetwork<*>
) {

    private constructor() : this(PipeNetwork())

    @Id(2)
    val distributor: ResourceDistributor = EqualizeContainers(network)

    fun getTransactions(): List<ResourceTransaction> {
        // so we're going to do this naively
        // given this situation:
        // producer produces 25 ore and 25 ingots
        // consumer requests 25 ore, 25 ingots, but says only request in while i have less than 25 quantity
        // what should happen here? maybe in an ideal future it would realize that these two types of resources now
        // depend on each other, but i don't think i will have this happen.

        val orders: Map<ResourceContainer, List<Pair<ResourceFlow, ResourceOrderPriority>>> =
            network.containers.associateWith { it.orders }.mapValues { (container, orders) ->
                orders.map {
                    container.maxFlow(network.getNecessaryFlow(container, it)) to it.priority
                }
            }.toMutableMap()

        val flows: Map<ResourceFlowDirection, Map<ResourceOrderPriority, Map<ResourceContainer, MutableResourceList>>> =
            ResourceFlowDirection.values().associateWith { dir ->
                ResourceOrderPriority.values().associateWith { priority ->
                    orders.mapValues { (_, flowsWithPriorities) ->
                        flowsWithPriorities.filter { it.first.direction == dir && it.second == priority }
                            .map { it.first.stack }.toMutableResourceList()
                    }
                }
            }

        val transactions = mutableListOf<ResourceTransaction>()
        transactions.addAll(
            distributeByPriority(flows, ResourceOrderPriority.DEMAND, ResourceOrderPriority.DEMAND)
        )
        transactions.addAll(
            distributeByPriority(flows, ResourceOrderPriority.DEMAND, ResourceOrderPriority.REQUEST)
        )
        return transactions
    }

    private fun distributeByPriority(
        flows: Map<ResourceFlowDirection, Map<ResourceOrderPriority, Map<ResourceContainer, MutableResourceList>>>,
        inPriority: ResourceOrderPriority,
        outPriority: ResourceOrderPriority
    ): List<ResourceTransaction> {
        val input = flows[ResourceFlowDirection.IN]!![inPriority]!!
        val output = flows[ResourceFlowDirection.OUT]!![outPriority]!!
        val types = output.values.flatMap { it.keys }
            .distinct() + input.values.flatMap { it.keys }.distinct()
        val transactions = mutableListOf<ResourceTransaction>()
        for (type in types) {
            transactions.addAll(distributeByType(type, input, output))
        }
        return transactions
    }

    private fun distributeByType(
        type: ResourceType,
        input: Map<ResourceContainer, MutableResourceList>,
        output: Map<ResourceContainer, MutableResourceList>
    ): List<ResourceTransaction> {
        val transactions = mutableListOf<ResourceTransaction>()
        for ((outContainer, outFlows) in output) {
            val quantity = outFlows[type]
            val demands = input.mapValues { (_, stacks) -> stacks[type] }
            val distribution = distributor.distribute(quantity, demands)
            for ((inContainer, allotment) in distribution) {
                transactions.add(ResourceTransaction(outContainer, inContainer, stackOf(type, allotment)))
                outFlows[type] -= allotment
                input[inContainer]!![type] -= allotment
            }
        }
        return transactions
    }
}