package graphics

import java.awt.image.BufferedImage

class SyncAnimation(val images: ImageCollection, val frameTimes: Array<Int>, var playing: Boolean = false) : Texture {

    override var currentImage: BufferedImage = images[0].currentImage
    override val widthPixels: Int
        get() = currentImage.width
    override val heightPixels: Int
        get() = currentImage.height

    private var tick = 0
    private var frame = 0

    fun stop() {
        playing = false
        reset()
    }

    fun toggle() {
        playing = !playing
    }

    fun toggleAndReset() {
        toggle()
        reset()
    }

    fun reset() {
        tick = 0
        frame = 0
        currentImage = images[0].currentImage
    }

    init {
        ALL.add(this)
    }

    private fun update() {
        if(!playing)
            return
        tick++
        if(frameTimes[frame] <= tick) {
            tick = 0
            frame++
            if(frame > frameTimes.lastIndex) {
                frame = 0
            }
        }
        currentImage = images[frame].currentImage
    }

    companion object {

        val ALL = mutableListOf<SyncAnimation>()
        // TODO
        //val WEAPON_1 = SyncAnimation(Image.Weapon.ONE.textures[0], arrayOf(2, 2, 6))
        //val WEAPON_2 = SyncAnimation(Image.Weapon.TWO.textures[0], arrayOf(8, 8, 8))
        //val WEAPON_3 = SyncAnimation(Image.Weapon.THREE.textures[0], arrayOf(2, 10, 30))

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}