package graphics

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import main.heightPixels
import main.toColor
import main.widthPixels
import misc.PixelCoord

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
}

class Frame(val fromFrameIndex: Int, val toFrameIndex: Int, ticks: Int = 0, stepID: String? = "frame $toFrameIndex") : AnimationStep(stepID) {

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

    override fun toString() = "Frame step, index: $fromFrameIndex -> $toFrameIndex, progress: ${pause.currentTicks}/${pause.ticks}"
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
            } else if (numberOfTimes == -1) {
                stepChain.reset()
            }
        }
        return false
    }

    override fun reset() {
        currentNumberOfTimes = 0
        stepChain.reset()
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
}

class Stop(stepID: String?) : AnimationStep(stepID) {

    override val progress get() = 1f

    override fun execute(context: StepChain, animation: Animation): Boolean {
        animation.playing = false
        return true
    }

    override fun reset() {
    }
}

class Reverse(stepID: String?) : AnimationStep(stepID) {
    override val progress get() = 1f

    override fun execute(context: StepChain, animation: Animation): Boolean {
        context.reversed = true
        return true
    }

    override fun reset() {}

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
}

class Animation(path: String,
                numberOfFrames: Int,
                startPlaying: Boolean = false,
                @Tag(1)
                val smoothing: Boolean = false,
                @Tag(2)
                val offsets: List<PixelCoord> = listOf(),
                closure: StepChain.() -> Unit = {}) : Renderable() {

    private constructor() : this("misc/error", 1)

    @Tag(3)
    val frames = ImageCollection(path, numberOfFrames)

    @Tag(4)
    var currentFrameIndex = 0
        set(value) {
            lastFrame = currentFrame
            field = value
        }

    override val xPixelOffset get() = if (currentFrameIndex > offsets.lastIndex) 0 else offsets[currentFrameIndex].xPixel
    override val yPixelOffset get() = if (currentFrameIndex > offsets.lastIndex) 0 else offsets[currentFrameIndex].yPixel
    val currentFrame get() = frames[currentFrameIndex]
    @Tag(5)
    private var lastFrame = frames[0]

    override val widthPixels get() = currentFrame.widthPixels
    override val heightPixels get() = currentFrame.heightPixels


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

    override fun render(xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, keepAspect: Boolean, params: TextureRenderParams) {
        if (smoothing) {
            if (keepAspect) {
                Renderer.renderTextureKeepAspect(lastFrame, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels)
                Renderer.renderTextureKeepAspect(currentFrame, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels, TextureRenderParams(color = toColor(alpha = currentStepProgress)).combine(params))
            } else {
                Renderer.renderTexture(lastFrame, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels)
                Renderer.renderTexture(currentFrame, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels, TextureRenderParams(color = toColor(alpha = currentStepProgress)).combine(params))
            }
        } else {
            if (keepAspect) {
                Renderer.renderTextureKeepAspect(currentFrame, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels, params)
            } else {
                Renderer.renderTexture(currentFrame, xPixel + xPixelOffset, yPixel + yPixelOffset, widthPixels, heightPixels, params)
            }
        }
    }

    /**
     * @return the [com.badlogic.gdx.graphics.g2d.TextureRegion] at the given index. This is not the same thing as a step!
     */
    operator fun get(i: Int) = frames[i]

    companion object {

        val ALL = mutableListOf<Animation>()

        val MAIN_MENU_PLAY_BUTTON_FLASH = Animation("gui/flash", 9, smoothing = true) {
            //toFrame(0, 5)
            //sequence(0..9, arrayOf(5, 5, 5, 5, 5, 5, 5, 5, 5, 5))
        }

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

        val MINER = Animation("block/miner", 5, true, true) {
            loop {
                sequence(0 until 5, arrayOf(5, 5, 5, 5, 5))
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