package behavior.leaves

import behavior.BehaviorTree
import behavior.DataLeaf
import behavior.Variable
import level.entity.Entity
import misc.PixelCoord
import misc.TileCoord

class CreateFormationAround(parent: BehaviorTree, val around: Variable, val padding: Int = 32) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        if (entity.group == null) {
            return false
        }
        val aroundPos = getData<Any>(around) ?: return false
        val actualAroundPos =
                if (aroundPos is TileCoord) aroundPos.pixel()
                else if (aroundPos is PixelCoord) aroundPos
                else return false
        entity.group!!.createFormationAround(actualAroundPos.xPixel, actualAroundPos.yPixel, padding)
        return true
    }
}