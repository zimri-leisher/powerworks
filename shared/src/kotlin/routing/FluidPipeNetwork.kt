package routing

import level.Level
import level.LevelManager
import resource.ResourceCategory

class FluidPipeNetwork(level: Level) : PipeNetwork(ResourceCategory.FLUID, level, 2) {
    private constructor() : this(LevelManager.EMPTY_LEVEL)
}