package level.block

import graphics.LocalAnimation

abstract class MachineBlock(xTile: Int, yTile: Int, override val type: MachineBlockType, on: Boolean = false) : Block(xTile, yTile, type) {

    var on = on
        set(value) {
            if (!value && field) {
                onTurnOff()
                if(type.getTexture(rotation) is LocalAnimation) {
                    (type.getTexture(rotation) as LocalAnimation).playing = false
                }
            } else if (value && !field) {
                onTurnOn()
                if(type.getTexture(rotation) is LocalAnimation) {
                    (type.getTexture(rotation) as LocalAnimation).playing = true
                }
            }
            field = value
        }
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
            if (currentWork == type.maxWork) {
                currentWork = 0
                onFinishWork()
                if (!type.loop)
                    on = false
            }
        }
    }
}