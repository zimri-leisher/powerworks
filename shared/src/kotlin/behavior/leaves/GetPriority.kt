package behavior.leaves

import behavior.BehaviorTree
import behavior.DataLeaf
import behavior.Variable
import level.entity.Entity

class GetPriority(parent: BehaviorTree, val priorityVar: Variable) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        val priority = entity.getPriority(parent)
        if(priority != -1) {
            setData(priorityVar, priority)
            return true
        }
        return false
    }

    override fun toString() = "GetPriority: (priorityVar: $priorityVar)"
}