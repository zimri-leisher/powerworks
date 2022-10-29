package serialization

import java.util.*

object SerializerDebugger {

    var alreadyPrinted = false
    const val safe = true
    const val bufferCount = 100
    const val on = false
    var debugDepth = 0
    val debugSpaces get() = (0 until (debugDepth % 16)).joinToString { "   " }

    val lastLinesBuffer = LinkedList<String>()

    fun writeln(text: String) {
        if (on) {
            println("[Serializer] $debugSpaces $text")
        } else if (safe) {
            lastLinesBuffer.add("[Serializer] $debugSpaces $text")
            if (lastLinesBuffer.size > bufferCount) {
                lastLinesBuffer.removeAt(0)
            }
        }
    }

    fun increaseDepth() {
        debugDepth++
    }

    fun decreaseDepth() {
        debugDepth--
    }

    fun <R> catchAndPrintIfSafe(block: () -> R): R {
        try {
            return block()
        } catch (e: Exception) {
            synchronized(lastLinesBuffer) {
                if (safe && !alreadyPrinted) {
                    alreadyPrinted = true
                    for (line in lastLinesBuffer) {
                        System.err.println(line)
                    }
                }
            }
            throw e
        }
    }
}