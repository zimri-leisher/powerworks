package item.weapon

import level.entity.Entity
import serialization.Id

class Weapon(
        @Id(1)
        val type: WeaponItemType) {

    private constructor() : this(WeaponItemType.ERROR)

    @Id(4)
    var fireAnimation = type.fireAnimations.createLocalInstance()

    @Id(2)
    var cooldown = 0

    @Id(3)
    var parent: Entity? = null

    val canFire get() = parent != null && cooldown == 0

    fun update() {
        if (cooldown > 0) {
            cooldown--
        }
    }

    fun render() {
        if (parent != null) {
            fireAnimation[parent!!.rotation].render(parent!!.x, parent!!.y)
        }
    }

    fun tryFire(angle: Float): Boolean {
        if (!canFire) {
            return false
        }
        fireAnimation[parent!!.rotation].playFrom("start")
        cooldown = type.cooldown
        parent!!.level.add(Projectile(type.projectileType, parent!!.x, parent!!.y, angle, parent!!))
        return true
    }

}