package behavior.leaves

import level.entity.Entity
import behavior.BehaviorTree
import behavior.DataLeaf
import behavior.Variable
import level.*
import level.moving.MovingObject
import main.Game
import misc.Geometry
import misc.Numbers

/**
 * Stores the nearest [LevelObject] to the nearest [Entity] for which this is being executed in the [BehaviorTree.data]
 * map. The object must match the given [predicate]
 */
class GetNearestLevelObject(parent: BehaviorTree, val dest: Variable, val predicate: (LevelObject) -> Boolean) :
        DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        var radius = 16
        // while the radius doesnt cover the whole level
        // this is going to be a binary search type thing. kind of. in that it's related to powers of two
        val previouslyChecked = mutableListOf<MovingObject>()
        var lowestDistance = Double.MAX_VALUE
        var closestObject: LevelObject? = null
        while(radius < Numbers.max(entity.level.widthPixels, entity.level.heightPixels) / 2)  {
            radius *= 2
            val inRadius: Set<LevelObject> = entity.level.getMovingObjectCollisionsInSquareCenteredOn(entity.xPixel, entity.yPixel, radius, predicate) +
                    entity.level.getBlockCollisionsInSquareCenteredOn(entity.xPixel, entity.yPixel, radius, predicate)
            for(levelObject in inRadius) {
                if(levelObject !in previouslyChecked && levelObject != entity) {
                    val h1 = entity.hitbox
                    val x1 = h1.xStart + entity.xPixel
                    val y1 = h1.xStart + entity.yPixel
                    val h2 = levelObject.hitbox
                    val x2 = h2.xStart + levelObject.xPixel
                    val y2 = h2.yStart + levelObject.yPixel
                    val distance = Geometry.distance(x1, y1, x1 + h1.width, y1 + h1.height,
                            x2, y2, x2 + h2.width, y2 + h2.height)
                    if(distance < lowestDistance) {
                        lowestDistance = distance
                        closestObject = levelObject
                    }
                }
            }
            if(closestObject != null) {
                break
            }
        }
        setData(dest, closestObject)
        println("found a level object $closestObject")
        return closestObject != null
    }

    override fun toString() = "GetNearestLevelObject: (dest: $dest, predicate: $predicate)"
}