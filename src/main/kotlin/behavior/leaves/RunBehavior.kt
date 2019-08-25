package behavior.leaves

import behavior.BehaviorTree
import behavior.Leaf
import behavior.NodeState
import behavior.Variable
import level.entity.Entity

class RunBehavior(parent: BehaviorTree, val behaviorTree: BehaviorTree, val priority: Int, val argumentVar: Variable?) : Leaf(parent) {
    override fun init(entity: Entity) {
        state = NodeState.RUNNING
    }

    override fun updateState(entity: Entity) {
    }

    override fun execute(entity: Entity) {
        entity.runBehavior(behaviorTree, priority, if(argumentVar == null) null else getData<Any?>(argumentVar))
        state = NodeState.SUCCESS
    }

    override fun toString() = "RunBehavior: (behaviorTree: $behaviorTree, priority: $priority)"
}