package behavior.leaves

import level.entity.Entity
import behavior.*
import level.LevelManager
import main.Game
import misc.PixelCoord

/**
 * Gets a random position from the level based on the given coordinates and [radius], and then puts it into the appropriate
 * data map based on [generalData]
 * @param key the key to put the data into
 * @param xCenter the center x pixel coordinate which the bounds circle with radius [radius] is based in
 * @param yCenter the center x pixel coordinate which the bounds circle with radius [radius] is based in
 * @param radius the radius of the circle to generate the random point in
 */
class GetRandomPosition(parent: BehaviorTree, val dest: Variable, val xCenter: Int, val yCenter: Int, val radius: Int) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        val xPixel = if (xCenter == -1)
            entity.level.rand.nextInt(entity.level.widthPixels)
        else
            xCenter + (entity.level.rand.nextInt(radius * 2) - radius)
        val yPixel = if (yCenter == -1)
            entity.level.rand.nextInt(entity.level.heightPixels)
        else
            yCenter + (entity.level.rand.nextInt(radius * 2) - radius)
        setData(dest, PixelCoord(xPixel, yPixel).enforceBounds(entity.level))
        return true
    }

    override fun toString() = "GetRandomPosition: (dest: $dest, xCenter: $xCenter, yCenter: $yCenter, radius: $radius)"
}