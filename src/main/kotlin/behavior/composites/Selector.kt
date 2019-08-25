package behavior.composites

import level.entity.Entity
import behavior.*
import main.Game

class Selector(parent: BehaviorTree, children: MutableList<Node>, val order: CompositeOrder = CompositeOrder.ORDERED) : Composite(parent, children) {

    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        if (order == CompositeOrder.RANDOM) {
            val order = 0 until children.size
            setData(DefaultVariable.SELECTOR_RANDOM_CHILD_EXECUTION_ORDER, order.shuffled(Game.currentLevel.rand))
        }
        setData(DefaultVariable.SELECTOR_CHILDREN_FAILED, mutableListOf<Node>())
        setData(DefaultVariable.SELECTOR_CURRENT_RUNNING_CHILD, children.first())
        children.forEach { it.init(entity) }
    }

    override fun execute(entity: Entity) {
        val failedChildren = getData<MutableList<Node>>(DefaultVariable.SELECTOR_CHILDREN_FAILED)!!
        if (order == CompositeOrder.RANDOM) {
            val childrenIndexOrder = getData<List<Int>>(DefaultVariable.SELECTOR_RANDOM_CHILD_EXECUTION_ORDER)!!
            for (index in childrenIndexOrder) {
                val child = children[index]
                if (child !in failedChildren) {
                    if (child.state == NodeState.RUNNING) {
                        child.execute(entity)
                        setData(DefaultVariable.SELECTOR_CURRENT_RUNNING_CHILD, child)!!
                        return
                    } else if (child.state == NodeState.FAILURE) {
                        failedChildren.add(child)
                        return
                    }
                }
            }
        }
        for (child in children) {
            if (child !in failedChildren) {
                if (child.state == NodeState.RUNNING) {
                    child.execute(entity)
                    if (order != CompositeOrder.PARALLEL) {
                        setData(DefaultVariable.SELECTOR_CURRENT_RUNNING_CHILD, child)!!
                        return
                    }
                } else if (child.state == NodeState.FAILURE) {
                    failedChildren.add(child)
                    return
                }
            }
        }
    }

    override fun updateState(entity: Entity) {
        if (order == CompositeOrder.PARALLEL) {
            children.forEach { it.updateState(entity) }
            if (children.any { it.state == NodeState.SUCCESS }) {
                state = NodeState.SUCCESS
            } else if (children.all { it.state == NodeState.FAILURE }) {
                state = NodeState.FAILURE
            } else {
                state = NodeState.RUNNING
            }
        } else {
            val child = getData<Node>(DefaultVariable.SELECTOR_CURRENT_RUNNING_CHILD)!!
            child.updateState(entity)
            if (child.state == NodeState.SUCCESS) {
                state = NodeState.SUCCESS
                return
            } else if (child.state == NodeState.RUNNING) {
                state = NodeState.RUNNING
                return
            }
            if (getData<MutableList<Node>>(DefaultVariable.SELECTOR_CHILDREN_FAILED)!!.containsAll(children)) {
                state = NodeState.FAILURE
                return
            }
        }
    }

    override fun toString(): String {
        return "Selector: [\n    ${children.joinToString(separator = "\n    ")}]"
    }
}