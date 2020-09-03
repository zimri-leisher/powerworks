package behavior.leaves

import level.entity.Entity
import behavior.*
import level.LevelManager
import misc.Coord

class GetMouseLevelPosition(parent: BehaviorTree, val dest: Variable) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        setData(dest, Coord(LevelManager.mouseLevelX, LevelManager.mouseLevelY))
        return true
    }

    override fun toString() = "GetMouseLevelPosition: (dest: $dest)"
}