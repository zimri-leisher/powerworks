package behavior.leaves

import level.entity.Entity
import behavior.*
import misc.Coord
import java.util.*

/**
 * Gets a random position from the level based on the given coordinates and [radius], and then puts it into the appropriate
 * data map based on [generalData]
 * @param key the key to put the data into
 * @param xCenter the center x coordinate which the bounds circle with radius [radius] is based in
 * @param yCenter the center y coordinate which the bounds circle with radius [radius] is based in
 * @param radius the radius of the circle to generate the random point in
 */
class GetRandomPosition(parent: BehaviorTree, val dest: Variable, val xCenter: Int, val yCenter: Int, val radius: Int) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        val rand = Random(entity.level.info.seed)
        val x = if (xCenter == -1)
            rand.nextInt(entity.level.width)
        else
            xCenter + (rand.nextInt(radius * 2) - radius)
        val y = if (yCenter == -1)
            rand.nextInt(entity.level.height)
        else
            yCenter + (rand.nextInt(radius * 2) - radius)
        setData(dest, Coord(x, y).enforceBounds(entity.level))
        return true
    }

    override fun toString() = "GetRandomPosition: (dest: $dest, xCenter: $xCenter, yCenter: $yCenter, radius: $radius)"
}