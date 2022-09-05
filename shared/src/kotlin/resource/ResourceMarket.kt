package resource

import java.lang.Integer.min
import kotlin.math.ceil

typealias ResourceBalance = MutableMap<ResourceType, Int>

enum class ResourceOrderPriority {
    DEMAND, REQUEST
}

data class ResourceOrder(val origin: ResourceNode2, val resources: ResourceBalance, val priority: ResourceOrderPriority)

sealed class ResourceDistributor(val network: ResourceNetwork) {
    abstract fun distribute(from: ResourceNode2, type: ResourceType, quantity: Int, destinations: Map<ResourceNode2, Int>): Map<ResourceNode2, Int>

    class EqualizeContainers(network: ResourceNetwork) : ResourceDistributor(network) {

        val containersSentTo = mutableMapOf<ResourceType, List<ResourceContainer>>()

        private fun getNodes(container: ResourceContainer, destinations: Map<ResourceNode2, Int>): Set<ResourceNode2> {
            return destinations.filterKeys { it.container == container }.keys
        }

        override fun distribute(
            from: ResourceNode2,
            type: ResourceType,
            quantity: Int,
            destinations: Map<ResourceNode2, Int>
        ): Map<ResourceNode2, Int> {
            val filteredDests = destinations.filterValues { it < 0 }
            // end distributions
            val distribution = destinations.keys.associateWith { 0 }.toMutableMap()

            // amount left undistributed
            var remainingSupply = quantity
            var remainingDemand = filteredDests.mapValues { -it.value } // the input should be negative to indicate demand

            val destinationContainers = filteredDests.keys.map { it.container }.toSet()
            val containersSentTo = (containersSentTo[type] ?: listOf()).filter { it in destinationContainers }.toMutableList()
            val containersNotSentTo = destinationContainers.filter { it !in containersSentTo }

            for(container in containersNotSentTo) {
                if(remainingSupply == 0 || remainingDemand.values.sum() == 0) {
                    return distribution
                }

                remainingSupply--
                remainingDemand[]
            }
        }
    }
}

class ResourceMarket(val network: ResourceNetwork) {

    private val orders = mutableListOf<ResourceOrder>()

    private fun add(dest: ResourceBalance, other: ResourceBalance) {
        for ((type, quantity) in dest) {
            dest[type] = quantity + (other[type] ?: 0)
        }
    }

    fun order(origin: ResourceNode2, resources: ResourceBalance, priority: ResourceOrderPriority) {
        val existing = getOrder(origin, priority)
        if (existing != null) {
            add(existing.resources, resources)
        } else {
            orders.add(ResourceOrder(origin, resources, priority))
        }
    }

    fun orderIn(to: ResourceNode2, resources: ResourceList, priority: ResourceOrderPriority) {
        order(to, resources.mapValues { (_, quantity) -> -quantity }.toMutableMap(), priority)
    }

    fun orderOut(from: ResourceNode2, resources: ResourceList, priority: ResourceOrderPriority) {
        order(from, resources.toMutableMap(), priority)
    }

    private fun getOrder(origin: ResourceNode2, priority: ResourceOrderPriority): ResourceOrder? {
        return orders.filter { it.origin == origin && it.priority == priority }
            .apply {
                if (size > 1) throw Exception("$size resource orders for $origin, should be <2")
            }.firstOrNull()
    }

    private fun getOrders(priority: ResourceOrderPriority): List<ResourceOrder> {
        return orders.filter { it.priority == priority }
    }

    private val demands = mutableMapOf<ResourceNode2, MutableMap<ResourceType, Int>>()
    private val requests = mutableMapOf<ResourceNode2, MutableMap<ResourceType, Int>>()

    val typesAndDestinations = mutableMapOf<ResourceType, MutableList<ResourceNode2>>()

    private fun combine(
        map: MutableMap<ResourceNode2, MutableMap<ResourceType, Int>>,
        node: ResourceNode2,
        amounts: Map<ResourceType, Int>
    ) {
        val entry = map[node]
        if (entry == null) {
            map[node] = amounts.toMutableMap()
        } else {
            map[node] = entry.map { (k, v) -> k to (v + (amounts[k] ?: 0)) }.toMap().toMutableMap()
        }
    }

    fun demand(node: ResourceNode2, amounts: Map<ResourceType, Int>) {
        combine(demands, node, amounts)
    }

    fun request(node: ResourceNode2, amounts: Map<ResourceType, Int>) {
        combine(requests, node, amounts)
    }

    fun requestOut(from: ResourceNode2, resources: ResourceList) {
        request(from, resources)
    }

    fun requestIn(to: ResourceNode2, resources: ResourceList) {
        request(to, resources.mapValues { (_, v) -> -v })
    }

    fun demandOut(from: ResourceNode2, resources: ResourceList) {
        demand(from, resources)
    }

    fun demandIn(to: ResourceNode2, resources: ResourceList) {
        demand(to, resources.mapValues { (_, v) -> -v })
    }

