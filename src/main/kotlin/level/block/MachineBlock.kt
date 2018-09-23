package level.block

import audio.AudioManager
import audio.AudioManager.SoundSource

abstract class MachineBlock(override val type: MachineBlockType<out MachineBlock>, xTile: Int, yTile: Int, rotation: Int, on: Boolean = type.defaultOn) : Block(type, xTile, yTile, rotation) {

    var on = on
        set(value) {
            val texture = type.textures[rotation]
            if (!value && field) {
                onTurnOff()
                if(currentSound != null)
                    currentSound!!.playing = false
            } else if (value && !field) {
                onTurnOn()
                if(currentSound == null && type.onSound != null) {
                    AudioManager.play(type.onSound!!, xPixel, yPixel, true)
                }
            }
            field = value
        }

    var currentSound: SoundSource? = null
    var currentWork = 0

    open fun onTurnOn() {

    }

    open fun onTurnOff() {

    }

    open fun onFinishWork() {

    }

    override fun update() {
        if (on) {
            currentWork++
            onWork()
            if (currentWork >= (type.maxWork / type.speed).toInt()) {
                currentWork = 0
                if (!type.loop)
                    on = false
                onFinishWork()
            }
        }
    }

    open fun onWork() {
        // use power and type.defaultEfficiency
    }
}