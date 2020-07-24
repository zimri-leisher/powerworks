package item.ammo

import item.ItemType
import item.weapon.WeaponItemType

class AmmoItemType(initializer: AmmoItemType.() -> Unit) : ItemType() {
    var validWeapons = listOf<WeaponItemType>()

    init {
        initializer()
        ALL.add(this)
    }

    companion object {
        val ALL = mutableListOf<AmmoItemType>()

        val ERROR = AmmoItemType {
            hidden = true
        }

        val DEFAULT_AMMO = AmmoItemType {
            name = "Default Ammunition"
            validWeapons = WeaponItemType.ALL
        }
    }
}