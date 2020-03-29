package item.tool

import item.ItemType
import level.LevelObjectType

class ToolItemType(initializer: ToolItemType.() -> Unit = {}) : ItemType() {
    var range = -1
    var targetType: LevelObjectType<*>? = null

    init {
        initializer()
        ALL.add(this)
    }

    companion object {
        val ALL = mutableListOf<ToolItemType>()
    }
}

/*

Kinds of tools:
    Weapons:
        Melee - a position within a certain range
        Ranged - a position within a much larger range
            Projectile - spawns a projectile
            "Satellite" - no projectile spawned
    Repair tool? - a LevelObject (maybe Block) within a certain range



 */