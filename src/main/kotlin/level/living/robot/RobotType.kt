package level.living.robot

import level.living.LivingType

class RobotType<T : Robot>(initializer: RobotType<T>.() -> Unit) : LivingType<T>({}) {
    init {
        initializer()
    }
}