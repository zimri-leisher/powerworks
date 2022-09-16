package resource

abstract class ResourceNodeConnection(val from: ResourceNode2, val to: ResourceNode2) : ResourceTransactionExecutor() {
    // only executes transactions between from and to, nothing else
    override fun canExecute(transaction: ResourceTransaction): Boolean {
        if(!transaction.isValid()) {
            return false
        }
        if(transaction.src != from.container || transaction.dest != to.container) {
            return false
        }
        return true
    }
}