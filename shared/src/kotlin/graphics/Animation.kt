package graphics

import main.height
import main.toColor
import main.width
import misc.Coord
import serialization.*

sealed class AnimationStep(val stepID: String?) {

    /**
     * The progress towards completion, where 1 is complete and 0 is beginning. The only case in which this shouldn't go
     * towards 1 as time goes on is if it has halted the program or if it is a [StepChain] moving in reverse
     */
    abstract val progress: Float

    /**
     * This function is called once per tick while this step is active. The function may choose to never return true
     * and continue holding the animation, or it could return true and the next animation step would start executing.
     * Immediately after this function passes execution (returns true), the reset method of this will be called by the
     * [StepChain] class
     *
     * @param animation the animation this step is part of.
     * This is used inside the [AnimationStep] children to change frames, ticks, whether or not the animation is playing and so on
     *
     * @return whether to go on to the next step or not
     */
    abstract fun execute(context: StepChain, animation: Animation): Boolean

    /**
     * Prepares this class for another loop
     */
    abstract fun reset()

    abstract fun copy(): AnimationStep
}

class Frame(val fromFrameIndex: Int, val toFrameIndex: Int, ticks: Int = 0, stepID: String? = "frame $toFrameIndex") :
    AnimationStep(stepID) {

    val pause = Pause(ticks, null)

    override val progress get() = pause.progress

    override fun execute(context: StepChain, animation: Animation): Boolean {
        if (pause.execute(context, animation)) {
            pause.reset()
            if (context.reversed) {
                animation.currentFrameIndex = fromFrameIndex
            } else {
                animation.currentFrameIndex = toFrameIndex
            }
            return true
        }
        return false
    }

    override fun reset() {
        pause.reset()
    }

    override fun copy(): AnimationStep {
        return Frame(fromFrameIndex, toFrameIndex, pause.ticks, stepID)
    }

    override fun toString() =
        "Frame step, index: $fromFrameIndex -> $toFrameIndex, progress: ${pause.currentTicks}/${pause.ticks}"
}

class Pause(val ticks: Int, stepID: String?) : AnimationStep(stepID) {

    var currentTicks = 0

    override val progress get() = if (ticks == 0) 1f else (currentTicks.toFloat() / ticks)

    override fun execute(context: StepChain, animation: Animation): Boolean {
        if (currentTicks == ticks) {
            return true
        }
        currentTicks++
        return false
    }

    override fun reset() {
        currentTicks = 0
    }

    override fun copy(): AnimationStep {
        return Pause(ticks, stepID)
    }
}

class Loop(val numberOfTimes: Int = -1, stepID: String?, closure: StepChain.() -> Unit) : AnimationStep(stepID) {

    val stepChain = StepChain(null, closure)

    var currentNumberOfTimes = 0

    override val progress get() = if (numberOfTimes == -1) stepChain.progress else (currentNumberOfTimes + stepChain.progress) / numberOfTimes

    override fun execute(context: StepChain, animation: Animation): Boolean {
        if (stepChain.execute(context, animation)) {
            currentNumberOfTimes++
            if (currentNumberOfTimes == numberOfTimes) {
                return true
            }
            stepChain.reset()
        }
        return false
    }

    override fun reset() {
        currentNumberOfTimes = 0
        stepChain.reset()
    }

    override fun copy(): AnimationStep {
        val copy = Loop(numberOfTimes, stepID, {})
        copy.stepChain.steps.clear()
        copy.stepChain.steps.addAll(stepChain.steps.map { it.copy() })
        return copy
    }
}

class GoToStep(val stepIDToGoTo: String, stepID: String?) : AnimationStep(stepID) {

    override val progress get() = 1f

    override fun execute(context: StepChain, animation: Animation): Boolean {
        context.currentStepIndex = context.findIndex(stepIDToGoTo)
        return true
    }

    override fun reset() {
    }

    override fun copy(): AnimationStep {
        return GoToStep(stepIDToGoTo, stepID)
    }
}

class Stop(stepID: String?) : AnimationStep(stepID) {

    override val progress get() = 1f

    override fun execute(context: StepChain, animation: Animation): Boolean {
        animation.playing = false
        return true
    }

    override fun reset() {
    }

    override fun copy(): AnimationStep {
        return Stop(stepID)
    }
}

class Reverse(stepID: String?) : AnimationStep(stepID) {
    override val progress get() = 1f

    override fun execute(context: StepChain, animation: Animation): Boolean {
        context.reversed = true
        return true
    }

