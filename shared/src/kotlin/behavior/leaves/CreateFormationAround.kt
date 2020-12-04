package behavior.leaves

import behavior.BehaviorTree
import behavior.DataLeaf
import behavior.Variable
import level.LevelPosition
import level.entity.Entity
import misc.Coord
import misc.TileCoord

class CreateFormationAround(parent: BehaviorTree, val around: Variable, val padding: Int = 32) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        if (entity.group == null) {
            return false
        }
        val aroundPos = getData<Any>(around) ?: return false
        if(aroundPos !is LevelPosition) {
            return false
        }
        entity.group!!.createFormationAround(aroundPos.x, aroundPos.y, padding)
        return true
    }
}