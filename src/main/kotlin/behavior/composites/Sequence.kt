package behavior.composites

import level.entity.Entity
import behavior.*
import main.Game

/**
 * A composite node that executes each of its children in the given [order].
 * If any children fail, it will return [NodeState.FAILURE], otherwise, it will return [NodeState.RUNNING] until
 * all have succeeded, in which case it returns [NodeState.SUCCESS]
 */
class Sequence(parent: BehaviorTree, children: MutableList<Node>, val order: CompositeOrder = CompositeOrder.ORDERED) : Composite(parent, children) {

    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        if (order == CompositeOrder.RANDOM) {
            setData(DefaultVariable.SEQUENCE_RANDOM_CHILD_EXECUTION_ORDER, children.indices.shuffled(Game.currentLevel.rand))
        }
        setData(DefaultVariable.SEQUENCE_CHILDREN_SUCCEEDED, mutableListOf<Node>())
        children.forEach { it.init(entity) }
        val firstRunningChild = children.firstOrNull { it.state == NodeState.RUNNING }
        setData(DefaultVariable.SEQUENCE_CURRENT_RUNNING_CHILD, firstRunningChild)
        if(firstRunningChild == null) {
            state = if(children.any { it.state == NodeState.FAILURE }) NodeState.FAILURE else NodeState.SUCCESS
            return
        }
    }

    override fun execute(entity: Entity) {
        val successfulChildren = getData<MutableList<Node>>(DefaultVariable.SEQUENCE_CHILDREN_SUCCEEDED)!!
        val childrenIndexOrder = if (order == CompositeOrder.RANDOM) getData<List<Int>>(DefaultVariable.SEQUENCE_RANDOM_CHILD_EXECUTION_ORDER)!! else children.indices
        for (index in childrenIndexOrder) {
            val child = children[index]
            if (child !in successfulChildren) {
                if (child.state == NodeState.RUNNING) {
                    child.execute(entity)
                    if (order != CompositeOrder.PARALLEL) {
                        setData(DefaultVariable.SEQUENCE_CURRENT_RUNNING_CHILD, child)
                        return
                    }
                } else if (child.state == NodeState.SUCCESS) {
                    successfulChildren.add(child)
                    return
                }
            }
        }
    }

    override fun updateState(entity: Entity) {
        if (order == CompositeOrder.PARALLEL) {
            children.forEach { it.updateState(entity) }
            if (children.any { it.state == NodeState.FAILURE }) {
                state = NodeState.FAILURE
            } else if (children.all { it.state == NodeState.SUCCESS }) {
                state = NodeState.SUCCESS
            } else {
                state = NodeState.RUNNING
            }
        } else {
            val child = getData<Node>(DefaultVariable.SEQUENCE_CURRENT_RUNNING_CHILD)
            if(child != null) {
                child.updateState(entity)
                if (child.state == NodeState.FAILURE) {
                    state = NodeState.FAILURE
                    return
                } else if (child.state == NodeState.RUNNING) {
                    state = NodeState.RUNNING
                    return
                }
                if (getData<MutableList<Node>>(DefaultVariable.SEQUENCE_CHILDREN_SUCCEEDED)!!.containsAll(children)) {
                    state = NodeState.SUCCESS
                    return
                }
            } else {
                if (children.any { it.state == NodeState.FAILURE }) {
                    state = NodeState.FAILURE
                } else if (children.all { it.state == NodeState.SUCCESS }) {
                    state = NodeState.SUCCESS
                }
            }
        }
    }

    override fun toString(): String {
        return "Sequence: [\n   ${children.joinToString(separator = "\n    ")}\n]"
    }
}