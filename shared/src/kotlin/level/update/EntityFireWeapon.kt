package level.update

import item.weapon.ProjectileType
import level.Level
import level.LevelManager
import level.entity.Entity
import misc.PixelCoord
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
        val positionWhenFired: PixelCoord,
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
        val entityReference: MovingObjectReference
) : LevelUpdate(LevelUpdateType.ENTITY_FIRE_WEAPON) {

    private constructor() : this(PixelCoord(0, 0), 0f, ProjectileType.ERROR, MovingObjectReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0))

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if (entityReference.value == null) {
            return false
        }
        val entity = entityReference.value!! as Entity
        if (entity.weapon == null) {
            return false
        }
        if (!entity.weapon!!.canFire) {
            return false
        }
        return true
    }

    override fun act(level: Level) {
        val entity = entityReference.value!! as Entity
        entity.setPosition(positionWhenFired.xPixel, positionWhenFired.yPixel)
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

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntityFireWeapon) {
            return false
        }

        if (other.entityReference.value == null) {
            return false
        }

        if (other.entityReference.value !== entityReference.value) {
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

    override fun resolveReferences() {
        entityReference.value = entityReference.resolve()
    }

}