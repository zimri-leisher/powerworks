package item.weapon

import item.Item

class WeaponItem(type: WeaponItemType, quantity: Int = 1) : Item(type, quantity) {
    private constructor() : this(WeaponItemType.ERROR, 0)
}