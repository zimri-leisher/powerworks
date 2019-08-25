package behavior.leaves

import behavior.*
import level.entity.Entity
import misc.Numbers

class FollowPath(parent: BehaviorTree, val pathVar: Variable) : Leaf(parent) {

    private val moveTo = MoveTo(parent, Local(DefaultVariable.MOVE_TO_GOAL_POSITION.name), goalThreshold = 16)

    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        val path = getData<EntityPath>(pathVar)
        if (path == null) {
            state = NodeState.FAILURE
            return
        } else if(path.steps.isEmpty()) {
            state = NodeState.SUCCESS
            return
        }
        setData(DefaultVariable.PATH_BEING_FOLLOWED, path)
        setData(DefaultVariable.PATH_CURRENT_STEP_INDEX, 0)

        setData(moveTo, entity, DefaultVariable.MOVE_TO_GOAL_POSITION.name, path.steps[0])
        moveTo.init(entity)
        if(moveTo.state == NodeState.FAILURE) {
            state = NodeState.FAILURE
        }
    }

    override fun updateState(entity: Entity) {
        val path = getData<EntityPath>(DefaultVariable.PATH_BEING_FOLLOWED)!!
        val currentStepIndex = getData<Int>(DefaultVariable.PATH_CURRENT_STEP_INDEX)!!
        moveTo.updateState(entity)
        if (moveTo.state == NodeState.SUCCESS) {
            if (currentStepIndex == path.steps.lastIndex) {
                state = NodeState.SUCCESS
                return
            }
            setData(DefaultVariable.PATH_CURRENT_STEP_INDEX, currentStepIndex + 1)
            setData(moveTo, entity, DefaultVariable.MOVE_TO_GOAL_POSITION.name, path.steps[currentStepIndex + 1])
            state = NodeState.RUNNING
            return
        } else if(moveTo.state == NodeState.FAILURE) {
            state = NodeState.FAILURE
            return
        } else {
            state = NodeState.RUNNING
        }
    }

    override fun execute(entity: Entity) {
        if(moveTo.state == NodeState.RUNNING) {
            moveTo.execute(entity)
        }
    }

    override fun toString() = "FollowPath: (pathVar: $pathVar)"
}