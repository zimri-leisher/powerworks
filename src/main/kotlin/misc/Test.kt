package misc

import item.Inventory
import item.ItemType
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

fun main() {
    // create data
    val controls = createData(100)
    val cases = createData(2)
    // find possible matches
    val casesWithPossibleControls = cases
            .associateWith { case ->
                controls.filter { control -> matches(case, control) }
            }
    val usedControls = mutableListOf<DataPoint>()
    val finalPairs = mutableMapOf<DataPoint, DataPoint>()
    val unpairedCases = mutableListOf<DataPoint>()
    for (caseWithPossibleControl in casesWithPossibleControls) {
        val case = caseWithPossibleControl.key
        val possibleControls = caseWithPossibleControl.value
        val notUsedControls = possibleControls.filterNot { it in usedControls }
        if (notUsedControls.isNotEmpty()) {
            val finalControl = notUsedControls.random()
            finalPairs.put(case, finalControl)
        } else {
            unpairedCases.add(case)
        }
    }
    println("cases generated: ${cases.joinToString()}")
    println("controls generated: ${controls.joinToString()}")
    println("final pairs: ${finalPairs.entries.joinToString(separator = "\n") { "[case: ${it.key}, control: ${it.value}" }}")
    println("cases that were unable to be paired: ${unpairedCases.joinToString()}")
}

fun matches(case: DataPoint, control: DataPoint): Boolean {
    return case.site == control.site && case.color == control.color && case.gender == control.gender
}

fun createData(range: Int): List<DataPoint> {
    val possibleSites = arrayOf("Site 1", "Site 2", "Site 3", "Site 4")
    val possibleGenders = arrayOf("Male", "Female", "Other")
    val possibleColors = arrayOf("Blue", "Red", "Green")
    val list = mutableListOf<DataPoint>()
    for (i in 1..range) {
        list.add(DataPoint(possibleSites.random(), possibleGenders.random(), possibleColors.random()))
    }
    return list
}

private var nextID = 0

data class DataPoint(val site: String, val gender: String, val color: String) {
    val id = nextID++
}


fun numWays(totalSteps: Int, possibleSteps: List<Int> = listOf(1, 2)): Int {
    if (totalSteps == 0)
        return 1
    var total = 0
    for (possibleStep in possibleSteps) {
        if (possibleStep <= totalSteps) {
            total += numWays(totalSteps - possibleStep, possibleSteps)
        }
    }
    return total
}

fun asyncTest() {
    val c = AtomicInteger()
    val time = measureTimeMillis {

        for (i in 0..1_000_000) {
        }
    }
    println("time taken: $time, resulting value: ${c.get()}")
}

// unique list of numbers that end in the total steps
// difference between the numbers is one of the possible steps
//

fun testAsyncStuff() {
    println("generating")
    val list = mutableListOf<String>()
    for (i in 0..1000000) {
        list.add("bleep")
    }
    list.add("boop")
    println("generated")
    val time = measureTimeMillis {
        println("${list.filterAsync { it == "boop" }.size} elements matching criteria")
    }
    val time2 = measureTimeMillis {
        println("${list.filter { it == "boop" }.size} elements matching criteria")
    }
    println("took $time millis for async, $time2 millis for non async")
}

fun testAsyncStuff2() {
    val g = AtomicLong()
    val time = measureNanoTime {
        for (i in 0.toLong()..100_000_000) {
            g.addAndGet(i)
        }
    }
    println("non async took $time, result: ${g.get()}")
    val b = AtomicLong()
    val time2 = measureNanoTime {
        for (i in 0.toLong()..100_000_000) {
        }
    }
    println("async took $time2, result: ${b.get()}")
}

fun <T> List<T>.filterAsync(f: (T) -> Boolean): List<T> {
    val threadsToGenerate = Math.ceil(size.toDouble() / 100).toInt()
    println("generating $threadsToGenerate coroutines")
    val ret = mutableListOf<T>()
    return ret
}

fun testInventory() {
    try {
        fun checkOrder(i: Inventory): Boolean {
            var currentID = -1
            var currentlyInNulls = false
            for (item in i) {
                if (item != null) {
                    if (currentlyInNulls)
                        return false
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
        for (x in 0..30)
            i.remove(ItemType.ALL.get((Math.random() * ItemType.ALL.size).toInt()), 1)
        assert(checkOrder(i))
    } catch (e: Exception) {
        println("Test failed:")
        e.printStackTrace()
    }
    println("Inventory tests successful")
}