    override fun reset() {}

    override fun copy(): AnimationStep {
        return Reverse(stepID)
    }
}

class StepChain(stepID: String? = null, closure: StepChain.() -> Unit) : AnimationStep(stepID) {

    val steps = mutableListOf<AnimationStep>()
    var currentStepIndex = 0
        set(value) {
            steps[field].reset()
            field = value
        }
    var reversed = false

    // subtract if reversed because going down
    override val progress get() = ((currentStepIndex + (if (reversed) -1 else 1) * steps[currentStepIndex].progress) / steps.size)

    init {
        closure()
    }

    override fun execute(context: StepChain, animation: Animation): Boolean {
        if (steps[currentStepIndex].execute(this, animation)) {
            // reset is called when step index changes
            if (!reversed) {
                if (currentStepIndex == steps.lastIndex) {
                    return true
                } else {
                    currentStepIndex++
                }
            } else {
                if (currentStepIndex == 0) {
                    return true
                } else {
                    currentStepIndex--
                }
            }
        }
        return false
    }

    override fun reset() {
        currentStepIndex = if (reversed) steps.lastIndex else 0
        steps.forEach { it.reset() }
        reversed = false
    }

    fun find(stepID: String): AnimationStep {
        return steps[findIndex(stepID)]
    }

    fun findIndex(stepID: String): Int {
        val index = steps.indexOfFirst { it.stepID != null && it.stepID == stepID }
        if (index == -1) {
            throw IllegalArgumentException("stepID $stepID does not exist")
        }
        return index
    }

    /**
     * Adds a sequence of [toFrame] steps defined by the [frameRange] with the given [times]
     * @param frameRange the range of frames (e.g. 0..10 or 5 until 7)
     * @param times the ticks parameter to pass to the toFrame step for the given step (denoted by index, if this is index
     * 2 in the array it will set the second frame of this sequence to the ticks in that index)
     * @param stepID the identifier that will be given to the first frame in this sequence. You can use this with the [goToStep] step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use [goToStep] to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     */
    fun sequence(frameRange: IntRange, times: Array<Int>, stepID: String? = null) {
        for (i in frameRange) {
            toFrame(i, times[i - frameRange.first], if (i == frameRange.first) stepID else null)
        }
    }

    /**
     * Adds a step telling the animation to go to the specified frame after a period of ticks
     * @param ticks the number of ticks to wait on this frame before going on to the next step
     * @param stepID the identifier of this frame. You can use this with the [goToStep] step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use [goToStep] to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     */
    fun toFrame(frame: Int = getLastFrameIndex() + 1, ticks: Int = 0, stepID: String? = null) {
        // the last instance of a Frame step should be the one before this
        var lastFrameIndex = getLastFrameIndex()
        if (lastFrameIndex == -1) {
            lastFrameIndex = frame
        }
        steps.add(Frame(lastFrameIndex, frame, ticks, stepID))
    }

    private fun getLastFrameIndex() = (steps.lastOrNull { it is Frame } as? Frame)?.toFrameIndex ?: -1

    /**
     * Adds a step that pauses execution of the next step until a period of ticks
     * @param ticks the number of ticks to wait before going on to the next step
     * @param stepID the identifier of this frame. You can use this with the [goToStep] step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use [goToStep] to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     */
    fun pauseFor(ticks: Int, stepID: String? = null) {
        steps.add(Pause(ticks, stepID))
    }

    /**
     * Adds a step that moves execution to the specified stepID
     * @param stepID the identifier of this frame. You can use this with the [goToStep] step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use [goToStep] to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     */
    fun goToStep(stepIDToGoTo: String, stepID: String? = null) {
        steps.add(GoToStep(stepIDToGoTo, stepID))
    }

    /**
     * Adds a step that will execute steps inside the closure a specified number of times, or forever
     * @param numberOfTimes the number of times to go through the steps in the closure. If -1, it will loop forever
     * @param stepID the identifier of this frame. You can use this with the [goToStep] step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use [goToStep] to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     * @param closure the execution context for the inside of the loop. You can use all the same methods for creating steps,
     * including this one
     */
    fun loop(numberOfTimes: Int = -1, stepID: String? = null, closure: StepChain.() -> Unit) {
        steps.add(Loop(numberOfTimes, stepID, closure))
    }

