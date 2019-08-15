package level.living.behavior

import level.living.LivingObject
import misc.Geometry
import misc.Numbers
import misc.PixelCoord

class LivingObjectBehavior(val parent: LivingObject) {

    var currentPositionGoal: PixelCoord? = null

    fun moveTo(xPixel: Int, yPixel: Int) {
        currentPositionGoal = PixelCoord(xPixel, yPixel)
    }

    fun patrol(centerXPixel: Int, centerYPixel: Int, radius: Int) {

    }

    fun stop() {
        currentPositionGoal = null
    }

    fun update() {
        if(currentPositionGoal != null) {
            val goal = currentPositionGoal!!
            val xDist = goal.xPixel - parent.xPixel
            val yDist = goal.yPixel - parent.yPixel
            val xSign = Numbers.sign(if(Math.abs(xDist) < 3) 0 else xDist)
            val ySign = Numbers.sign(if(Math.abs(yDist) < 3) 0 else yDist)
            parent.xVel += xSign
            parent.yVel += ySign
            if(Geometry.distance(goal.xPixel, goal.yPixel, parent.xPixel, parent.yPixel) < 5) {
                currentPositionGoal = null
            }
        }
    }

    companion object {
        val DISTANCE_BETWEEN_NEAREST_TO_MAINTAIN = 20
    }
}