    fun simplifyTransactions(transactions: List<ResourceTransaction>): List<ResourceTransaction> {
        val newTransactions = mutableMapOf<Pair<ResourceNode2, ResourceNode2>, MutableResourceList>()
        outer@ for (transaction in transactions) {
            for (newTransaction in newTransactions) {
                if (newTransaction.key.first == transaction.src && newTransaction.key.second == transaction.dest) {
                    newTransaction.value.putAll(transaction.resources)
                    continue@outer
                }
            }
            newTransactions[transaction.src!! to transaction.dest!!] = transaction.resources.toMutableResourceList()
        }
        return newTransactions.map { ResourceTransaction(it.key.first, it.key.second, it.value) }
    }

    fun distributeEvenly(
        type: ResourceType,
        quantity: Int,
        requests: Map<ResourceNode2, Int>
    ): Map<ResourceNode2, Int> {

        // TODO need to decide whether we should distribute evenly by resource node or resource container
        // final map of allotments
        val allotments = mutableMapOf<ResourceNode2, Int>()
        for (dest in requests.keys) {
            // start them all out at 0
            allotments[dest] = 0
        }
        // we distribute evenly wrt type by making sure that each node that hasn't received any
        // if the type has not been sent to make it an empty list
        if (type !in typesAndDestinations) {
            typesAndDestinations[type] = mutableListOf()
        }
        val sentTo = typesAndDestinations[type]!!
        var remainingQuantity = quantity
        val remainingRequests = requests.toMutableMap()
        // first fill those that haven't gotten resources recently
        for ((dest, amount) in remainingRequests) {
            if (amount >= 0) { // should be negative
                continue
            }
            if (remainingQuantity == 0) {
                return allotments
            }
            if (dest !in sentTo) {
                // if the destination hasn't been sent to this cycle
                allotments[dest] = allotments[dest]!! + 1
                remainingRequests[dest] = remainingRequests[dest]!! + 1
                remainingQuantity--
                sentTo.add(dest)
            }
        }
        if (sentTo)
        // now they're all at even footing.
        // if they aren't at even footing then we've run out of resources

        // while there are some resources left to be distributed and there are some requests left to be fulfilled
            while (remainingQuantity > 0 && remainingRequests.any { it.value < 0 }) {
                // now try to give them all the "average" amount of remaining requests until we run out
                val average =
                    ceil(remainingQuantity.toDouble() / remainingRequests.filterValues { it < 0 }.size).toInt()
                for ((dest, request) in remainingRequests) {
                    if (request >= 0) { // should be negative
                        continue
                    }
                    if (remainingQuantity == 0) {
                        return allotments
                    }
                    val amountToSend = min(min(average, remainingQuantity), -request)
                    // if the destination hasn't been sent to this cycle
                    allotments[dest] = allotments[dest]!! + amountToSend
                    remainingRequests[dest] = remainingRequests[dest]!! + amountToSend
                    remainingQuantity -= amountToSend
                }
            }
        return allotments
    }

    fun createTransactions(
        from: Map<ResourceNode2, MutableMap<ResourceType, Int>>,
        to: Map<ResourceNode2, MutableMap<ResourceType, Int>>
    ): List<ResourceTransaction> {
        // first find out which resources are actually going to be sent
        // remove them from `from`
        // go through each resource type in the resources to be sent
        // try to distribute them evenly among nodes that are asking for it

        // only positive values (those with a surplus)
        val sends = from.map { (node, transacts) -> node to transacts.filterValues { it > 0 } }.toMap()
        // only negative values (those with a deficit)
        val receives = to.map { (node, transacts) -> node to transacts.filterValues { it < 0 } }.toMap()

        val potentialTransactions = mutableListOf<ResourceTransaction>()
        for ((fromNode, resources) in sends) {
            // for each node and the resources it wants to send
            for ((type, quantity) in resources) {
                // for each type and quantity in the resources
                // make a map of resource node to quantity of this type that it wants
                val receivesOfThisType = receives.mapValues { (_, transacts) -> transacts[type] ?: 0 }
                // get an allotment map of node to quantity that it will receive
                val allotments = distributeEvenly(type, quantity, receivesOfThisType)
                for ((toNode, amount) in allotments) {
                    // add this transaction
                    potentialTransactions.add(ResourceTransaction(fromNode, toNode, resourceListOf(type to amount)))
                }
            }
        }
        return potentialTransactions
    }

    fun confirmTransactions(
        from: MutableMap<ResourceNode2, MutableMap<ResourceType, Int>>,
        to: MutableMap<ResourceNode2, MutableMap<ResourceType, Int>>,
        transactions: List<ResourceTransaction>
    ) {
        for (transaction in transactions) {
            from[transaction.src!!] =
                to[transaction.src]!!.mapValues { (type, quantity) -> quantity - transaction.resources[type] }
                    .toMutableMap()
            from[transaction.dest!!] =
                to[transaction.dest]!!.mapValues { (type, quantity) -> quantity + transaction.resources[type] }
                    .toMutableMap()
        }
    }

    fun getTransactions(): List<ResourceTransaction> {
        val demandsToDemands = createTransactions(demands, demands)
        confirmTransactions(demands, demands, demandsToDemands)
        val demandsToRequests = createTransactions(demands, requests)
        confirmTransactions(demands, requests, demandsToRequests)
        return simplifyTransactions(demandsToDemands + demandsToRequests)
    }
}