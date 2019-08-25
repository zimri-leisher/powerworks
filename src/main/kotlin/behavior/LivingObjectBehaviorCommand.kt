package behavior

// import level.LevelObject
// import level.living.Entity
// import main.Game
// import misc.Geometry
// import misc.Numbers
// import misc.PixelCoord
// import kotlin.math.atan
// import kotlin.math.cos
// import kotlin.math.sin
//
//
// sealed class LivingObjectBehaviorCommand2(val priority: Int = Integer.MAX_VALUE, val living: Entity) {
// var finished = false
//
// abstract fun update()
//
// abstract fun checkIfFinished()
// }
//
// class MoveBehaviorCommand(living: Entity, val goal: PixelCoord) : LivingObjectBehaviorCommand2(2, living) {
// override fun update() {
// val xDist = goal.xPixel - living.xPixel
// val yDist = goal.yPixel - living.yPixel
// val xSign = Numbers.sign(if (Math.abs(xDist) < AXIS_THRESHOLD) 0 else xDist)
// val ySign = Numbers.sign(if (Math.abs(yDist) < AXIS_THRESHOLD) 0 else yDist)
// living.xVel += xSign
// living.yVel += ySign
// }
//
// override fun checkIfFinished() {
// if (Geometry.distance(goal.xPixel, goal.yPixel, living.xPixel, living.yPixel) < COMPLETION_THRESHOLD) {
// finished = true
// }
// }
//
// companion object {
// const val COMPLETION_THRESHOLD = 5
// const val AXIS_THRESHOLD = 3
// }
// }
//
// class AttackBehaviorCommand(living: Entity, val goal: LevelObject) : LivingObjectBehaviorCommand2(3, living) {
// override fun update() {
// val xDist = goal.xPixel - living.xPixel
// val yDist = goal.yPixel - living.yPixel
// val slope = yDist.toDouble() / xDist.toDouble()
// val angle = atan(slope)
// // yeah im like 70 percent sure that this is just a work around and im forgetting how to actually do this
// // but aaaa who cares this will work 100 percent
// val xRatio = Math.abs(cos(angle)) * Numbers.sign(xDist)
// val yRatio = Math.abs(sin(angle)) * Numbers.sign(yDist)
//
// val distance = Geometry.distance(goal.xPixel, goal.yPixel, living.xPixel, living.yPixel)
// if (distance < RETREAT_THRESHOLD) {
// living.xVel -= Math.ceil(xRatio * living.type.moveSpeed).toInt()
// living.yVel -= Math.ceil(yRatio * living.type.moveSpeed).toInt()
// } else if(distance > APPROACH_THRESHOLD) {
// living.xVel += Math.ceil(xRatio * living.type.moveSpeed).toInt()
// living.yVel += Math.ceil(yRatio * living.type.moveSpeed).toInt()
// }
// }
//
// override fun checkIfFinished() {
// if(!goal.inLevel) {
// finished = true
// }
// }
//
// companion object {
// val RETREAT_THRESHOLD = 20
// val APPROACH_THRESHOLD = 30
// val AXIS_THRESHOLD = 3
// }
// }
//
// enum class LivingObjectBehaviorCommand(val priority: Int, val execute: (livingObjects: List<Entity>) -> Unit) {
// MOVE(2, { livingObjects ->
// livingObjects.forEach { it.behavior.moveTo(Game.currentLevel.mouseLevelXPixel, Game.currentLevel.mouseLevelYPixel) }
// }),
// ATTACK(3, { livingObjects ->
//
// }),
// DEFEND(1, { livingObjects ->
//
// }),
// STOP(0, { livingObjects ->
// livingObjects.forEach { it.behavior.stop() }
// })
// }