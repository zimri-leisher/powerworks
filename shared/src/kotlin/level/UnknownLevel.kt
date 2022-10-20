package level

import item.weapon.Projectile
import level.generator.LevelType
import level.particle.Particle
import level.update.LevelUpdate
import network.User
import java.util.*

class UnknownLevel(id: UUID) : Level(id, LevelInfo(User(UUID.randomUUID(), ""), "", "", LevelType.EMPTY, 0L)) {

    private constructor() : this(UUID.randomUUID())
    override fun canModify(update: LevelUpdate): Boolean {
        return false
    }

    override fun modify(update: LevelUpdate, transient: Boolean): Boolean {
        throw NotImplementedError("Cannot call modify($update, $transient) on an unknown level (id $id)")
    }

    override fun add(p: Particle) {
        throw NotImplementedError("Cannot call add($p) on an unknown level (id $id)")
    }

    override fun add(projectile: Projectile) {
        throw NotImplementedError("Cannot call add($projectile) on an unknown level (id $id)")
    }

    override fun remove(p: Particle) {
        throw NotImplementedError("Cannot call remove($p) on an unknown level (id $id)")
    }

    override fun remove(projectile: Projectile) {
        throw NotImplementedError("Cannot call remove($projectile) on an unknown level (id $id)")
    }
}