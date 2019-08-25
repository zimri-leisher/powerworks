package behavior.leaves

import level.entity.Entity
import behavior.*
import java.util.*

class PopFromStack(parent: BehaviorTree, val destVar: Variable, val stackVar: Variable) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        if (dataExists(stackVar)) {
            val stack = getData<Stack<Any?>>(stackVar)!!
            if (stack.isEmpty()) {
                return false
            } else {
                setData(destVar, stack.pop())
                return true
            }
        } else {
            return false
        }
    }

    override fun toString() = "PopFromStack: (destVar: $destVar, stackVar: $stackVar)"
}