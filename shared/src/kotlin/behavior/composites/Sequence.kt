package behavior.composites

import level.entity.Entity
import behavior.*
import java.util.*

/**
 * A composite node that executes each of its children in the given [order].
 * If any children fail, it will return [NodeState.FAILURE], otherwise, it will return [NodeState.RUNNING] until
 * all have succeeded, in which case it returns [NodeState.SUCCESS]
 */
class Sequence(parent: BehaviorTree, children: MutableList<Node>, val order: CompositeOrder = CompositeOrder.ORDERED) : Composite(parent, children) {

    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        val firstRunningChild: Node
        if (order == CompositeOrder.RANDOM) {
            val rand = Random(entity.level.info.seed)
            val order = children.indices.shuffled(rand)
            firstRunningChild = children[order.first()]
            setData(DefaultVariable.SEQUENCE_RANDOM_CHILD_EXECUTION_ORDER, order)
        } else {
            firstRunningChild = children.first()
        }
        setData(DefaultVariable.SEQUENCE_CHILDREN_SUCCEEDED, mutableListOf<Node>())
        if (order == CompositeOrder.PARALLEL) {
            children.forEach { it.init(entity) }
        } else {
            firstRunningChild.init(entity)
            setData(DefaultVariable.SEQUENCE_CHILDREN_INITIALIZED, mutableListOf(firstRunningChild))
        }
    }

    override fun execute(entity: Entity) {
        val successfulChildren = getData<MutableList<Node>>(DefaultVariable.SEQUENCE_CHILDREN_SUCCEEDED)!!
        val order = if(order == CompositeOrder.RANDOM) getData<List<Int>>(DefaultVariable.SEQUENCE_RANDOM_CHILD_EXECUTION_ORDER)!! else children.indices
        for(index in order) {
            val child = children[index]
            if(child !in successfulChildren) {
                if(child.state == NodeState.RUNNING) {
                    child.execute(entity)
                    if(this.order != CompositeOrder.PARALLEL) {
                        break
                    }
                }
            }
        }
    }

    override fun updateState(entity: Entity): NodeState {
        if (order == CompositeOrder.PARALLEL) {
            children.forEach { it.updateAndSetState(entity) }
            if (children.any { it.state == NodeState.FAILURE }) {
                return NodeState.FAILURE
            } else if (children.all { it.state == NodeState.SUCCESS }) {
                return NodeState.SUCCESS
            } else {
                return NodeState.RUNNING
            }
        } else {
            val successfulChildren = getData<MutableList<Node>>(DefaultVariable.SEQUENCE_CHILDREN_SUCCEEDED)!!
            val initializedChildren = getData<MutableList<Node>>(DefaultVariable.SEQUENCE_CHILDREN_INITIALIZED)!!
            val order = if(order == CompositeOrder.RANDOM) getData<List<Int>>(DefaultVariable.SEQUENCE_RANDOM_CHILD_EXECUTION_ORDER)!! else children.indices
            for (index in order) {
                val child = children[index]
                if(child !in successfulChildren) {
                    if(child !in initializedChildren) {
                        child.init(entity)
                        initializedChildren.add(child)
                    }
                    child.updateAndSetState(entity)
                    if (child.state == NodeState.SUCCESS) {
                        println("child succeeded")
                        successfulChildren.add(child)
                    } else if (child.state == NodeState.FAILURE) {
                        return NodeState.FAILURE
                    } else {
                        return NodeState.RUNNING
                    }
                }
            }
        }
        return NodeState.SUCCESS
    }

    override fun toString(): String {
        return "Sequence: [\n   ${children.joinToString(separator = "\n    ")}\n]"
    }
}