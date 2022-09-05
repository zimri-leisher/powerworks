package resource

// resource network should handle expected resources
// pathfinding can be handled by connection
// connection can be created when a new resource node joins the group
// it will handle the pathfinding
// list of sell and buy
// every update, connects sellers with buyers
// we want to:
// * never have something selling that also wants to be bought
// * distribute sold items across buyers
abstract class TransactionExecutor {
    abstract fun canExecute(transaction: ResourceTransaction): Boolean
    abstract fun execute(transaction: ResourceTransaction)
}