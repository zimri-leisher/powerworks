package level.block

import com.badlogic.gdx.Input
import io.ControlEvent
import io.ControlEventType
import level.*
import level.moving.MovingObject
import level.update.FarseekerBlockSetAvailableLevels
import level.update.LevelObjectSwitchLevelsTo
import misc.TileCoord
import network.BlockReference
import progression.ProgressionManager
import screen.Camera
import screen.gui.GuiIngame
import serialization.Id
import java.util.*

class FarseekerBlock(xTile: Int, yTile: Int) : MachineBlock(MachineBlockType.FARSEEKER, xTile, yTile), LevelEventListener {

    private constructor() : this(0, 0)

    @Id(23)
    var availableDestinations = mapOf<UUID, LevelInfo>()

    @Id(24)
    var destinationLevel: Level? = null
        set(value) {
            if (field != value) {
                field = value
                destinationPosition = TileCoord(xTile, yTile)
                if (field != null && !field!!.loaded) {
                    (field as? RemoteLevel)?.load()
                }
            }
        }

    @Id(25)
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

    @Id(26)
    val sendingQueue = mutableListOf<MovingObject>()

    init {
        LevelManager.levelEventListeners.add(this)
    }

    override fun onLevelEvent(level: Level, event: LevelEvent) {
        if(level == destinationLevel && event == LevelEvent.LOAD) {
            // move the second view camera to the new level TODO find a more permanent solution for this?
            val cameraToMove = GuiIngame.secondView.camera
            if(cameraToMove is Camera) {
                destinationLevel!!.modify(LevelObjectSwitchLevelsTo(cameraToMove, TileCoord(cameraToMove.xTile, cameraToMove.yTile), destinationLevel!!), true)
            }
        }
    }

    override fun afterAddToLevel(oldLevel: Level) {
        super.afterAddToLevel(oldLevel)
        level.modify(FarseekerBlockSetAvailableLevels(this, ProgressionManager.getAvailableEnemyLevels(team.players.first()), level))
    }

    override fun onInteractOn(event: ControlEvent, x: Int, y: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (event.type == ControlEventType.PRESS && !shift && !ctrl && !alt) {
            if (button == Input.Buttons.LEFT) {
                this.type.guiPool!!.toggle(this)
            }
        }
        super.onInteractOn(event, x, y, button, shift, ctrl, alt)
    }

    override fun onCollide(obj: PhysicalLevelObject) {
        if (obj !is MovingObject) {
            return
        }
        println("moving obj collide ${destinationLevel}, ${destinationLevel?.loaded}, $destinationPosition")
        if (destinationLevel != null && destinationLevel!!.loaded && destinationPosition != null) {
            sendingQueue.add(obj)
        }
    }

    override fun update() {
        val iter = sendingQueue.iterator()
        if(sendingQueue.isNotEmpty()) {
            println("sending queue: ${sendingQueue.joinToString()}")
        }
        for(obj in iter) {
            if(destinationLevel != null && destinationPosition != null) {
                if(destinationLevel!!.modify(LevelObjectSwitchLevelsTo(obj, destinationPosition!!, destinationLevel!!))) {
                    iter.remove()
                    println("switched levels succesfully")
                }
            } else {
                println("objects in queue with $destinationLevel, $destinationPosition")
            }
        }
        super.update()
    }
}