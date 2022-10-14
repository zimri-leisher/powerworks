package level.update

import level.*
import level.block.BlockType
import level.block.DefaultBlock
import level.moving.MovingObject
import misc.TileCoord
import network.BlockReference
import network.LevelObjectReference
import network.PhysicalLevelObjectReference
import player.Player
import serialization.AsReference
import serialization.Id
import java.util.*

class LevelObjectSwitchLevelsTo(
    @Id(3)
    @AsReference
    val obj: LevelObject,
    @Id(5)
    val destinationPosition: TileCoord, level: Level
) : LevelUpdate(LevelUpdateType.LEVEL_OBJECT_SWITCH_LEVELS, level) {

    private constructor() : this(DefaultBlock(BlockType.ERROR, 0, 0), TileCoord(0, 0), LevelManager.EMPTY_LEVEL)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        if (obj !is PhysicalLevelObject) {
            return true
        }
        if (!level.canAdd(obj.type, destinationPosition.xTile shl 4, destinationPosition.yTile shl 4)) {
            return false
        }
        return true
    }

    override fun act() {
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

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is LevelObjectSwitchLevelsTo) {
            return false
        }
        return other.obj === obj && destinationPosition == other.destinationPosition
    }
}