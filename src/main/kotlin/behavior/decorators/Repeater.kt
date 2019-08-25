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

    override fun updateState(entity: Entity) {
        child.updateState(entity)
        if (untilFail) {
            if (child.state == NodeState.FAILURE) {
                state = NodeState.SUCCESS
                return
            }
        }
        if (untilSucceed) {
            if (child.state == NodeState.SUCCESS) {
                state = NodeState.SUCCESS
                return
            }
        }
        if (iterations == -1) {
            state = NodeState.RUNNING
        } else if (currentIteration == iterations - 1) {
            state = child.state
        } else {
            state = NodeState.RUNNING
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