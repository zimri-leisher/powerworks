package misc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext

val collection = mutableListOf<Int>()

fun main() {
    loop()
}

fun loop() {
    val scope = CoroutineScope(newFixedThreadPoolContext(4, "synchPool"))
    for (i in 0..100) {
        scope.launch {
            synchronized(collection) {
                collection.add(i)
            }
        }
    }
    while (true) {
        tick()
        Thread.sleep(5)
    }
}

fun tick() {
    synchronized(collection) {
        for (i in collection) {
            println(i)
        }
    }
}
