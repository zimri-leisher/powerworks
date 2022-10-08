package level.block

import audio.AudioManager
import audio.AudioManager.SoundSource
import level.update.MachineBlockFinishWork
import network.BlockReference
import serialization.Id

abstract class MachineBlock(override val type: MachineBlockType<out MachineBlock>, xTile: Int, yTile: Int) : Block(type, xTile, yTile) {

    @Id(20)
    var on = type.startOn
        set(value) {
            if (!value && field) {
                onTurnOff()
                if (currentSound != null)
                    currentSound!!.playing = false
            } else if (value && !field) {
                onTurnOn()
                if (currentSound == null && type.onSound != null) {
                    AudioManager.play(type.onSound!!, x, y, true)
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
        super.update()
        if (on) {
            currentWork += 1
            onWork()
            if (currentWork >= (type.maxWork / type.speed).toInt()) {
                if (!type.loop)
                    on = false
                currentWork = (type.maxWork / type.speed).toInt()
                level.modify(MachineBlockFinishWork(toReference() as BlockReference, level))
            }
        }
    }

    open fun onWork() {
        // use power and type.defaultEfficiency
    }
}