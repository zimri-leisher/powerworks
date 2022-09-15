package level.update

import player.Player

class ResourceTransactionExecute : GameUpdate(LevelUpdateType.RESOURCE_TRANSACTION_EXECUTE) {
    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {

    }

    override fun act() {
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: GameUpdate): Boolean {
    }

    override fun resolveReferences() {
    }
}