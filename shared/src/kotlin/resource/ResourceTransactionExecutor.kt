package resource

import misc.Geometry

// resource network should handle expected resources
// pathfinding can be handled by connection
// connection can be created when a new resource node joins the group
// it will handle the pathfinding
// list of sell and buy
// every update, connects sellers with buyers
// we want to:
// * never have something selling that also wants to be bought
// * distribute sold items across buyers

// this class is an abstraction for:
//      connections between resource containers
//      the "source" of resources
//      the player
abstract class ResourceTransactionExecutor {
    abstract fun canExecute(transaction: ResourceTransaction): Boolean
    abstract fun execute(transaction: ResourceTransaction)
    abstract fun getExecutionCost(transaction: ResourceTransaction): Int

    class Player(val player: player.Player) : ResourceTransactionExecutor() {

        override fun canExecute(transaction: ResourceTransaction): Boolean {
            if (!transaction.isValid()) {
                return false
            }
            val fromNodes = transaction.src.nodes
            val toNodes = transaction.dest.nodes

            var minDist = (MAX_RESOURCE_TRANSFER_DIST + 1).toDouble()
            for (fromNode in fromNodes) {
                for (toNode in toNodes) {
                    val dist = Geometry.distance(fromNode.x, fromNode.y, toNode.x, toNode.y)
                    if (dist < minDist) {
                        minDist = dist
                    }
                }
            }
            return minDist < MAX_RESOURCE_TRANSFER_DIST
        }

        override fun execute(transaction: ResourceTransaction) {
            transaction.start()
            transaction.finish()
        }

        override fun getExecutionCost(transaction: ResourceTransaction): Int {
            return 0
        }

        companion object {
            const val MAX_RESOURCE_TRANSFER_DIST = 16 * 16 // 16 tiles
        }
    }

    object Source : ResourceTransactionExecutor() {
        override fun canExecute(transaction: ResourceTransaction): Boolean {
            if(transaction.src !is SourceContainer && transaction.dest !is SourceContainer) {
                return false
            }
            return transaction.isValid()
        }

        override fun execute(transaction: ResourceTransaction) {
            transaction.start()
            transaction.finish()
        }

        override fun getExecutionCost(transaction: ResourceTransaction): Int {
            return 0
        }
    }
}