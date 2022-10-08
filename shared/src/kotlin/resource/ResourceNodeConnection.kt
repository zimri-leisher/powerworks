package resource

abstract class ResourceNodeConnection(val from: ResourceNode, val to: ResourceNode) : ResourceTransactionExecutor() {
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

    override fun execute(transaction: ResourceTransaction) {
        transaction.start()
        transaction.finish()
    }
}