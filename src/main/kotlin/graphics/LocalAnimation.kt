package graphics

import java.awt.image.BufferedImage

/**
 * An animation that is not necessarily in sync with all other instances of itself. This is handy for machines, which, depending on when they
 * are turned on and when they are placed, may not always be running at the same place
 */
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
        var index = 0
        while(index < animation.frameTimes.lastIndex && total + (animation.frameTimes[index + 1] / speed).toInt() < tick) {
            index++
            total += (animation.frameTimes[index] / speed).toInt()
        }
        currentImage = animation.images[index].currentImage
        if(index == animation.frameTimes.lastIndex) {
            tick = 0
        }
    }

    companion object {

        val ALL = mutableListOf<LocalAnimation>()

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}