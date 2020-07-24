package level.entity

import behavior.BehaviorTree
import behavior.DefaultVariable
import behavior.leaves.EntityPath
import level.LevelObject
import level.UpdateEntityPathPosition
import main.Game
import network.MovingObjectReference
import serialization.Id
import kotlin.math.*

class EntityBehavior(
        @Id(0)
        private val parent: Entity) {

    private constructor() : this(DefaultEntity(EntityType.ERROR, 0, 0))

    // ignore these all. client side only for now.
    val running = mutableMapOf<BehaviorTree, Int>()

    private val _toAdd = mutableMapOf<BehaviorTree, Int>()

    private val _toRemove = mutableListOf<BehaviorTree>()

    private var traversing = false

    @Id(5)
    var path: EntityPath? = null

    @Id(6)
    private var currentPathStepIndex = -1

    @Id(7)
    private var timeSincePathingStart = -1

    @Id(8)
    private var timesReachedStep = arrayOfNulls<Int>(0)

    @Id(9)
    var target: LevelObject? = null

    fun update() {
        traversing = true
        running.forEach { it.key.update(parent) }
        traversing = false
        running.putAll(_toAdd)
        _toAdd.clear()
        _toRemove.forEach { running.remove(it) }
        _toRemove.clear()

        moveToFollowPath()
        attackTarget()
    }

    private fun attackTarget() {
        if (target != null && parent.weapon != null) {
            val target = target!!
            if (target.level != parent.level || !target.inLevel) {
                this.target = null
                println("target not in same level")
                return
            }
            val xDiff = target.xPixel - parent.xPixel
            val yDiff = target.yPixel - parent.yPixel
            val angle = atan2(yDiff.toFloat(), xDiff.toFloat())
            parent.weapon!!.tryFire(angle)
        }
    }

    private fun moveToFollowPath() {
        if (path != null && currentPathStepIndex < path!!.steps.size) {
            val path = path!!
            val currentStep = path.steps[currentPathStepIndex]
            val xDist = currentStep.xPixel - parent.xPixel
            val yDist = currentStep.yPixel - parent.yPixel
            val angle = atan2(yDist.toDouble(), xDist.toDouble())
            val speed = parent.type.moveSpeed * 16 / 60 // convert from tiles per second to pixels per tick
            parent.xVel += cos(angle) * speed
            parent.yVel += sin(angle) * speed
            val dist = sqrt(Math.pow(xDist.toDouble(), 2.0) + Math.pow(yDist.toDouble(), 2.0))
            if (dist < 1 && timesReachedStep[currentPathStepIndex] == null) { // if we just reached this for the first time
                // reached step
                timesReachedStep[currentPathStepIndex] = timeSincePathingStart
                parent.level.modify(UpdateEntityPathPosition(parent.toReference() as MovingObjectReference, currentPathStepIndex, timeSincePathingStart, path.hashCode()))
                currentPathStepIndex++
            }
            timeSincePathingStart++
        }
    }

    fun getEstimatedTimeToStep(index: Int): Int {
        val path = path!!
        val step = path.steps[index]
        val xDist = step.xPixel - parent.xPixel
        val yDist = step.yPixel - parent.yPixel
        val dist = sqrt(Math.pow(xDist.toDouble(), 2.0) + Math.pow(yDist.toDouble(), 2.0))
        val speed = parent.type.moveSpeed * 16 / Game.UPDATES_PER_SECOND // convert from tiles per second to pixels per tick
        return (dist / speed).toInt() // approximate number of ticks
    }

    fun isFollowingPath() = path != null && currentPathStepIndex < path!!.steps.size // when it's equal to size, we've finished

    fun follow(path: EntityPath) {
        currentPathStepIndex = 0
        this.path = path
        timeSincePathingStart = 0
        timesReachedStep = arrayOfNulls(path.steps.size)
    }

    fun shouldBeAtStepAtTime(index: Int, time: Int) {
        if (path != null) {
            val timeReachedStep = timesReachedStep[index]
            if (timeReachedStep == null) {
                //println("server before client")
                // we haven't reached this step at all yet.
                // will we reach there on time?
                val estimatedTime = getEstimatedTimeToStep(index)
                //println("current info: $timeSincePathingStart + $estimatedTime (approx) versus $time")
                if ((timeSincePathingStart + estimatedTime - time).absoluteValue <= 1) {
                    // if we're within 1 tick of probably getting there on time
                    //println("expected time is OK")
                } else {
                    val step = path!!.steps[index]
                    timeSincePathingStart = time // synchronize time
                    timesReachedStep[index] = timeSincePathingStart
                    parent.setPosition(step.xPixel, step.yPixel)
                    currentPathStepIndex = index + 1
                }
            } else {
                //println("client before server")
                if (timeReachedStep == time) {
                    //println("on track!")
                    // we're good. don't do anything
                } else if (timeReachedStep < time) {
                    //println("too fast by ${time - timeReachedStep}")
                    // we reached the step too early -> we're going too fast
                } else {
                    //println("too slow by ${timeReachedStep - time}")
                    // we reached the step too late -> we're going too slow
                }
            }
        }
    }

    fun stopFollowingPath() {
        timeSincePathingStart = -1
        currentPathStepIndex = -1
        timesReachedStep = arrayOfNulls(0)
        path = null
    }

    fun setPriority(behavior: BehaviorTree, priority: Int) {
        running[behavior] = priority
    }

    fun getPriority(behavior: BehaviorTree) = running.filterKeys { it == behavior }.values.firstOrNull() ?: -1

    fun finish(behavior: BehaviorTree) {
        if (traversing) {
            _toRemove.add(behavior)
        } else {
            running.remove(behavior)
        }
    }

    fun run(behavior: BehaviorTree, priority: Int = 0, argument: Any? = null) {
        if (behavior.hasBeenInitialized(parent)) {
            behavior.reset(parent)
        }
        if (traversing) {
            _toAdd.put(behavior, priority)
        } else {
            running.put(behavior, priority)
        }
        if (argument != null) {
            behavior.data.set(name = DefaultVariable.ARGUMENT.name, value = argument)
        }
        behavior.init(parent)
    }
}