package resource

abstract class ResourceNodeConnection(val from: ResourceNode2, val to: ResourceNode2) : TransactionExecutor() {
    override fun canExecute(transaction: ResourceTransaction): Boolean {
        if(transaction.src != from || transaction.dest != to) {
            return false
        }
        return transaction.isValid()
    }

    override fun execute(transaction: ResourceTransaction) {
        transaction.start()
        transaction.finish()
    }
}