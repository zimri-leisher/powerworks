package behavior.leaves

import level.entity.Entity
import behavior.*
import misc.Geometry
import misc.Numbers
import misc.PixelCoord
import misc.TileCoord
import java.lang.Math.floor

class MoveTo(parent: BehaviorTree, val goalVar: Variable,
             val goalThreshold: Int = 5,
             val failAfter: Int = -1,
             val axisThreshold: Int = 3) : Leaf(parent) {

    lateinit var goal: PixelCoord

    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        if (failAfter != -1) {
            setData(DefaultVariable.MOVE_TO_TICKS_MOVING, 0)
        }
        val nullableGoal = getData<Any?>(goalVar)
        if (nullableGoal == null) {
            state = NodeState.FAILURE
            return
        }
        if(nullableGoal is TileCoord) {
            goal = nullableGoal.toPixel()
        } else if(nullableGoal is PixelCoord){
            goal = nullableGoal
        }
    }

    override fun updateState(entity: Entity) {
        val nullableGoal = getData<Any?>(goalVar)
        if (nullableGoal == null) {
            state = NodeState.FAILURE
            return
        }
        if(nullableGoal is TileCoord) {
            goal = nullableGoal.toPixel()
        } else if(nullableGoal is PixelCoord) {
            goal = nullableGoal
        }
        val distance = Geometry.distance(entity.xPixel, entity.yPixel, goal.xPixel, goal.yPixel)
        if (distance <= goalThreshold) {
            state = NodeState.SUCCESS
        } else if (failAfter != -1) {
            val ticksMoving: Int = getData(DefaultVariable.MOVE_TO_TICKS_MOVING)!!
            if (ticksMoving >= failAfter) {
                state = NodeState.FAILURE
            }
        } else {
            state = NodeState.RUNNING
        }
    }

    override fun execute(entity: Entity) {
        if (parent.hasPriority(entity)) {
            val xDist = goal.xPixel - entity.xPixel
            val yDist = goal.yPixel - entity.yPixel
            val xSign = Numbers.sign(xDist)
            val ySign = Numbers.sign(yDist)
            entity.xVel += xSign
            entity.yVel += ySign
            if (failAfter != -1) {
                val ticksMoving: Int = getData(DefaultVariable.MOVE_TO_TICKS_MOVING)!!
                setData(DefaultVariable.MOVE_TO_TICKS_MOVING, ticksMoving + 1)
            }
        }
    }

    override fun toString() = "MoveTo: (goalVar: $goalVar, goalThreshold: $goalThreshold, failAfter: $failAfter, axisThreshold: $axisThreshold)"
}