    /**
     * Adds a step that will execute multiple, other steps together
     * @param stepID the identifier of this frame. You can use this with the [goToStep] step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use [goToStep] to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     * @param closure the execution context for the steps you want to run. You can use all the same methods for creating steps,
     * including this one
     */
    fun chain(stepID: String? = null, closure: StepChain.() -> Unit) {
        steps.add(StepChain(stepID, closure))
    }

    /**
     * Adds a step that will stop execution. Please note the difference between stopping the animation and stopping
     * the execution--stopping execution doesn't reset frames or steps. It merely keeps execution exactly where it is.
     * @param stepID the identifier of this frame. You can use this with the [goToStep] step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use [goToStep] to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     */
    fun stop(stepID: String? = null) {
        steps.add(Stop(stepID))
    }

    /**
     * Adds a step that will reverse the step chain it is executed in. For example, if it is executed inside of a loop,
     * it will cause the loop to run backwards.
     * @param stepID the identifier of this frame. You can use this with the [goToStep] step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use [goToStep] to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     */
    fun reverse(stepID: String? = null) {
        steps.add(Reverse(stepID))
    }

    override fun copy(): AnimationStep {
        val copy = StepChain(stepID, {})
        copy.steps.clear()
        copy.steps.addAll(steps.map { it.copy() })
        return copy
    }
}

private var nextId = 0

