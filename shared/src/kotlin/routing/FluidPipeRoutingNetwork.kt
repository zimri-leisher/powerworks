package routing

import level.Level
import level.LevelManager
import resource.ResourceCategory

class FluidPipeRoutingNetwork(level: Level) : PipeRoutingNetwork(ResourceCategory.FLUID, level, 2) {
    private constructor() : this(LevelManager.EMPTY_LEVEL)
}