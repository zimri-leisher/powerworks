package behavior.decorators

import behavior.BehaviorTree
import behavior.Decorator
import behavior.Node
import behavior.NodeState
import level.entity.Entity

class WaitFor(parent: BehaviorTree, child: Node) : Decorator(parent, child) {
    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        child.init(entity)
    }

    override fun updateState(entity: Entity): NodeState {
        if(child.state == NodeState.SUCCESS) {
            return NodeState.SUCCESS
        } else {
            return NodeState.RUNNING
        }
    }

    override fun execute(entity: Entity) {

    }

}