class Animation(
    path: String,
    numberOfFrames: Int,
    val startPlaying: Boolean = false,
    val smoothing: Boolean = false,
    val offsets: List<Coord> = listOf(),
    closure: StepChain.() -> Unit = {}
) : Renderable() {

    private constructor() : this("misc/error", 1)

    var id = nextId++
    var isLocal = false

    val frames = ImageCollection(path, numberOfFrames)

    var currentFrameIndex = 0
        set(value) {
            lastFrame = currentFrame
            field = value
        }

    override val xOffset get() = if (currentFrameIndex > offsets.lastIndex) 0 else offsets[currentFrameIndex].x
    override val yOffset get() = if (currentFrameIndex > offsets.lastIndex) 0 else offsets[currentFrameIndex].y
    val currentFrame get() = frames[currentFrameIndex]
    private var lastFrame = frames[0]

    override val width get() = currentFrame.width
    override val height get() = currentFrame.height


    /**
     * The stepID of the current lowest level step
     */
    val currentStepID get() = steps.steps[steps.currentStepIndex].stepID

    /**
     * A float between 0 and 1 which represents the percentage of completion of the step. This doesn't change whether
     * this is playing forwards or backwards.
     *
     * For example, the [Frame] step would return .5f if it took 14 ticks to complete and had currently gone through 7 of
     * them
     */
    val currentStepProgress get() = steps.steps[steps.currentStepIndex].progress

    /**
     * The overall progress of this animation.
     */
    val overallProgress get() = steps.progress

    var playing = false

    val steps: StepChain = StepChain {
        toFrame(0, 0, "start")
        closure()
    }

    init {
        if (startPlaying)
            play()
        ALL.add(this)
    }

    /**
     * Starts an animation where it last left off (if this hasn't been played before, that's at the beginning) and plays
     * it forwards. The direction will not change unless [playBackwards] or [playBackwardsFrom] is called, regardless of if
     * it's stopped or started
     */
    fun play() {
        playing = true
        steps.reversed = false
    }

    /**
     * Starts an animation where it last left off (if this hasn't been played before, that's at the beginning) and plays
     * it backwards. The direction will not change unless [play] or [playFrom] is called, regardless of if
     * it's stopped or started
     */
    fun playBackwards() {
        playing = true
        steps.reversed = true
    }

    /**
     * Plays an animation forwards from a given [stepID]. The [stepID] must be assigned to something inside the first level of closure,
     * otherwise it will not be found. The direction will not change unless [playBackwards] or [playBackwardsFrom]is called,
     * regardless of if it's stopped or started
     * @param stepID the stepID to play from
     */
    fun playFrom(stepID: String) {
        val index = steps.findIndex(stepID)
        if (index == -1) {
            throw IllegalArgumentException("stepID $stepID does not exist")
        }
        playFrom(index)
    }

    /**
     * Plays an animation forwards from the given step index. The direction will not change unless [playBackwards] or [playBackwardsFrom]
     * is called, regardless of if it's stopped or started
     */
    fun playFrom(index: Int) {
        steps.currentStepIndex = index
        play()
    }

    /**
     * Plays an animation from a given stepID. The stepID must be assigned to something inside the first level of closure,
     * otherwise it will not be found. The direction will not change unless [play] or [playFrom] is called, regardless of
     * if it's stopped or started
     */
    fun playBackwardsFrom(stepID: String) {
        val index = steps.findIndex(stepID)
        if (index == -1) {
            throw IllegalArgumentException("stepID $stepID does not exist")
        }
        playBackwardsFrom(index)
    }

    /**
     * Plays an animation backwards from the given step index. The direction will not change unless [play] or
     * [playFrom] is called, regardless of if it's stopped or started
     */
    fun playBackwardsFrom(index: Int) {
        steps.currentStepIndex = index
        playBackwards()
    }

    /**
     * Stops playing an animation but resets nothing. This doesn't change direction or the current step
     */
    fun pause() {
        playing = false
    }

    /**
     * This completely stops and resets the animation and all of the steps, except for the direction.
     */
    fun stop() {
        playing = false
        steps.currentStepIndex = 0
        steps.reset()
    }

    fun update() {
        if (playing) {
            if (steps.execute(steps, this)) {
                playing = false
            }
        }
    }

    override fun render(x: Int, y: Int, width: Int, height: Int, keepAspect: Boolean, params: TextureRenderParams) {
        if (smoothing) {
            if (keepAspect) {
                Renderer.renderTextureKeepAspect(lastFrame, x + xOffset, y + yOffset, width, height)
                Renderer.renderTextureKeepAspect(
                    currentFrame,
                    x + xOffset,
                    y + yOffset,
                    width,
                    height,
                    TextureRenderParams(color = toColor(alpha = currentStepProgress)).combine(params)
                )
            } else {
                Renderer.renderTexture(lastFrame, x + xOffset, y + yOffset, width, height)
                Renderer.renderTexture(
                    currentFrame,
                    x + xOffset,
                    y + yOffset,
                    width,
                    height,
                    TextureRenderParams(color = toColor(alpha = currentStepProgress)).combine(params)
                )
            }
        } else {
            if (keepAspect) {
                Renderer.renderTextureKeepAspect(currentFrame, x + xOffset, y + yOffset, width, height, params)
            } else {
                Renderer.renderTexture(currentFrame, x + xOffset, y + yOffset, width, height, params)
            }
        }
    }

    fun createLocalInstance(): Animation {
        val local = Animation(frames.identifier, frames.textures.size, startPlaying, smoothing, offsets)
        local.steps.steps.clear()
        local.steps.steps.addAll(steps.steps.map { it.copy() })
        local.isLocal = true
        return local
    }

    /**
     * @return the [com.badlogic.gdx.graphics.g2d.TextureRegion] at the given index. This is not the same thing as a step!
     */
    operator fun get(i: Int) = frames[i]

    companion object {

        val ALL = mutableListOf<Animation>()

        val TEST_ANIM = Animation("gui/test/test", 8, true) {
            sequence(0 until 8, arrayOf(50, 50, 50, 50, 50, 50, 50, 50))
        }

        val MAIN_MENU_PLAY_BUTTON = Animation("gui/play_button", 7, smoothing = true) {
            toFrame(ticks = 2)
            toFrame(ticks = 3)
            toFrame(ticks = 3)
            toFrame(ticks = 3)
            toFrame(ticks = 3, stepID = "outside_loop")
            loop(stepID = "inside_loop") {
                toFrame(5, 15)
                toFrame(6, 15)
            }
        }

        val MINER = Animation("block/miner", 8, true, false) {
            loop {
                loop(20) {
                    sequence(0 until 4, arrayOf(5, 5, 5, 5))
                }
                toFrame(4)
                pauseFor(80)
                loop(20) {
                    sequence(4 until 8, arrayOf(5, 5, 5, 5))
                }
                pauseFor(80)
            }
        }

        val SOLIDIFIER = Animation("block/solidifier", 4, true, true) {
            loop {
                sequence(0 until 4, arrayOf(10, 10, 10, 10))
            }
        }

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}

class AnimationSerializer(type: Class<Animation>, settings: List<SerializerSetting<*>>) :
    Serializer<Animation>(type, settings) {


    override val writeStrategy = object : WriteStrategy<Animation>(type, settings) {
        override fun write(obj: Animation, output: Output) {
            output.writeInt(obj.id)
            output.writeBoolean(obj.isLocal)
        }
    }

    override val createStrategy = object : CreateStrategy<Animation>(type, settings) {
        override fun create(input: Input): Animation {
            val id = input.readInt()
            val isCopy = input.readBoolean()
            return Animation.ALL.first { it.id == id }.let { if (isCopy) it.createLocalInstance() else it }
        }
    }
}