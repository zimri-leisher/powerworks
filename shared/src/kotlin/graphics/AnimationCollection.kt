package graphics

import misc.PixelCoord
import serialization.Input
import serialization.Output
import serialization.Serializer

private var nextId = 0

class AnimationCollection(val animations: List<Animation>) {

    var id = nextId++
    var isLocal = false

    constructor(path: String,
                numberOfAnimations: Int,
                numberOfFrames: Int,
                startPlaying: Boolean = false,
                smoothing: Boolean = false,
                offsets: List<PixelCoord> = listOf(),
                closure: StepChain.() -> Unit = {}) : this(List(numberOfAnimations) { index -> Animation(path + "_$index", numberOfFrames, startPlaying, smoothing, offsets, closure) })

    init {
        ALL.add(this)
    }

    fun createLocalInstance() = AnimationCollection(animations.map { it.createLocalInstance() }).also { it.id = id; it.isLocal = true }

    operator fun get(i: Int) = animations[i]

    companion object {
        val ALL = mutableListOf<AnimationCollection>()

        val MACHINE_GUN = AnimationCollection("weapon/machine_gun/fire_dir", 4, 3) {
            sequence(0..2, arrayOf(0, 2, 10))
            toFrame(2, 10)
        }
    }
}

class AnimationCollectionSerializer : Serializer<AnimationCollection>() {
    override fun write(obj: Any, output: Output) {
        obj as AnimationCollection
        output.writeInt(obj.id)
        output.writeBoolean(obj.isLocal)
    }

    override fun instantiate(input: Input): AnimationCollection {
        val id = input.readInt()
        val isCopy = input.readBoolean()
        return AnimationCollection.ALL.first { it.id == id }.let { if (isCopy) it.createLocalInstance() else it }
    }
}