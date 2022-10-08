package item.weapon

import graphics.Animation
import graphics.AnimationCollection
import graphics.Texture
import item.ItemType
import serialization.ObjectList

class WeaponItemType(initializer: WeaponItemType.() -> Unit = {}) : ItemType() {

    var projectileType = ProjectileType.SMALL_BULLET
    var cooldown = 30
    var fireAnimations = AnimationCollection.ALL.first()

    init {
        initializer()
        ALL.add(this)
    }

    companion object {

        @ObjectList
        val ALL = mutableListOf<WeaponItemType>()

        val ERROR = WeaponItemType { hidden = true }

        val MACHINE_GUN = WeaponItemType {
            name = "Heavy Kinetic 40 Mk."
            cooldown = 22
            icon = Texture(AnimationCollection.MACHINE_GUN.animations[1].frames[2])
            fireAnimations = AnimationCollection.MACHINE_GUN
        }
    }
}

/* TODO
sealed class WeaponType(val id: Int, val tubeConnections: Array<Pair<Int, Int>>) {

    val validMods: Array<Modification>
    val textures: Array<ImageCollection>
    val proj: Image = Image(WEAPON_DIR + "weapon$id/proj.png")

    init {
        textures = arrayOf(
                ImageCollection(WEAPON_DIR + "weapon$id/dir_0.png", 3),
                ImageCollection(WEAPON_DIR + "weapon$id/dir_1.png", 3),
                ImageCollection(WEAPON_DIR + "weapon$id/dir_2.png", 3),
                ImageCollection(WEAPON_DIR + "weapon$id/dir_3.png", 3)
        )
        val modImages = mutableListOf<Image>()
        var i = 0
        while (Paths.get(WEAPON_DIR, "weapon$id/mod_$i.png").toFile().exists()) {
            modImages.add(Image(WEAPON_DIR + "weapons$id/mod_$i.png"))
            i++
        }
        mods = modImages.toTypedArray()
    }

    companion object {
        private const val WEAPON_DIR = "/textures/weapon/"
    }

    object MachineGun : WeaponType(1, arrayOf(
            Pair(3, 23),
            Pair(1, 3),
            Pair(3, 1),
            Pair(23, 3)
    ))
    object IonPulse : WeaponType(2)
    object Sniper : WeaponType(3)
}
        */