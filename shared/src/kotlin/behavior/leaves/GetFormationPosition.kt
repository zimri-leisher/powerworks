package behavior.leaves

import behavior.BehaviorTree
import behavior.DataLeaf
import behavior.Variable
import level.entity.Entity

class GetFormationPosition(parent: BehaviorTree, val dest: Variable) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        val formationPos = entity.group?.formation?.positions?.get(entity) ?: return false
        setData(dest, formationPos)
        return true
    }
}