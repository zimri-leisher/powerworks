package behavior

import level.Level
import level.LevelObject
import level.entity.Entity
import behavior.leaves.*
import behavior.composites.*
import behavior.decorators.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

private var nextId = 0

/**
 * A node in the [parent] [BehaviorTree]. This can either be a [Leaf], a [Composite] or a [Decorator] node. This contains
 * several utility methods for [getting][getData] and [setting][setData] data in the [parent]'s [data] map
 *
 * Nodes have [init], [updateState] and [execute] functions, which are meant to be overridden by subclasses, as well as
 * a [state], which is used to determine if the task represented by the node has completed or not
 */
sealed class Node(val parent: BehaviorTree) {

    val id = nextId++

    /**
     * The current [NodeState] of this node
     */
    var state = NodeState.RUNNING

    fun setData(node: Node? = null, entity: Entity? = null, name: String, value: Any?): Any? = parent.data.set(node, entity, name, value)

    fun setData(variable: Variable, value: Any?) = setData(
            if (variable.nodeSpecific) this else null,
            if (variable.entitySpecific) parent.currentEntity else null,
            variable.name, value)

    fun <T> getData(node: Node? = null, entity: Entity? = null, name: String): T? = parent.data.get(node, entity, name)

    fun <T> getData(variable: Variable): T? = getData(
            if(variable.nodeSpecific) this else null,
            if(variable.entitySpecific) parent.currentEntity else null,
            variable.name)

    fun dataExists(node: Node? = null, entity: Entity? = null, name: String) = parent.data.exists(node, entity, name)

    fun dataExists(variable: Variable) = dataExists(
            if(variable.nodeSpecific) this else null,
            if(variable.entitySpecific) parent.currentEntity else null,
            variable.name)

    fun clearData(node: Node? = null, entity: Entity? = null, name: String) {
        parent.data.deleteCorresponding(node, entity, name)
    }

    fun clearData(variable: Variable) = clearData(if(variable.nodeSpecific) this else null, if(variable.entitySpecific) parent.currentEntity else null, variable.name)

    /**
     * Initializes this node for the given [entity].
     * This should be called before calling either [updateState] or [execute] for the given [entity]
     */
    abstract fun init(entity: Entity)

    /**
     * Updates the [state] of this node for the given [entity]. It should not act on anything, only check values and set the state accordingly.
     * This is where the definition of [NodeState.SUCCESS], [NodeState.FAILURE] and [NodeState.RUNNING] are defined.
     * Because this is called before [execute] and only checks values, it is not necessary to store a [state] value for
     * each [Entity] this [BehaviorTree] is executing for
     */
    abstract fun updateState(entity: Entity)

    /**
     * Executes this node for the given [entity]. This should only be called after calling [updateState] with the same
     * [entity]. This is where [Leaf] nodes acts on the [Entity] (and anything else in the game), perhaps by changing
     * velocities or modifying an inventory, and where [Composite] and [Decorator] nodes run their logic and execute
     * their children
     */
    abstract fun execute(entity: Entity)
}

/**
 * A node which has no children, only a [parent] [BehaviorTree]. These are generally the functional nodes, which act on
 * the [Level] and other things that are independent from this [BehaviorTree].
 *
 * [MoveTo] is a simple and common example of a leaf node
 */
abstract class Leaf(parent: BehaviorTree) : Node(parent)

abstract class DataLeaf(parent: BehaviorTree) : Leaf(parent) {
    abstract fun run(entity: Entity): Boolean

    final override fun init(entity: Entity) {
        state = if (run(entity)) NodeState.SUCCESS else NodeState.FAILURE
    }

    final override fun execute(entity: Entity) {
    }

    final override fun updateState(entity: Entity) {
    }
}

/**
 * A node with one child. These perform various functions in the tree ranging from simple transformations on the result
 * of their children to actions performed on the nodes themselves.
 *
 * [Succeeder] and [Repeater] are common and simple examples of decorators
 */
abstract class Decorator(parent: BehaviorTree, child: Node) : Composite(parent, mutableListOf(child)) {
    val child
        get() = try {
            children.first()
        } catch (e: NoSuchElementException) {
            throw DecoratorWithoutChildException("Decorator is missing a child")
        }
}

/**
 * A node with more than one children.
 *
 * [Sequence] is a common and simple example of a composite
 */
abstract class Composite(parent: BehaviorTree, val children: MutableList<Node>) : Node(parent)