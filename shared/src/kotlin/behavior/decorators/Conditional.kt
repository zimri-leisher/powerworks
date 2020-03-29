package behavior.decorators

import behavior.*
import level.entity.Entity

class Conditional(parent: BehaviorTree, child: Node, val conditionVar: Variable) : Decorator(parent, child) {

    var currentCondition: Boolean = false

    override fun init(entity: Entity) {
        currentCondition = getData<Boolean>(conditionVar) ?: false
        if(currentCondition) {
            child.init(entity)
            state = child.state
        } else {
            state = NodeState.FAILURE
        }
    }

    override fun updateState(entity: Entity) {
        currentCondition = getData<Boolean>(conditionVar) ?: false
        if(currentCondition) {
            child.updateState(entity)
            state = child.state
        } else {
            state = NodeState.FAILURE
        }
    }

    override fun execute(entity: Entity) {
        if(currentCondition) {
            if(child.state == NodeState.RUNNING) {
                child.execute(entity)
            }
        }
    }

    override fun toString() = "Conditional: (conditionVar: $conditionVar): [\n    $child\n]"

}