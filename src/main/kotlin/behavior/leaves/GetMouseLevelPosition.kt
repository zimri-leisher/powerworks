package behavior.leaves

import level.entity.Entity
import behavior.*
import main.Game
import misc.PixelCoord

class GetMouseLevelPosition(parent: BehaviorTree, val dest: Variable) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {
        setData(dest, PixelCoord(Game.currentLevel.mouseLevelXPixel, Game.currentLevel.mouseLevelYPixel))
        return true
    }

    override fun toString() = "GetMouseLevelPosition: (dest: $dest)"
}