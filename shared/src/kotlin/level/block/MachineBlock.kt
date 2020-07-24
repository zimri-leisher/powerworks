package level.block

import audio.AudioManager
import audio.AudioManager.SoundSource
import level.MachineBlockFinishWork
import network.BlockReference
import serialization.Id

abstract class MachineBlock(override val type: MachineBlockType<out MachineBlock>, xTile: Int, yTile: Int, rotation: Int, on: Boolean = type.startOn) : Block(type, xTile, yTile, rotation) {

    @Id(20)
    var on = on
        set(value) {
            if (!value && field) {
                onTurnOff()
                if (currentSound != null)
                    currentSound!!.playing = false
            } else if (value && !field) {
                onTurnOn()
                if (currentSound == null && type.onSound != null) {
                    AudioManager.play(type.onSound!!, xPixel, yPixel, true)
                }
            }
            field = value
        }

    @Id(21)
    var currentSound: SoundSource? = null

    @Id(22)
    var currentWork = 0

    open fun onTurnOn() {

    }

    open fun onTurnOff() {

    }

    open fun onFinishWork() {

    }

    override fun update() {
        if (on) {
            currentWork += 1
            onWork()
            if (currentWork >= (type.maxWork / type.speed).toInt()) {
                if (!type.loop)
                    on = false
                level.modify(MachineBlockFinishWork(toReference() as BlockReference))
            }
        }
    }

    open fun onWork() {
        // use power and type.defaultEfficiency
    }
}