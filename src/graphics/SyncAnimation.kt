package graphics

import java.awt.image.BufferedImage

object SyncAnimations {
    val WEAPON_1 = SyncAnimation(ImageCollections.WEAPON_1, arrayOf(10, 30, 30))
}

class SyncAnimation(val images: ImageCollection, val frameTimes: Array<Int>, var playing: Boolean = false) : Texture {
    override var currentImage: BufferedImage = images[0].currentImage
    override val widthPixels: Int
        get() = currentImage.width
    override val heightPixels: Int
        get() = currentImage.height

    private var tick = 0

    fun pause() {
        playing = false
    }

    fun stop() {
        playing = false
        tick = 0
        currentImage = images[0].currentImage
    }

    fun play() {
        playing = true
    }

    init {
        anims.add(this)
    }

    private fun update() {
        if(!playing)
            return
        tick++
        var tot = 0
        var ind = -1
        while(tot + frameTimes[ind + 1] < tick) {
            ind++
            tot += frameTimes[ind]
        }
        currentImage = images[ind].currentImage
    }

    companion object {

        val anims = mutableListOf<SyncAnimation>()

        fun update() {
            anims.forEach { it.update() }
        }
    }
}