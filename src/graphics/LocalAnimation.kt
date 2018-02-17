package graphics

import java.awt.image.BufferedImage

class LocalAnimation(val animation: SyncAnimation, var playing: Boolean = false, var speed: Float = 1f): Texture {
    override var currentImage: BufferedImage = animation.images[0].currentImage
    override val widthPixels: Int
        get() = currentImage.width
    override val heightPixels: Int
        get() = currentImage.height

    private var tick = 0

    /**
     * Stops and resets this
     */
    fun stop() {
        playing = false
        tick = 0
        currentImage = animation.images[0].currentImage
    }

    init {
        ALL.add(this)
    }

    fun update() {
        if(!playing)
            return
        tick++
        var total = 0
        var index = -1
        while(total + (animation.frameTimes[index + 1] / speed).toInt() < tick) {
            index++
            total += (animation.frameTimes[index] / speed).toInt()
        }
        currentImage = animation.images[index].currentImage
    }

    companion object {

        val ALL = mutableListOf<LocalAnimation>()

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}