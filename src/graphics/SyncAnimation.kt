package graphics

import java.awt.image.BufferedImage

class SyncAnimation(val images: ImageCollection, val frameTimes: Array<Int>, var playing: Boolean = false) : Texture {

    constructor(path: String, numberOfFrames: Int, frameTimes: Array<Int>, playing: Boolean = false) : this(ImageCollection(path, numberOfFrames), frameTimes, playing)

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
        val WEAPON_1_0 = SyncAnimation("/textures/weapon/weapon1/dir_0.png", 3, arrayOf(2, 2, 6), true)
        val WEAPON_1_1 = SyncAnimation("/textures/weapon/weapon1/dir_1.png", 3, arrayOf(2, 2, 6), true)
        val WEAPON_1_2 = SyncAnimation("/textures/weapon/weapon1/dir_2.png", 3, arrayOf(2, 2, 6), true)
        val WEAPON_1_3 = SyncAnimation("/textures/weapon/weapon1/dir_3.png", 3, arrayOf(2, 2, 6), true)
        val WEAPON_2_0 = SyncAnimation("/textures/weapon/weapon1/dir_0.png", 3, arrayOf(8, 8, 8), true)
        val WEAPON_2_1 = SyncAnimation("/textures/weapon/weapon1/dir_1.png", 3, arrayOf(8, 8, 8), true)
        val WEAPON_2_2 = SyncAnimation("/textures/weapon/weapon1/dir_2.png", 3, arrayOf(8, 8, 8), true)
        val WEAPON_2_3 = SyncAnimation("/textures/weapon/weapon1/dir_3.png", 3, arrayOf(8, 8, 8), true)

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}