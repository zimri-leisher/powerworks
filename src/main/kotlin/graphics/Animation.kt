package graphics

import com.badlogic.gdx.graphics.g2d.TextureRegion

sealed class AnimationStep(val stepID: String?) {

    /**
     * This function is called once per tick while this step is active. The function may choose to never return true
     * and continue holding the animation, or it could return false and the next animation step would start executing.
     * Immediately after this function passes execution (returns true), the reset method of this will be called by the
     * StepChain class
     *
     * @param context the animation this step is part of.
     * This is used inside the AnimationStep children to change frames, ticks, whether or not the animation is playing and so on
     *
     * @return whether to go on to the next step or not
     */
    abstract fun execute(context: Animation): Boolean

    /**
     * Prepares this class for another loop
     */
    abstract fun reset()
}

class Frame(val frameIndex: Int, ticks: Int = 0, stepID: String? = "frame $frameIndex") : AnimationStep(stepID) {

    val pause = Pause(ticks, "")

    override fun execute(context: Animation): Boolean {
        if (pause.execute(context)) {
            context.currentFrame = frameIndex
            return true
        }
        return false
    }

    override fun reset() {
        pause.reset()
    }

}

class Pause(val ticks: Int, stepID: String?) : AnimationStep(stepID) {

    var currentTicks = 0

