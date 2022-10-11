package resource

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


typealias ResourceBalance = MutableMap<ResourceType, Int>

enum class ResourceOrderPriority {
    DEMAND, REQUEST
}

enum class ResourceOrderDirection {
    IN, OUT
}

data class ResourceOrder(
    val type: ResourceType,
    val quantity: Int,
    val direction: ResourceOrderDirection,
    val priority: ResourceOrderPriority
)

sealed class ResourceDistributor(val network: ResourceNetwork<*>) {

    abstract fun distribute(
        out: ResourceOrder,
        inputs: Map<ResourceNode, ResourceOrder>,
    ): Map<ResourceNode, ResourceOrder>

}

class EqualizeContainers(network: ResourceNetwork<*>) : ResourceDistributor(network) {

    val containersSentTo = mutableMapOf<ResourceType, List<ResourceContainer>>()

    private fun getNodes(container: ResourceContainer, destinations: Map<ResourceNode, Int>): Set<ResourceNode> {
        return destinations.filterKeys { it.container == container }.keys
    }

    override fun distribute(
        out: ResourceOrder,
        inputs: Map<ResourceNode, ResourceOrder>,
    ): Map<ResourceNode, ResourceOrder> {
        val filteredDests = destinations.filterValues { it < 0 }.toMutableMap()
        // end distributions
        val distribution = destinations.keys.associateWith { 0 }.toMutableMap()

        // amount left undistributed
        var remainingSupply = quantity
        // the input should be negative to indicate demand, this makes it positive
        val remainingDemand = filteredDests.mapValues { -it.value }.toMutableMap()

        val destinationContainers = filteredDests.keys
        val containersSentTo =
            (containersSentTo[type] ?: listOf()).filter { it in destinationContainers }.toMutableList()
        val containersNotSentTo = destinationContainers.filter { it !in containersSentTo }

        for (container in containersNotSentTo) {
            if (remainingSupply == 0 || remainingDemand.isEmpty()) {
                return distribution
            }
            // give 1 to this container
            remainingSupply--
            remainingDemand[container] = remainingDemand[container] - 1
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

    fun order(origin: ResourceContainer, resources: ResourceBalance, priority: ResourceOrderPriority) {
        val existing = getOrder(origin, priority)
        if (existing != null) {
            add(existing.resources, resources)
        } else {
            orders.add(ResourceOrder(origin, resources, priority))
        }
    }

    fun orderIn(to: ResourceContainer, resources: ResourceList, priority: ResourceOrderPriority) {
        order(to, resources.mapValues { (_, quantity) -> -quantity }.toMutableMap(), priority)
    }

    fun orderOut(from: ResourceContainer, resources: ResourceList, priority: ResourceOrderPriority) {
        order(from, resources.toMutableMap(), priority)
    }

    private fun getOrder(origin: ResourceContainer, priority: ResourceOrderPriority): ResourceOrder? {
        return orders.filter { it.origin == origin && it.priority == priority }
            .apply {
                if (size > 1) throw Exception("$size resource orders for $origin, should be <2")
            }.firstOrNull()
    }

    private fun getOrders(priority: ResourceOrderPriority): List<ResourceOrder> {
        return orders.filter { it.priority == priority }
    }

    private val demands = mutableMapOf<ResourceContainer, MutableMap<ResourceType, Int>>()
    private val requests = mutableMapOf<ResourceContainer, MutableMap<ResourceType, Int>>()

    val typesAndDestinations = mutableMapOf<ResourceType, MutableList<ResourceContainer>>()

    private fun combine(
        map: MutableMap<ResourceNode, MutableMap<ResourceType, Int>>,
        node: ResourceNode,
        amounts: Map<ResourceType, Int>
    ) {
        val entry = map[node]
        if (entry == null) {
            map[node] = amounts.toMutableMap()
        } else {
            map[node] = entry.map { (k, v) -> k to (v + (amounts[k] ?: 0)) }.toMap().toMutableMap()
        }
    }

    fun demand(node: ResourceNode, amounts: Map<ResourceType, Int>) {
        combine(demands, node, amounts)
    }

    fun request(node: ResourceNode, amounts: Map<ResourceType, Int>) {
        combine(requests, node, amounts)
    }

    fun requestOut(from: ResourceNode, resources: ResourceList) {
        request(from, resources)
    }

    fun requestIn(to: ResourceNode, resources: ResourceList) {
        request(to, resources.mapValues { (_, v) -> -v })
    }

    fun demandOut(from: ResourceNode, resources: ResourceList) {
        demand(from, resources)
    }

    fun demandIn(to: ResourceNode, resources: ResourceList) {
        demand(to, resources.mapValues { (_, v) -> -v })
    }

    fun simplifyTransactions(transactions: List<ResourceTransaction>): List<ResourceTransaction> {
        val newTransactions = mutableMapOf<Pair<ResourceContainer, ResourceContainer>, MutableResourceList>()
        outer@ for (transaction in transactions) {
            for (newTransaction in newTransactions) {
                if (newTransaction.key.first == transaction.src && newTransaction.key.second == transaction.dest) {
                    newTransaction.value.putAll(transaction.resources)
                    continue@outer
                }
            }
            newTransactions[transaction.src to transaction.dest] = transaction.resources.toMutableResourceList()
        }
        return newTransactions.map { ResourceTransaction(it.key.first, it.key.second, it.value) }
    }

    fun distributeEvenly(
        type: ResourceType,
        quantity: Int,
        requests: Map<ResourceContainer, Int>
    ): Map<ResourceContainer, Int> {

        // TODO need to decide whether we should distribute evenly by resource node or resource container
        // final map of allotments
        val allotments = mutableMapOf<ResourceContainer, Int>()
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
        from: Map<ResourceContainer, MutableMap<ResourceType, Int>>,
        to: Map<ResourceContainer, MutableMap<ResourceType, Int>>
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
        from: MutableMap<ResourceContainer, MutableMap<ResourceType, Int>>,
        to: MutableMap<ResourceContainer, MutableMap<ResourceType, Int>>,
        transactions: List<ResourceTransaction>
    ) {
        for (transaction in transactions) {
            from[transaction.src] =
                to[transaction.src]!!.mapValues { (type, quantity) -> quantity - transaction.resources[type] }
                    .toMutableMap()
            from[transaction.dest] =
                to[transaction.dest]!!.mapValues { (type, quantity) -> quantity + transaction.resources[type] }
                    .toMutableMap()
        }
    }

    fun getTransactions(): List<ResourceTransaction> {
        // we want to store the last time that we sent resources to some node
        // network flow problem
        // create a source and sink
        // consider only high priority requests
        // for each resource type
        // create a graph
        // source with edges going to each output container, capacity of edge is equal to desired output
        // edges connecting output containers to input containers, if they both allow this type currently
        // i need to simplify something here... something has to give. i think that these demands are basically
        // impossible... if the resource nodes can stop accepting things based on the amounts contained in other
        // resources nodes...

        // alright. break it into steps
        // we have resource containers which have demands
        // use bipartite matching to create a distribution. doesn't matter yet if this is impossible due to resource
        // node constraints. we just vaguely know that this container should send to this container.
        // questions: using bipartite matching, can we make sure that we send to close containers first?

        // what if we think about this in a radically different way?
        // what if we take advantage of the fact that the machines produce on a schedule.. a period.. no i don't think so
        val demandsToDemands = createTransactions(demands, demands)
        confirmTransactions(demands, demands, demandsToDemands)
        val demandsToRequests = createTransactions(demands, requests)
        confirmTransactions(demands, requests, demandsToRequests)
        return simplifyTransactions(demandsToDemands + demandsToRequests)
    }
}