package level.update

import item.ItemType
import level.Level
import level.LevelManager
import player.Player
import resource.*
import serialization.Id

class ResourceTransactionExecute(
    @Id(2)
    val transaction: ResourceTransaction,
    @Id(3)
    val executor: ResourceTransactionExecutor,
    level: Level
) : LevelUpdate(LevelUpdateType.RESOURCE_TRANSACTION_EXECUTE, level) {

    private constructor() : this(
        ResourceTransaction(SourceContainer(), SourceContainer(), stackOf(ItemType.ERROR, 0)),
        SourceTransactionExecutor(),
        LevelManager.EMPTY_LEVEL
    )

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct() = executor.canExecute(transaction)

    override fun act() {
        executor.execute(transaction)
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        return other is ResourceTransactionExecute && other.transaction == transaction && other.executor == executor
    }
}