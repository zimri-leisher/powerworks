package level.entity

import item.weapon.Weapon
import level.moving.MovingObject
import serialization.Id

abstract class Entity(type: EntityType<out Entity>, x: Int, y: Int, rotation: Int = 0) : MovingObject(type, x, y, rotation) {
    override val type = type

    @Id(23)
    var weapon: Weapon? = null
        set(value) {
            field = value
            field?.parent = this
        }

    @Id(24)
    val behavior = EntityBehavior(this)

    @Id(25)
    var group: EntityGroup? = null

    override fun update() {
        weapon?.update()
        behavior.update()
        super.update()
    }

    override fun render() {
        super.render()
        weapon?.render()
        group?.render()
    }
}