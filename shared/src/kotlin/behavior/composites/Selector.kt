package behavior.composites

import level.entity.Entity
import behavior.*
import level.LevelManager
import main.Game
import java.util.*

/**
 * A composite node that executes each of its children in the given [order].
 * If any children succeed, it will return [NodeState.SUCCESS], otherwise, it will return [NodeState.RUNNING] until
 * all have failed, in which case it returns [NodeState.FAILURE]
 */
class Selector(parent: BehaviorTree, children: MutableList<Node>, val order: CompositeOrder = CompositeOrder.ORDERED) : Composite(parent, children) {

    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        val firstRunningChild: Node
        if (order == CompositeOrder.RANDOM) {
            val rand = Random(entity.level.info.seed)
            val order = children.indices.shuffled(rand)
            firstRunningChild = children[order.first()]
            setData(DefaultVariable.SELECTOR_RANDOM_CHILD_EXECUTION_ORDER, order)
        } else {
            firstRunningChild = children.first()
        }
        setData(DefaultVariable.SELECTOR_CHILDREN_FAILED, mutableListOf<Node>())
        if (order == CompositeOrder.PARALLEL) {
            children.forEach { it.init(entity) }
        } else {
            firstRunningChild.init(entity)
            setData(DefaultVariable.SELECTOR_CHILDREN_INITIALIZED, mutableListOf(firstRunningChild))
        }
    }

    override fun execute(entity: Entity) {
        val failedChildren = getData<MutableList<Node>>(DefaultVariable.SELECTOR_CHILDREN_FAILED)!!
        val order = if (order == CompositeOrder.RANDOM) getData<List<Int>>(DefaultVariable.SELECTOR_RANDOM_CHILD_EXECUTION_ORDER)!! else children.indices
        for (index in order) {
            val child = children[index]
            if (child !in failedChildren) {
                if (child.state == NodeState.RUNNING) {
                    child.execute(entity)
                    if (this.order != CompositeOrder.PARALLEL) {
                        break
                    }
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
            val failedChildren = getData<MutableList<Node>>(DefaultVariable.SELECTOR_CHILDREN_FAILED)!!
            val initializedChildren = getData<MutableList<Node>>(DefaultVariable.SELECTOR_CHILDREN_INITIALIZED)!!
            val order = if (order == CompositeOrder.RANDOM) getData<List<Int>>(DefaultVariable.SELECTOR_RANDOM_CHILD_EXECUTION_ORDER)!! else children.indices
            for (index in order) {
                val child = children[index]
                if (child !in failedChildren) {
                    if (child !in initializedChildren) {
                        child.init(entity)
                        initializedChildren.add(child)
                    }
                    child.updateState(entity)
                    if (child.state == NodeState.FAILURE) {
                        failedChildren.add(child)
                    } else if (child.state == NodeState.SUCCESS) {
                        state = NodeState.SUCCESS
                        return
                    } else {
                        state = NodeState.RUNNING
                        return
                    }
                }
            }
            state = NodeState.FAILURE
        }
    }

    override fun toString(): String {
        return "Selector: [\n   ${children.joinToString(separator = "\n    ")}\n]"
    }
}