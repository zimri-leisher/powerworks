package behavior.leaves

import behavior.*
import level.entity.Entity
import java.util.*

class PushToStack(parent: BehaviorTree, val dataVar: Variable, val stackVar: Variable) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        val stack: Stack<Any?>
        if (dataExists(stackVar)) {
            stack = getData(stackVar)!!
        } else {
            stack = Stack()
            setData(stackVar, stack)
        }
        stack.push(getData(dataVar))
        return true
    }

    override fun toString() = "PushToStack: (dataVar: $dataVar, stackVar: $stackVar)"
}