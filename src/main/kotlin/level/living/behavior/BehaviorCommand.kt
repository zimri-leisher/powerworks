package level.living.behavior

import level.living.LivingObject
import main.Game

enum class BehaviorCommand(val execute: (livingObjects: List<LivingObject>) -> Unit) {
    MOVE({ livingObjects ->
        livingObjects.forEach { it.behavior.moveTo(Game.currentLevel.mouseLevelXPixel, Game.currentLevel.mouseLevelYPixel) }
    }),
    ATTACK({ livingObjects ->

    }),
    DEFEND({ livingObjects ->

    }),
    STOP({ livingObjects ->
        livingObjects.forEach { it.behavior.stop() }
    })
}