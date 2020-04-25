package routing

import level.Level
import level.LevelManager
import resource.ResourceCategory

class ItemPipeRoutingNetwork(level: Level) : PipeRoutingNetwork(ResourceCategory.ITEM, level, 1) {
    private constructor() : this(LevelManager.EMPTY_LEVEL)
}