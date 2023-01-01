package level

import network.LevelObjectReference
import player.team.Team
import serialization.AsReference
import serialization.Id
import serialization.Referencable
import java.util.*

abstract class LevelObject protected constructor(
    type: LevelObjectType<out LevelObject>
) : Referencable<LevelObject> {

    private constructor() : this(LevelObjectType.ERROR)

    open val type = type

    @Id(5)
    var id = UUID.randomUUID()!!

    @AsReference
    @Id(6)
    var level: Level = LevelManager.EMPTY_LEVEL
        set(value) {
            if (field != value) {
                if (inLevel) {
                    // on remove from old level
                    beforeRemoveFromLevel(field)
                    beforeAddToLevel(value)
                }
                val oldLevel = field
                field = value
                if (field == LevelManager.EMPTY_LEVEL) {
                    inLevel = false
                } else if (inLevel) {
                    // on add to new level
                    afterAddToLevel(value)
                    afterRemoveFromLevel(oldLevel)
                }
            }
        }

    @Id(992)
    var team = Team.NEUTRAL

    /**
     * If this has been added to a [Level] (one that isn't [LevelManager.EMPTY_LEVEL])
     */
    @Id(15)
    open var inLevel = false
        set(value) {
            if (field && !value) {
                beforeRemoveFromLevel(level)
                field = false
                afterRemoveFromLevel(level)
            } else if (!field && value) {
                if (level == LevelManager.EMPTY_LEVEL) {
                    throw IllegalStateException("Cannot add a LevelObject to the empty level")
                }
                beforeAddToLevel(level)
                field = true
                afterAddToLevel(level)
            }
        }

    // list of objects that this depends on
    // resource containers that this uses, resource networks that this uses
    // they all have to be added first and removed first with this
    @Id(993)
    @AsReference(true)
    val children = mutableListOf<LevelObject>()

    open fun beforeAddToLevel(newLevel: Level) {
    }

    /**
     * When this gets put in the level
     * Called when [inLevel] is changed to true, should usually be from Level.add
     */
    open fun afterAddToLevel(oldLevel: Level) {
    }

    open fun beforeRemoveFromLevel(newLevel: Level) {
    }

    /**
     * When this gets taken out of the level
     * Called when [inLevel] is changed to false, should usually be from Level.remove
     */
    open fun afterRemoveFromLevel(oldLevel: Level) {
    }

    /**
     * Only called if [requiresUpdate] is true
     */
    open fun update() {
    }

    open fun render() {
    }
}