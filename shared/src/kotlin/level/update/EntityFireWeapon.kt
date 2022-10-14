package level.update

import item.weapon.ProjectileType
import level.Level
import level.LevelManager
import level.entity.DefaultEntity
import level.entity.Entity
import level.entity.EntityType
import misc.Coord
import network.MovingObjectReference
import player.Player
import serialization.Id
import java.util.*
import kotlin.math.absoluteValue

/**
 * A level update for the firing of an [Entity]'s weapon.
 */
class EntityFireWeapon(
    /**
     * The position of the [Entity] when it fired.
     */
    @Id(2)
    val positionWhenFired: Coord,
    /**
     * The angle the [Entity] fired at.
     */
    @Id(3)
    val angleFired: Float,
    /**
     * The type of [Projectile] fired.
     */
    @Id(4)
    val projectileType: ProjectileType,
    /**
     * A reference to the [Entity] that fired.
     */
    @Id(5)
    val entity: Entity,
    level: Level
) : LevelUpdate(LevelUpdateType.ENTITY_FIRE_WEAPON, level) {

    private constructor() : this(
        Coord(0, 0),
        0f,
        ProjectileType.ERROR,
        DefaultEntity(EntityType.ERROR, 0, 0),
        LevelManager.EMPTY_LEVEL
    )

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        if (entity.weapon == null) {
            return false
        }
        if (!entity.weapon!!.canFire) {
            return false
        }
        return true
    }

    override fun act() {
        entity.setPosition(positionWhenFired.x, positionWhenFired.y)
        if (entity.weapon != null) {
            if (!entity.weapon!!.canFire) {
                entity.weapon!!.cooldown = 0
            }
            if (entity.weapon!!.type.projectileType == projectileType) {
                entity.weapon!!.tryFire(angleFired)
                return
            } else {
                println("projectile types diff")
            }
        }

        println("desync, synchronize weapon?")
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntityFireWeapon) {
            return false
        }


        if (other.entity !== entity) {
            return false
        }

        if ((other.angleFired - angleFired).absoluteValue > 1e-6) {
            return false
        }

        if (other.positionWhenFired != positionWhenFired) {
            return false
        }

        if (other.projectileType != projectileType) {
            return false
        }

        return true
    }
}