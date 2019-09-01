package behavior

import level.entity.Entity
import behavior.composites.Sequence

class DecoratorWithoutChildException(message: String) : Exception(message)
class NoSuchDataNameException(message: String) : Exception(message)
class DataCastException(message: String) : Exception(message)

/**
 * A data structure that is used to define behavior for [Entities][Entity]. It is based on [Leaf nodes][Leaf], [Composite nodes][Composite],
 * and [Decorator nodes][Decorator] (see documentation for each for more details about tree structure).
 *
 * To use an instance, first call [init] with the entity object you want to use as the argument, and then call [update]
 * every game tick to execute the behavior.
 *
 * Behavior trees, in addition to storing data about nodes and their layout, store arbitrary data used to communicate between
 * nodes in the [data] map. This data can be retrieved/modified with a [DataKey] and [Node.getData] or [Node.setData]
 */
class BehaviorTree(initializer: CompositeContext.() -> Unit = {}) {

    private val base = Sequence(this, mutableListOf())
    private val entities = mutableListOf<Entity>()

    var currentEntity: Entity? = null

    /**
     * The data for this tree that is used to communicate between [Node]s
     */
    val data = VariableData()

    init {
        CompositeContext(base).initializer()
    }

    /**
     * Initializes the [Node]s in this behavior tree for the given [Entity]. This must be called before calling
     * [update] with the same [entity], or undefined behavior (probably crashes) will occur
     */
    fun init(entity: Entity) {
        currentEntity = entity
        if (!hasBeenInitialized(entity)) {
            entities.add(entity)
            base.init(entity)
        }
    }

    /**
     * @return whether the [init] method has been called for this [entity]
     */
    fun hasBeenInitialized(entity: Entity) = entity in entities

    fun hasPriority(entity: Entity) = entity.behaviors.values.max() == entity.getPriority(this)

    /**
     * Updates the [Node]s in this behavior tree for the given [Entity]. This should only be called after [init]
     * and it should happen once every game update for correct functionality
     */
    fun update(entity: Entity) {
        currentEntity = entity
        base.updateState(entity)
        if (base.state == NodeState.RUNNING) {
            base.execute(entity)
        } else {
            reset(entity)
        }
    }

    fun reset(entity: Entity) {
        currentEntity = entity
        entity.finishBehavior(this)
        entities.remove(entity)
        data.deleteCorresponding(entity)
    }

    override fun toString(): String {
        return "BehaviorTree: [\n    ${base.children.joinToString(separator = "\n    ")}\n]"
    }
}

/**
 * A state of a node. These are used to determine if the task represented by the node has completed, and how it completed:
 * was it successful or not?
 */
enum class NodeState {
    /**
     * Defines a node which has not finished its task
     */
    RUNNING,
    /**
     * Defines a node which has finished its task successfully. The definition of success is arbitrary and decided by the
     * node
     */
    SUCCESS,
    /**
     * Defines a node which has finished its task unsuccessfully. The definition of failure is arbitrary and decided by the
     * node
     */
    FAILURE,
    /**
     * A node which has this state was stopped by something and should halt functioning
     */
    STOPPED
}


enum class CompositeOrder {
    /**
     * The children of this composite will be executed in a random order that is shuffled every time it is initialized
     * for the given entity object
     */
    RANDOM,
    /**
     * The children of this composite will be executed in the order they are defined
     */
    ORDERED,
    /**
     * The children of this composite will be executed all at the same time
     */
    PARALLEL
}

class DefaultLeaf(parent: BehaviorTree) : Leaf(parent) {
    override fun init(entity: Entity) {
    }

    override fun updateState(entity: Entity) {
    }

    override fun execute(entity: Entity) {
    }
}