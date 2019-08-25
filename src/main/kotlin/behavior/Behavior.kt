package behavior

object Behavior {

    object Movement {
        val TO_MOUSE = BehaviorTree {
            moveTo(getMouseLevelPosition())
        }

        val TO_ARGUMENT = BehaviorTree {
            moveTo(DefaultVariable.ARGUMENT)
        }
    }

    object Offense {
        val ATTACK_ARGUMENT = BehaviorTree {
            followPath(findPath(getMouseLevelPosition()))
        }
    }
}