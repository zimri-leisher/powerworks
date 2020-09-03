package level.entity

import behavior.BehaviorTree
import behavior.DefaultVariable
import behavior.leaves.EntityPath
import level.LevelObject
import level.LevelPosition
import level.update.EntityFireWeapon
import level.update.EntityPathUpdate
import main.Game
import misc.Coord
import network.MovingObjectReference
import serialization.Id
import kotlin.math.*

/**
 * A class for managing the behavior of an [Entity]. Stores currently running [BehaviorTree]s, [EntityPath] and [attack] information.
 *
 */
class EntityBehavior(
        @Id(0)
        private val parent: Entity) {

    private constructor() : this(DefaultEntity(EntityType.ERROR, 0, 0))

    // TODO make these not transient
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
    var attackTarget: LevelObject? = null

    @Id(10)
    var goalPosition: LevelPosition? = null

    @Id(11)
    private var movingToGoalPosition = false

    fun update() {
        traversing = true
        running.forEach { it.key.update(parent) }
        traversing = false
        running.putAll(_toAdd)
        _toAdd.clear()
        _toRemove.forEach { running.remove(it) }
        _toRemove.clear()

        moveToFollowPath()
        attack()
    }

    private fun attack() {
        if (attackTarget != null && parent.weapon != null) {
            val target = attackTarget!!
            if (target.level != parent.level || !target.inLevel) {
                this.attackTarget = null
                return
            }
            if (parent.weapon?.canFire == true) {
                val xDiff = target.x + target.hitbox.xStart + target.hitbox.width / 2 - parent.x
                val yDiff = target.y + target.hitbox.yStart + target.hitbox.height / 2 - parent.y
                val angle = atan2(yDiff.toFloat(), xDiff.toFloat())
                parent.level.modify(EntityFireWeapon(Coord(parent.x, parent.y), angle, parent.weapon!!.type.projectileType, parent.toReference() as MovingObjectReference))
            }
        }
    }

    private fun moveToFollowPath() {
        if (path != null && currentPathStepIndex < path!!.steps.size) {
            val path = path!!
            if (parent.level != path.goal.level || !parent.inLevel) {
                stopFollowingPath()
                return
            }
            val currentStep = path.steps[currentPathStepIndex]
            moveToPoint(currentStep.x, currentStep.y)
            val dist = distanceToPoint(currentStep.x, currentStep.y)
            if (dist < 1 && timesReachedStep[currentPathStepIndex] == null) { // if we just reached this for the first time
                // reached step
                timesReachedStep[currentPathStepIndex] = timeSincePathingStart
                parent.level.modify(EntityPathUpdate(parent.toReference() as MovingObjectReference, currentPathStepIndex, timeSincePathingStart, path.hashCode()))
                currentPathStepIndex++
            }
            timeSincePathingStart++
        } else if (goalPosition != null) {
            val goalPosition = goalPosition!!
            val dist = distanceToPoint(goalPosition.x, goalPosition.y)
            if(dist > 16) {
                movingToGoalPosition = true
            } else if(dist <= 1) {
                movingToGoalPosition = false
            }
            if(movingToGoalPosition) {
                moveToPoint(goalPosition.x, goalPosition.y)
            }
        }
    }

    private fun distanceToPoint(x: Int, y: Int): Double {
        val xDist = x - parent.x
        val yDist = y - parent.y
        return sqrt(Math.pow(xDist.toDouble(), 2.0) + Math.pow(yDist.toDouble(), 2.0))
    }

    private fun moveToPoint(x: Int, y: Int) {
        val xDist = x - parent.x
        val yDist = y - parent.y
        val angle = atan2(yDist.toDouble(), xDist.toDouble())
        val speed = parent.type.moveSpeed * 16 / Game.UPDATES_PER_SECOND // convert from tiles per second to units per tick
        parent.xVel += cos(angle) * speed
        parent.yVel += sin(angle) * speed
    }

    private fun getEstimatedTimeToStep(index: Int): Int {
        val path = path!!
        val step = path.steps[index]
        val xDist = step.x - parent.x
        val yDist = step.y - parent.y
        val dist = sqrt(Math.pow(xDist.toDouble(), 2.0) + Math.pow(yDist.toDouble(), 2.0))
        val speed = parent.type.moveSpeed * 16 / Game.UPDATES_PER_SECOND // convert from tiles per second to units per tick
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
                    parent.setPosition(step.x, step.y)
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