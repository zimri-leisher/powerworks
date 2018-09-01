package routing

import resource.ResourceNode
import resource.ResourceType

class EvaluationException(message: String?) : Exception(message)

class Expression<T>(
        /**
         * A formatting string for inputting arguments. Use '$<argument number>', where <argument number> is replaced with a single digit
         * number, as a placeholder for where the arguments will go. Whatever the digit is will be the index of the argument in the array.
         * Any text will be matched exactly, but not case-sensitively
         */
        val format: String,
        vararg val otherFormats: String,
        private val evaluate: (context: ResourceNode<*>, arguments: Array<String>) -> T) {

    /**
     * Evaluates the expression
     * @param context the resource node which this is relative to
     * @param arguments the list of arguments in the positions specified by the format string of the expression
     */
    fun evaluate(context: ResourceNode<*>, arguments: Array<String>): T {
        try {
            return evaluate(context, arguments)
        } catch (e: Throwable) {
            throw EvaluationException(e.message)
        }
    }

    companion object {
        val BOOLEAN_LITERAL = Expression("$0b") { _, arguments ->
            arguments[0].toBoolean()
        }
        val INT_LITERAL = Expression("$0i") { _, arguments ->
            arguments[0].toInt()
        }
        val CONTAINS = Expression("contains $1 of $0") { context, arguments ->
            val possible = ResourceType.possibleResourceTypes(arguments[0])
            if (possible.size > 1) {
                throw EvaluationException("More than one possible resource type matches the input '${arguments[0]}'")
            }
            return@Expression context.network.contains(possible[0], arguments[1].toInt())
        }
        val GET_QUANTITY = Expression("quantity of $0") {context, arguments ->
            val possible = ResourceType.possibleResourceTypes(arguments[0])
            if (possible.size > 1) {
                throw EvaluationException("More than one possible resource type matches the input '${arguments[0]}'")
            }
            return@Expression context.network.getQuantity(possible[0])
        }
    }
}