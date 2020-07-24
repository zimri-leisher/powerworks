package behavior.leaves

import level.entity.Entity
import behavior.*
import level.LevelManager
import misc.PixelCoord

class GetMouseLevelPosition(parent: BehaviorTree, val dest: Variable) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        setData(dest, PixelCoord(LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel))
        return true
    }

    override fun toString() = "GetMouseLevelPosition: (dest: $dest)"
}