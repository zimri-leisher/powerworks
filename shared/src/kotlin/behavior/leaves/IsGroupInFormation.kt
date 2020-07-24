package behavior.leaves

import behavior.BehaviorTree
import behavior.DataLeaf
import level.entity.Entity

class IsGroupInFormation(parent: BehaviorTree) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        val value = entity.group?.inFormation
        return value == true
    }
}