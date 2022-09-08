package resource

// resource nodes should be simple
//      they should not interact with the network
// transactions should be simple
//      they should be executed by the network
// put the complex stuff in the network
//      negotiating process
//      actual routing algo

enum class TransactionState {
    NEW, PENDING, FINISHED
}

// TODO maybe this should be between two resource containers?
data class ResourceTransaction(val src: ResourceContainer, val dest: ResourceContainer, val resources: ResourceList) {
    var state = TransactionState.NEW

    fun isValid(): Boolean {
        if (state != TransactionState.PENDING && !src.canRemove(resources)) {
            return false
        }
        if (state != TransactionState.FINISHED && !dest.canAdd(resources)) {
            return false
        }
        return true
    }

    fun start() {
        if (state != TransactionState.NEW) {
            throw Exception("Tried to start a transaction that was in state $state")
        }
        state = TransactionState.PENDING
        src.remove(resources)
    }

    fun finish() {
        if (state != TransactionState.PENDING) {
            throw Exception("Tried to finish a transaction that was in state $state")
        }
        state = TransactionState.FINISHED
        dest.add(resources)
    }
}