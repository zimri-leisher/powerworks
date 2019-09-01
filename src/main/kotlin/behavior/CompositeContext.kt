package behavior

import behavior.composites.Selector
import behavior.decorators.Inverter
import behavior.decorators.Repeater
import behavior.decorators.Succeeder
import behavior.leaves.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import level.LevelObject

class CompositeContext(val node: Composite) {

    val parent get() = node.parent

    private fun <T : Composite> composite(child: T, initializer: CompositeContext.() -> Unit = {}) {
        CompositeContext(child).initializer()
        node.children.add(child)
    }

    private fun <T : Decorator> decorator(child: T, initializer: CompositeContext.() -> Unit = {}) {
        child.children.clear()
        CompositeContext(child).initializer()
        node.children.add(child)
    }

    private fun leaf(child: Leaf) {
        node.children.add(child)
    }

    /**
     * A composite node that executes each of its children in the given [order].
     * If any children fail, it will return [NodeState.FAILURE], otherwise, it will return [NodeState.RUNNING] until
     * all have succeeded, in which case it returns [NodeState.SUCCESS]
     */
    fun sequence(order: CompositeOrder = CompositeOrder.ORDERED, initializer: CompositeContext.() -> Unit = {}) {
        composite(behavior.composites.Sequence(node.parent, mutableListOf(), order), initializer)
    }

    /**
     * A composite node that executes each of its children in the given [order].
     * If any children succeed, it will return [NodeState.SUCCESS], otherwise, it will return [NodeState.RUNNING] until
     * all have failed, in which case it returns [NodeState.FAILURE]
     */
    fun selector(order: CompositeOrder = CompositeOrder.ORDERED, initializer: CompositeContext.() -> Unit = {}) {
        composite(Selector(parent, mutableListOf(), order), initializer)
    }

    fun inverter(initializer: CompositeContext.() -> Unit = {}) {
        decorator(Inverter(parent, DefaultLeaf(parent)), initializer)
    }

    fun alwaysSucceed(initializer: CompositeContext.() -> Unit = {}) {
        decorator(Succeeder(parent, DefaultLeaf(parent)), initializer)
    }

    fun alwaysFail(initializer: CompositeContext.() -> Unit = {}) {
        inverter {
            alwaysSucceed(initializer)
        }
    }

    fun repeater(iterations: Int = -1, untilFail: Boolean = false, untilSucceed: Boolean = false, initializer: CompositeContext.() -> Unit = {}) {
        decorator(Repeater(parent, DefaultLeaf(parent), iterations, untilFail, untilSucceed), initializer)
    }

    fun moveTo(goal: Variable, goalThreshold: Int = 5, failAfter: Int = -1, axisThreshold: Int = 3) {
        leaf(MoveTo(parent, goal, goalThreshold, failAfter, axisThreshold))
    }

    fun getRandomPosition(dest: Variable = DefaultVariable.RANDOM_POSITION, xCenter: Int = -1, yCenter: Int = -1, radius: Int = 0): Variable {
        leaf(GetRandomPosition(parent, dest, xCenter, yCenter, radius))
        return dest
    }

    fun getMouseLevelPosition(dest: Variable = DefaultVariable.MOUSE_LEVEL_POSITION): Variable {
        leaf(GetMouseLevelPosition(parent, dest))
        return dest
    }

    fun getNearestLevelObject(dest: Variable = DefaultVariable.NEAREST_LEVEL_OBJECT, predicate: (LevelObject) -> Boolean = { true }): Variable {
        leaf(GetNearestLevelObject(parent, dest, predicate))
        return dest
    }

    fun getLevelObjectPosition(levelObjectVar: Variable, destVar: Variable = DefaultVariable.RETURN): Variable {
        leaf(GetLevelObjectPosition(parent, levelObjectVar, destVar))
        return destVar
    }

    fun followPath(pathVar: Variable) {
        leaf(FollowPath(parent, pathVar))
    }

    @ExperimentalCoroutinesApi
    fun findPath(goalVar: Variable, pathDestVar: Variable = DefaultVariable.PATH_FOUND, useCoroutines: Boolean = false): Variable {
        leaf(FindPath(parent, goalVar, pathDestVar, useCoroutines))
        return pathDestVar
    }

    fun getPriority(dest: Variable): Variable {
        leaf(GetPriority(parent, dest))
        return dest
    }

    fun setPriority(priority: Int) {
        leaf(SetPriority(parent, priority))
    }

    fun runBehavior(behaviorTree: BehaviorTree, priority: Int = 0, argument: Variable? = null) {
        leaf(RunBehavior(parent, behaviorTree, priority, argument))
    }

    fun clearVariable(variable: Variable) {
        leaf(ClearVariable(parent, variable))
    }

    fun reset() {
        leaf(Reset(parent, node))
    }
}
