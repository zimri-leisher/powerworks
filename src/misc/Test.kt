package misc

import item.Inventory
import item.ItemType

fun testInventory() {
    try {
        fun checkOrder(i: Inventory): Boolean {
            var currentID = -1
            var currentlyInNulls = false
            for (item in i) {
                if (item != null) {
                    if (currentlyInNulls)
                        return false
                    println(item)
                    if (item.type.id >= currentID)
                        currentID = item.type.id
                    else
                        return false
                } else {
                    currentlyInNulls = true
                }
            }
            return true
        }
        /* Check creation */
        val i = Inventory(10, 10)
        /* Check single item addition */
        assert(i.add(ItemType.ERROR, 1))
        assert(i[0]!!.type == ItemType.ERROR)
        /* Check single item removal */
        i.remove(ItemType.ERROR, 1)
        assert(i[0] == null)
        /* Check multiple item addition */
        println("Addition")
        for (x in 0..30)
            i.add(ItemType.ALL.get((Math.random() * ItemType.ALL.size).toInt()), 1)
        assert(checkOrder(i))
        /* Check multiple item removal */
        println("Removal")
        for(x in 0..30)
            i.remove(ItemType.ALL.get((Math.random() * ItemType.ALL.size).toInt()), 1)
        assert(checkOrder(i))
    } catch (e: Exception) {
        println("Test failed:")
        e.printStackTrace()
    }
    println("Inventory tests successful")
}