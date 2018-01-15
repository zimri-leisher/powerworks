package level.block

import audio.AudioManager
import audio.AudioManager.SoundSource
import graphics.LocalAnimation

abstract class MachineBlock(xTile: Int, yTile: Int, override val type: MachineBlockType, on: Boolean = false) : Block(type, yTile, xTile) {

    var on = on
        set(value) {
            if (!value && field) {
                onTurnOff()
                if(currentSound != null)
                    currentSound!!.playing = false
                if (type.getTexture(rotation) is LocalAnimation) {
                    (type.getTexture(rotation) as LocalAnimation).playing = false
                }
            } else if (value && !field) {
                onTurnOn()
                if(currentSound == null && type.onSound != null) {
                    AudioManager.play(type.onSound!!, xPixel, yPixel, true)
                }
                if (type.getTexture(rotation) is LocalAnimation) {
                    (type.getTexture(rotation) as LocalAnimation).playing = true
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
            if (currentWork >= (type.maxWork / type.defaultSpeed).toInt()) {
                currentWork = 0
                onFinishWork()
                if (!type.loop)
                    on = false
            }
        }
    }

    open fun onWork() {
        // use power and type.defaultEfficiency
    }
}