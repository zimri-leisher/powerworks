package level.block

import io.PressType
import level.*
import level.moving.MovingObject
import level.update.FarseekerBlockSetAvailableLevels
import level.update.LevelObjectAdd
import misc.TileCoord
import network.BlockReference
import progression.ProgressionManager
import screen.IngameGUI
import serialization.Id
import java.util.*

class FarseekerBlock(xTile: Int, yTile: Int, rotation: Int) : Block(BlockType.FARSEEKER, xTile, yTile, rotation) {

    private constructor() : this(0, 0, 0)

    @Id(20)
    var availableDestinations = mapOf<UUID, LevelInfo>()
        set(value) {
            field = value
            val destination = field.entries.firstOrNull { it.key != level.id }
            if (destination != null) {
                destinationLevel = LevelManager.getLevelByIdOrNull(destination.key)
                        ?: RemoteLevel(destination.key, destination.value).apply { initialize() }
                destinationPosition = TileCoord(xTile, yTile)
            }
        }

    @Id(21)
    var destinationLevel: Level? = null
        set(value) {
            if (field != value) {
                field = value
                if (field != null && !field!!.loaded) {
                    (field as? RemoteLevel)?.load()
                }
            }
        }

    @Id(22)
    var destinationPosition: TileCoord? = null
        set(value) {
            if (value != null) {
                if (destinationLevel != null && destinationLevel!!.isTileWithinBounds(value.xTile, value.yTile)) {
                    field = value
                } else {
                    field = null
                }
            } else {
                field = null
            }
        }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        destinationLevel?.modify(LevelObjectAdd(IngameGUI.cameras[1]), true)
    }

    override fun onAddToLevel() {
        super.onAddToLevel()
        level.modify(FarseekerBlockSetAvailableLevels(toReference() as BlockReference, ProgressionManager.getAvailableEnemyLevels(team.players.first())))
    }

    override fun onCollide(o: LevelObject) {
        if (o !is MovingObject) {
            return
        }
        if (destinationLevel != null && destinationLevel!!.loaded && destinationPosition != null) {
            println("sending to $destinationLevel")
            o.level.remove(o)
            o.setPosition(destinationPosition!!.xTile shl 4, destinationPosition!!.yTile shl 4)
            destinationLevel!!.add(o)
        }
    }
}