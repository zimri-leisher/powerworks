package level.update

import level.Level
import level.LevelManager
import level.canAdd
import level.moving.MovingObject
import misc.TileCoord
import network.BlockReference
import network.LevelObjectReference
import player.Player
import serialization.Id
import java.util.*

class LevelObjectSwitchLevelsTo(
        @Id(3)
        val reference: LevelObjectReference,
        @Id(5)
        val destinationPosition: TileCoord) : LevelUpdate(LevelUpdateType.LEVEL_OBJECT_SWITCH_LEVELS) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), TileCoord(0, 0))

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if (reference.value == null) {
            return false
        }
        if (!level.canAdd(reference.value!!.type, destinationPosition.xTile shl 4, destinationPosition.yTile shl 4)) {
            return false
        }
        return true
    }

    override fun act(level: Level) {
        val obj = reference.value!!
        if (obj is MovingObject) {
            obj.setPosition(destinationPosition.xTile shl 4, destinationPosition.yTile shl 4)
        }
        if (obj.inLevel) {
            if (obj.level != level) {
                // transient because its part of this update, doesnt need to be sent because this already is
                obj.level.modify(LevelObjectRemove(obj), true)
            } else {
                return
            }
        }
        println("acting on level switch, moving object from ${obj.level} to $level")
        level.modify(LevelObjectAdd(obj), true)
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is LevelObjectSwitchLevelsTo) {
            return false
        }
        if (other.reference.value == null) {
            return false
        }
        return other.reference.value == reference.value && destinationPosition == other.destinationPosition
    }

    override fun resolveReferences() {
        reference.value = reference.resolve()
    }

}