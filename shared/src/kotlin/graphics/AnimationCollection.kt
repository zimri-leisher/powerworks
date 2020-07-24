package graphics

import misc.PixelCoord

private var nextId = 0

class AnimationCollection(val animations: List<Animation>) {

    constructor(path: String,
                numberOfAnimations: Int,
                numberOfFrames: Int,
                startPlaying: Boolean = false,
                smoothing: Boolean = false,
                offsets: List<PixelCoord> = listOf(),
                closure: StepChain.() -> Unit = {}) : this(List(numberOfAnimations) { index -> Animation(path + "_$index", numberOfFrames, startPlaying, smoothing, offsets, closure) })

    init {
        nextId++
        ALL.add(this)
    }

    fun createLocalInstance() = AnimationCollection(animations.map { it.createLocalInstance() })

    operator fun get(i: Int) = animations[i]

    companion object {
        val ALL = mutableListOf<AnimationCollection>()

        val MACHINE_GUN = AnimationCollection("weapon/machine_gun/fire_dir", 4, 3) {
            sequence(0..2, arrayOf(0, 5, 10))
            toFrame(2, 10)
        }
    }
}