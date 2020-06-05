package screen

import screen.elements.GUIDefaultTextureRectangle
import screen.elements.GUIResourceTypeSelection
import screen.elements.GUIWindow
import kotlin.system.measureTimeMillis

val Int.bitString: String get() = Integer.toBinaryString(this)

fun main() {
    measureTimeMillis {
        for (m in 0..10000) {
            for (n in 1..1000) {
                if (m % n != fastMod(m, n)) {
                    println("efasdf")
                }
            }
        }
    }.apply { println(this) }
}

fun slowMod(m: Int, n: Int): Int {
    if (n > m) {
        return m
    }
    if (2 * n > m) {
        return m - n
    }
    var k = 0
    while (k * n + n <= m) {
        k++
    }
    return m - k * n
}

fun fastMod(m: Int, n: Int): Int {
    if (n > m) {
        return m
    }
    var k = 1
    while(n shl k < m) {
        k++
    }
    return fastMod(m - (n shl (k - 1)), n)
}

internal object TestGUI : GUIWindow("Testing GUI", 0, 0, 60, 70, windowGroup = ScreenManager.Groups.INVENTORY) {

    init {
        GUIDefaultTextureRectangle(this, "test").apply {
            GUIResourceTypeSelection(this, "tester", { 1 }, { 54 }, allowRowGrowth = true)
        }
        generateDragGrip(layer = 5)
    }
}