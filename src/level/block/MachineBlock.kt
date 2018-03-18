package level.block

import audio.AudioManager
import audio.AudioManager.SoundSource
import graphics.LocalAnimation

abstract class MachineBlock(override val type: MachineBlockTemplate<out MachineBlock>, xTile: Int, yTile: Int, rotation: Int, on: Boolean = false) : Block(type, xTile, yTile, rotation) {

    var on = on
        set(value) {
            val texture = type.textures[rotation]
            if (!value && field) {
                onTurnOff()
                if(currentSound != null)
                    currentSound!!.playing = false
                if (texture.texture is LocalAnimation) {
                    texture.texture.playing = false
                }
            } else if (value && !field) {
                onTurnOn()
                if(currentSound == null && type.onSound != null) {
                    AudioManager.play(type.onSound!!, xPixel, yPixel, true)
                }
                if (texture.texture is LocalAnimation) {
                    texture.texture.playing = true
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