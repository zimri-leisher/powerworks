package behavior

import kotlinx.coroutines.ExperimentalCoroutinesApi

object Behavior {

    object Movement {
        val PATH_TO_MOUSE = BehaviorTree {
            followPath(findPath(getMouseLevelPosition(), useCoroutines = true))
        }
    }

    object Offense {
        @ExperimentalCoroutinesApi
        val ATTACK_NEAREST = BehaviorTree {
            followPath(findPath(getNearestLevelObject()))
        }
    }
}