    override fun execute(context: Animation): Boolean {
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

    override fun execute(context: Animation): Boolean {
        if (stepChain.execute(context)) {
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
    override fun execute(context: Animation): Boolean {
        context.steps.currentStep = context.steps.findIndex(stepIDToGoTo)
        return true
    }

    override fun reset() {
    }
}

class Stop(stepID: String?) : AnimationStep(stepID) {
    override fun execute(context: Animation): Boolean {
        context.playing = false
        return true
    }

    override fun reset() {
    }
}

class StepChain(stepID: String? = null, val closure: StepChain.() -> Unit) : AnimationStep(stepID) {

    val steps = mutableListOf<AnimationStep>()
    var currentStep = 0
    var reversed = false

    init {
        closure()
    }

    override fun execute(context: Animation): Boolean {
        if (steps[currentStep].execute(context)) {
            steps[currentStep].reset()
            if (!reversed) {
                if (currentStep == steps.lastIndex) {
                    return true
                } else {
                    currentStep++
                }
            } else {
                if (currentStep == 0) {
                    return true
                } else {
                    currentStep--
                }
            }
        }
        return false
    }

    override fun reset() {
        currentStep = 0
        steps.forEach { it.reset() }
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
     * Adds a sequence of toFrame steps defined by the frameRange parameter with the given times
     * @param frameRange the range of frames (e.g. 0..10 or 5 until 7)
     * @param times the ticks parameter to pass to the toFrame step for the given step (denoted by index, if this is index
     * 2 in the array it will set the second frame of this sequence to the ticks in that index)
     * @param stepID the identifier that will be given to the first frame in this sequence. You can use this with the goToStep step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use goToStep to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     */
    fun sequence(frameRange: IntRange, times: Array<Int>, stepID: String? = null) {
        for (i in frameRange) {
            toFrame(i, times[i - frameRange.first], if (i == frameRange.first) stepID else null)
        }
    }

    /**
     * Adds a step telling the animation to go to the next frame after a period of ticks
     * @param ticks the number of ticks to wait on this frame before going on to the next step
     * @param stepID the identifier of this frame. You can use this with the goToStep step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use goToStep to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     */
    fun toFrame(frame: Int, ticks: Int = 0, stepID: String? = null) {
        steps.add(Frame(frame, ticks, stepID))
    }

    /**
     * Adds a step that pauses execution of the next step until a period of ticks
     * @param ticks the number of ticks to wait before going on to the next step
     * @param stepID the identifier of this step. You can use this with the goToStep step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use goToStep to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     */
    fun pauseFor(ticks: Int, stepID: String? = null) {
        steps.add(Pause(ticks, stepID))
    }

    /**
     * Adds a step that moves execution to the specified stepID
     * @param stepID the identifier of this step. You can use this with the goToStep step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use goToStep to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     */
    fun goToStep(stepIDToGoTo: String, stepID: String? = null) {
        steps.add(GoToStep(stepIDToGoTo, stepID))
    }

    /**
     * Adds a step that will execute steps inside the closure a specified number of times, or forever
     * @param numberOfTimes the number of times to go through the steps in the closure. If -1, it will loop forever
     * @param stepID the identifier of this step. You can use this with the goToStep step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use goToStep to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     * @param closure the execution context for the inside of the loop. You can use all the same methods for creating steps,
     * including this one!
     */
    fun loop(numberOfTimes: Int = -1, stepID: String? = null, closure: StepChain.() -> Unit) {
        steps.add(Loop(numberOfTimes, stepID, closure))
    }

    /**
     * Adds a step that will execute multiple, other steps together
     * @param stepID the identifier of this step. You can use this with the goToStep step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use goToStep to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     * @param closure the execution context for the steps you want to run. You can use all the same methods for creating steps,
     * including this one!
     */
    fun chain(stepID: String? = null, closure: StepChain.() -> Unit) {
        steps.add(StepChain(stepID, closure))
    }

    /**
     * Adds a step that will stop execution. Please note the difference between stopping the animation and stopping
     * the execution--stopping execution doesn't reset frames or steps. It merely keeps execution exactly where it is.
     * @param stepID the identifier of this step. You can use this with the goToStep step to move backwards or forwards
     * in the execution order to this one. This identifier only works inside of the closure it's created in.
     * What this means is if you give a step inside of a loop a step ID, you won't be able to navigate to that step
     * from outside of the loop. Simply put, you cannot use goToStep to move inside or outside of the brackets it was created
     * in. If multiple steps have the same ID, the first one with that ID will be selected. If null, this step has no ID
     */
    fun stop(stepID: String? = null) {
        steps.add(Stop(stepID))
    }
}

class Animation(path: String, val numberOfFrames: Int, startPlaying: Boolean = false, closure: StepChain.() -> Unit) {

    // TODO add local animations

    val frames = ImageCollection(path, numberOfFrames)
    var currentFrame = 0
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
     * it forwards. The direction will not change unless playBackwards or playBackwardsFrom is called, regardless of if
     * it's stopped or started
     */
    fun play() {
        playing = true
        steps.reversed = false
    }

    /**
     * Starts an animation where it last left off (if this hasn't been played before, that's at the beginning) and plays
     * it backwards. The direction will not change unless playBackwards or playBackwardsFrom is called, regardless of if
     * it's stopped or started
     *
     * Playing an animation backwards simply reverses the order of the steps. It does not reverse the direction Pause steps
     * tick in, meaning if you reverse the animation in the middle of a Pause's execution period it will continue normally
     * until the end of that Pause
     */
    fun playBackwards() {
        playing = true
        steps.reversed = true
    }

    /**
     * Plays an animation forwards from a given stepID. The stepID must be assigned to something inside the first level of closure,
     * otherwise it will not be found. The direction will not change unless playBackwards or playBackwardsFrom is called,
     * regardless of if it's stopped or started
     */
    fun playFrom(stepID: String) {
        val index = steps.findIndex(stepID)
        if (index == -1) {
            throw IllegalArgumentException("stepID $stepID does not exist")
        }
        playFrom(index)
    }

    /**
     * Plays an animation forwards from the given step index. The direction will not change unless playBackwards or
     * playBackwardsFrom is called, regardless of if it's stopped or started
     */
    fun playFrom(index: Int) {
        steps.currentStep = index
        play()
    }

    /**
     * Plays an animation from a given stepID. The stepID must be assigned to something inside the first level of closure,
     * otherwise it will not be found.
     *
     * Playing an animation backwards simply reverses the order of the steps. It does not reverse the direction Pause steps
     * tick in, meaning if you reverse the animation in the middle of a Pause's execution period it will continue normally
     * until the end of that Pause
     */
    fun playBackwardsFrom(stepID: String) {
        val index = steps.findIndex(stepID)
        if (index == -1) {
            throw IllegalArgumentException("stepID $stepID does not exist")
        }
        playBackwardsFrom(index)
    }

    /**
     * Plays an animation backwards from the given step index. The direction will not change unless playBackwards or
     * playBackwardsFrom is called, regardless of if it's stopped or started.
     *
     * Playing an animation backwards simply reverses the order of the steps. It does not reverse the direction Pause steps
     * tick in, meaning if you reverse the animation in the middle of a Pause's execution period it will continue normally
     * until the end of that Pause
     */
    fun playBackwardsFrom(index: Int) {
        steps.currentStep = index
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
        steps.currentStep = 0
        steps.reset()
    }

    /**
     * @return the stepID of the current lowest level step
     */
    fun getCurrentStepID() = steps.steps[steps.currentStep].stepID

    fun update() {
        if (playing) {
            if (steps.execute(this)) {
                playing = false
            }
        }
    }

    /**
     * @return the TextureRegion at the given index. This is not the same thing as a step!
     */
    operator fun get(i: Int) = frames[i]

    companion object {

        val ALL = mutableListOf<Animation>()

        val MAIN_MENU_PLAY_BUTTON = Animation("gui/play_button", 7) {
            toFrame(1, 5)
            toFrame(2, 8)
            toFrame(3, 14)
            toFrame(4, 17, "outside_loop")
            loop(stepID = "inside_loop") {
                toFrame(5, 15)
                toFrame(6, 15)
            }
        }
        val MINER = Animation("block/miner", 5, true) {
            sequence(0 until 5, arrayOf(5, 5, 5, 5, 5))
        }

        val SOLIDIFIER = Animation("block/solidifier", 4, true) {
            sequence(0 until 4, arrayOf(10, 10, 10, 10))
        }

        fun update() {
            ALL.forEach { it.update() }
        }
    }
}