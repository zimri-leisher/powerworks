package behavior

sealed class Variable(val nodeSpecific: Boolean, val entitySpecific: Boolean, val name: String) {
    override fun toString() = (if(nodeSpecific) "node-" else "-") + (if(entitySpecific) "entity-" else "-") + name
}

class Local(name: String) : Variable(true, true, name)

class NodeOnly(name: String) : Variable(true, false, name)

class EntityOnly(name: String) : Variable(false, true, name)

class Global(name: String) : Variable(false, false, name)

object DefaultVariable {
    val RETURN = Global("return")
    val ARGUMENT = Global("argument")
    val RANDOM_POSITION = Local("randomPosition")
    val PATH_FOUND = EntityOnly("pathFound")
    val PATHING_JOB = Local("pathingJob")
    val MOUSE_LEVEL_POSITION = Global("mouseLevelPosition")
    val MOUSE_MOVED_RELATIVE_TO_LEVEL_MOVEMENT_NUMBER = EntityOnly("mouseHasMovedRelativeToLevel")
    val MOVE_TO_GOAL_POSITION = NodeOnly("moveToGoalPosition")
    val MOVE_TO_TICKS_MOVING = Local("moveToTicksMoving")
    val PATH_BEING_FOLLOWED = Local("moveToPath")
    val PATH_CURRENT_STEP_INDEX = Local("moveToPathCurrentStepIndex")
    val SEQUENCE_CHILDREN_INITIALIZED = Local("sequenceChildrenInitialized")
    val SEQUENCE_CHILDREN_SUCCEEDED = Local("sequenceChildrenSucceeded")
    val SEQUENCE_CURRENT_RUNNING_CHILD = Local("sequenceCurrentRunningChild")
    val SEQUENCE_RANDOM_CHILD_EXECUTION_ORDER = Local("sequenceRandomChildExecutionOrder")
    val SELECTOR_CHILDREN_INITIALIZED = Local("selectorChildrenInitialized")
    val SELECTOR_CHILDREN_FAILED = Local("selectorChildrenFailed")
    val SELECTOR_CURRENT_RUNNING_CHILD = Local("selectorCurrentRunningChild")
    val SELECTOR_RANDOM_CHILD_EXECUTION_ORDER = Local("selectorRandomChildExecutionOrder")
    val STACK_DEFAULT = Local("stackDefault")
    val NEAREST_LEVEL_OBJECT = EntityOnly("nearestLevelObject")
}