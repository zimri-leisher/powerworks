package misc

import graphics.text.TaggedText
import graphics.text.TextManager
import item.Inventory
import item.ItemType
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    println(numWays(4))
}

fun numWays(totalSteps: Int, possibleSteps: List<Int> = listOf(1, 2)): Int {
    if(totalSteps == 0)
        return 1
    var total = 0
    for(possibleStep in possibleSteps) {
        if(possibleStep <= totalSteps) {
            total += numWays(totalSteps - possibleStep, possibleSteps)
        }
    }
    return total
}

fun asyncTest() {
    val c = AtomicInteger()
    val time = measureTimeMillis {

        for(i in 0..1_000_000) {
            launch {
                // this is a launched coroutine, will execute simultaneously
                //random operation for testing purposes
                c.addAndGet(i)
            }
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
        for(i in 0.toLong()..100_000_000) {
            g.addAndGet(i)
        }
    }
    println("non async took $time, result: ${g.get()}")
    val b = AtomicLong()
    val time2 = measureNanoTime {
        for(i in 0.toLong()..100_000_000) {
            launch { b.addAndGet(i) }
        }
    }
    println("async took $time2, result: ${b.get()}")
}

fun <T> List<T>.filterAsync(f: (T) -> Boolean): List<T> {
    val threadsToGenerate = Math.ceil(size.toDouble() / 100).toInt()
    println("generating $threadsToGenerate coroutines")
    val ret = mutableListOf<T>()
    val jobs = mutableListOf<Deferred<List<T>>>()
    for (i in 0 until threadsToGenerate) {
        jobs.add(filterAsyncSectioned(i * 100, Math.min(size, i * 100 + 100), f))
    }
    for(job in jobs) {
        runBlocking {
            ret.addAll(job.await())
        }
    }
    return ret
}

private fun <T> List<T>.filterAsyncSectioned(from: Int, to: Int, f: (T) -> Boolean): Deferred<List<T>> {
    return async {
        val ret = mutableListOf<T>()
        for (i in this@filterAsyncSectioned.subList(from, to)) {
            if (f(i))
                ret.add(i)
        }
        ret
    }
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