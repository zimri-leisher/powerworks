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
data class ResourceTransaction(val src: ResourceNode2?, val dest: ResourceNode2?, val resources: ResourceList) {
    var state = TransactionState.NEW

    fun isValid(): Boolean {
        // if both ends are null then it is not valid
        if (src == null && dest == null) {
            return false
        }
        if (state != TransactionState.PENDING && src != null && !src.canOutput(resources)) {
            return false
        }
        if (state != TransactionState.FINISHED && dest != null && !dest.canInput(resources)) {
            return false
        }
        return true
    }

    fun start() {
        if (state != TransactionState.NEW) {
            throw Exception("Tried to start a transaction that was in state $state")
        }
        state = TransactionState.PENDING
        src?.output(resources)
    }

    fun finish() {
        if (state != TransactionState.PENDING) {
            throw Exception("Tried to finish a transaction that was in state $state")
        }
        state = TransactionState.FINISHED
        dest?.input(resources)
    }
}