package behavior.leaves

import level.LevelObject
import level.entity.Entity
import behavior.BehaviorTree
import behavior.DataLeaf
import behavior.Variable
import misc.Coord

/**
 * Gets the position of the [LevelObject] stored under the [levelObjectName] and stores it as a [Coord] under the [destName]
 */
class GetLevelObjectPosition(parent: BehaviorTree, val levelObjVar: Variable, val dest: Variable) : DataLeaf(parent) {

    override fun run(entity: Entity): Boolean {
        val obj = getData<LevelObject>(levelObjVar) ?: return false
        setData(dest, Coord(obj.x, obj.y))
        return true
    }

    override fun toString() = "GetLevelObjectPosition: (levelObjVar: $levelObjVar, dest: $dest)"
}