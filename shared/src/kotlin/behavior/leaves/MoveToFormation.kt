package behavior.leaves

import behavior.BehaviorTree
import behavior.Leaf
import behavior.NodeState
import behavior.Variable
import level.entity.Entity
import misc.Numbers
import misc.PixelCoord
import kotlin.math.ceil
import kotlin.math.sqrt

class MoveToFormation(parent: BehaviorTree, val formationCenter: Variable, val formationIndex: Variable) : Leaf(parent) {

    lateinit var bottom: PixelCoord
    var index = 0

    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        val nullableCenter: PixelCoord? = getData(formationCenter)
        if (nullableCenter == null) {
            state = NodeState.FAILURE
            return
        }
        index = getData(formationIndex) ?: 0
    }

    override fun updateState(entity: Entity) {
    }

    override fun execute(entity: Entity) {
    }
}

data class Formation(val coordinates: Array<Array<PixelCoord?>>, val originXPixel: Int, val originYPixel: Int, val spacing: Int) {

    companion object {
        /*
        fun make(centerXPixel: Int, centerYPixel: Int, spacing: Int, entities: List<Entity>): Formation {
            // ok so basically we need to make something as close to a square as possible with w/h that multiply to the
            // size of the entity list
            var side = ceil(Numbers.sqrt(entities.size)).toInt()
            var largestHitboxWidth = entities.maxBy { it.hitbox.width }?.hitbox?.width ?: 0
            var largestHitboxHeight = entities.maxBy { it.hitbox.height }?.hitbox?.height ?: 0
            // just assume they're all the same hitbox (the largest)
        }
         */
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Formation

        if (!coordinates.contentDeepEquals(other.coordinates)) return false
        if (originXPixel != other.originXPixel) return false
        if (originYPixel != other.originYPixel) return false
        if (spacing != other.spacing) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coordinates.contentDeepHashCode()
        result = 31 * result + originXPixel
        result = 31 * result + originYPixel
        result = 31 * result + spacing
        return result
    }
}