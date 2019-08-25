package behavior.leaves

import level.entity.Entity
import behavior.BehaviorTree
import behavior.DataLeaf

class SetPriority(parent: BehaviorTree, val priority: Int) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        if(parent.hasBeenInitialized(entity)) {
            entity.setPriority(parent, priority)
            return true
        }
        return false
    }

    override fun toString() = "SetPriority: (priority: $priority)"
}