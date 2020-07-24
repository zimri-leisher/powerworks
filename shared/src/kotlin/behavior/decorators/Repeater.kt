package behavior.decorators

import behavior.BehaviorTree
import behavior.Decorator
import behavior.Node
import behavior.NodeState
import level.entity.Entity

class Repeater(parent: BehaviorTree, child: Node, val iterations: Int = -1, val untilFail: Boolean = false, val untilSucceed: Boolean = false) : Decorator(parent, child) {

    private var currentIteration = 0

    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        child.init(entity)
    }

    override fun updateState(entity: Entity): NodeState {
        child.updateAndSetState(entity)
        if (untilFail) {
            if (child.state == NodeState.FAILURE) {
                return NodeState.SUCCESS
            }
        }
        if (untilSucceed) {
            if (child.state == NodeState.SUCCESS) {
                return NodeState.SUCCESS
            }
        }
        if (iterations == -1) {
            return NodeState.RUNNING
        } else if (currentIteration == iterations - 1) {
            return child.state
        } else {
            return NodeState.RUNNING
        }
    }

    override fun execute(entity: Entity) {
        if (child.state == NodeState.RUNNING) {
            child.execute(entity)
        } else if (iterations == -1) {
            child.init(entity)
            child.execute(entity)
        } else if (currentIteration < iterations) {
            child.init(entity)
            child.execute(entity)
            currentIteration++
        }
    }

    override fun toString(): String {
        return "Repeater: (iterations: $iterations, untilFail: $untilFail, untilSucceed: $untilSucceed): [\n    $child\n]"
    }
}