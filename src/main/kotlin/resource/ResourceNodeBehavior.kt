package resource

import level.Level
import main.State
import main.joinToString
import routing.RoutingLanguage
import routing.RoutingLanguageStatement
import screen.RoutingLanguageEditor.node

/**
 * The input/output behavior of this node. This encompasses which resource types are allowed to be inputted/outputted, as
 * well as which resources types to actively send from this node to the resource network and to actively request be sent
 * to this node from the resource network.
 * The behaviors are specified by routing language statements, which are parsed from text into a boolean expression in
 * the context of the node.
 */
class ResourceNodeBehavior(val node: ResourceNode<*>) {
    var allowIn = RoutingLanguageIORule()
        private set
    var allowOut = RoutingLanguageIORule()
        private set
    var forceIn = RoutingLanguageIORule()
        private set
    var forceOut = RoutingLanguageIORule()
        private set

    var allowModification = true

    private fun updateNodeAttachements() {
        if (State.CURRENT_STATE == State.INGAME) {
            Level.ResourceNodes.updateAttachments(node)
        }
    }

    inner class RoutingLanguageIORule(private val statements: MutableMap<RoutingLanguageStatement, List<ResourceType>?>) {
        constructor() : this(mutableMapOf())

        /**
         * Adds a statement and type pair to the IO rule.
         * @param statement the [RoutingLanguageStatement] that, when evaluated to true, will allow the [types]
         * @param types the list of [ResourceType] to allow when the [statement] is true. If null, it will allow all types
         */
        fun addStatement(statement: RoutingLanguageStatement, types: List<ResourceType>?) {
            if (allowModification) {
                statements.put(statement, types)
                updateNodeAttachements()
            }
        }

        /**
         * Removes all statements from the IO rule. This means that nothing will be allowed in or out
         */
        fun clearStatements() {
            if (allowModification) {
                statements.clear()
                updateNodeAttachements()
            }
        }

        /**
         * Removes all other statements and adds the specified one to the IO rule
         * @param statement the [RoutingLanguageStatement] that, when evaluated to true, will allow the [types]
         * @param types the list of [ResourceType] to allow when the [statement] is true. If null, it will allow all types
         */
        fun setStatement(statement: RoutingLanguageStatement, types: List<ResourceType>?) {
            clearStatements()
            addStatement(statement, types)
        }

        fun check(type: ResourceType) = statements.filterValues { it == null || type in it }.any { println("statement: ${it.key}"); it.key.evaluate(node) }

        fun possible(): List<ResourceType>? {
            val trueStatements = statements.filterKeys { it.evaluate(node) }
            val types = mutableListOf<ResourceType>()
            for (typeList in trueStatements.values) {
                if (typeList == null) {
                    return null
                }
                types.addAll(typeList)
            }
            return types
        }

        private fun <K, V> MutableMap<K, V>.copy(): MutableMap<K, V> {
            val map = mutableMapOf<K, V>()
            forEach { k, v -> map.put(k, v) }
            return map
        }

        fun copy() = RoutingLanguageIORule(statements.copy())

        override fun toString() = statements.joinToString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RoutingLanguageIORule

            if (statements != other.statements) return false

            return true
        }

        override fun hashCode(): Int {
            return statements.hashCode()
        }
    }

    fun copy(node: ResourceNode<*>) = ResourceNodeBehavior(node).apply {
        allowIn = this@ResourceNodeBehavior.allowIn.copy()
        allowOut = this@ResourceNodeBehavior.allowOut.copy()
        forceIn = this@ResourceNodeBehavior.forceIn.copy()
        forceOut = this@ResourceNodeBehavior.forceOut.copy()
    }

    override fun toString() = "allow in: $allowIn\nallow out: $allowOut\nforce in: $forceIn\nforce out: $forceOut"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResourceNodeBehavior

        if (node != other.node) return false
        if (allowIn != other.allowIn) return false
        if (allowOut != other.allowOut) return false
        if (forceIn != other.forceIn) return false
        if (forceOut != other.forceOut) return false
        if (allowModification != other.allowModification) return false

        return true
    }

    override fun hashCode(): Int {
        var result = node.hashCode()
        result = 31 * result + allowIn.hashCode()
        result = 31 * result + allowOut.hashCode()
        result = 31 * result + forceIn.hashCode()
        result = 31 * result + forceOut.hashCode()
        result = 31 * result + allowModification.hashCode()
        return result
    }
}

