package behavior.leaves

import behavior.BehaviorTree
import behavior.DataLeaf
import behavior.Variable
import level.entity.Entity

class GetCenterOfGroup(parent: BehaviorTree, val destVar: Variable) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        if (entity.group == null) {
            return false
        }
        setData(destVar, entity.group!!.center)
        return true
    }
}