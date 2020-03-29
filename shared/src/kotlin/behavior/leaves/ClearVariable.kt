package behavior.leaves

import behavior.BehaviorTree
import behavior.DataLeaf
import behavior.Variable
import level.entity.Entity

class ClearVariable(parent: BehaviorTree, val variable: Variable) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        clearData(variable)
        return true
    }
}