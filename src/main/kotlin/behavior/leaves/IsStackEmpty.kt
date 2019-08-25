package behavior.leaves

import level.entity.Entity
import behavior.*
import java.util.*

class IsStackEmpty(parent: BehaviorTree, val stackVar: Variable) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        return !dataExists(stackVar) && getData<Stack<Any?>>(stackVar)?.isNotEmpty() == true
    }

    override fun toString() = "IsStackEmpty: (stackVar: $stackVar)"
}