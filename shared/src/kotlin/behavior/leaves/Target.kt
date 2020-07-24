package behavior.leaves

import behavior.BehaviorTree
import behavior.DataLeaf
import behavior.Variable
import level.LevelObject
import level.SetEntityTarget
import level.entity.Entity
import network.LevelObjectReference
import network.MovingObjectReference

class Target(parent: BehaviorTree, val targetVar: Variable) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        val target = getData<Any>(targetVar)
        val actualTarget = when (target) {
            is LevelObjectReference -> target
            is LevelObject -> target.toReference()
            null -> null
            else -> return false
        }
        println("targeting: $actualTarget")
        return entity.level.modify(SetEntityTarget(entity.toReference() as MovingObjectReference, actualTarget))
    }
}