package behavior.decorators

import behavior.BehaviorTree
import behavior.Decorator
import behavior.Node
import behavior.NodeState
import level.entity.Entity

class Inverter(parent: BehaviorTree, child: Node) : Decorator(parent, child) {
    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        child.init(entity)
    }

    override fun updateState(entity: Entity) {
        child.updateState(entity)
        if (child.state == NodeState.FAILURE) {
            state = NodeState.SUCCESS
        } else if (child.state == NodeState.SUCCESS) {
            state = NodeState.FAILURE
        } else {
            state = NodeState.RUNNING
        }
    }

    override fun execute(entity: Entity) {
        child.execute(entity)
    }

    override fun toString(): String {
        return "Inverter: [\n    $child\n]"
    }
}