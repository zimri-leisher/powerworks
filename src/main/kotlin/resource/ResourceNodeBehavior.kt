package resource

import level.Level
import main.State
import main.joinToString
import routing.RoutingLanguageStatement

private fun <K, V> MutableMap<K, V>.copy(): MutableMap<K, V> {
    val map = mutableMapOf<K, V>()
    forEach { k, v -> map.put(k, v) }
    return map
}

/**
 * The input/output behavior of this node. This encompasses which resource types are allowed to be inputted/outputted, as
 * well as which resources types to actively send from this node to the resource network and to actively request be sent
 * to this node from the resource network.
 * The behaviors are specified by routing language statements, which are parsed from text into a boolean expression in
 * the context of the node.
 */
class ResourceNodeBehavior(val node: ResourceNode) {
    var allowIn = RoutingLanguageIORule()
        private set
    var allowOut = RoutingLanguageIORule()
        private set
    var forceIn = RoutingLanguageIORule(RoutingLanguageStatement.FALSE)
        private set
    var forceOut = RoutingLanguageIORule(RoutingLanguageStatement.FALSE)
        private set

    var allowModification = true

    private fun updateAttachments() {
        if (State.CURRENT_STATE == State.INGAME) {
            Level.ResourceNodes.updateAttachments(node)
        }
    }

    /**
     * A rule specifying any number of [RoutingLanguageStatement]s and the [ResourceType]s they correspond to.
     * [check] returns true if there are no statements, so by default, this will always evaluate to true
     */
    inner class RoutingLanguageIORule(val statements: MutableMap<RoutingLanguageStatement, List<ResourceType>?>) {
        constructor() : this(mutableMapOf())
        constructor(statement: RoutingLanguageStatement) : this(mutableMapOf<RoutingLanguageStatement, List<ResourceType>?>(statement to null))

        /**
         * Adds a statement and type pair to the IO rule.
         * @param statement the [RoutingLanguageStatement] that, when evaluated to true, will allow the [types]
         * @param types the list of [ResourceType] to allow when the [statement] is true. If null, it will allow all types
         */
        fun addStatement(statement: RoutingLanguageStatement, types: List<ResourceType>?) {
            if (allowModification) {
                statements.put(statement, types)
                updateAttachments()
            }
        }

        /**
         * Removes all statements from the IO rule. This means that nothing will be allowed in or out
         */
        fun clearStatements() {
            if (allowModification) {
                statements.clear()
                updateAttachments()
            }
        }

        /**
         * Removes all other statements and adds the specified one to the IO rule
         * @param statement the [RoutingLanguageStatement] that, when evaluated to true, will allow the [types]
         * @param types the list of [ResourceType] to allow when the [statement] is true. If null, it will allow all types
         */
        fun setStatement(statement: RoutingLanguageStatement, types: List<ResourceType>? = null) {
            clearStatements()
            addStatement(statement, types)
        }

        /**
         * Checks whether this resource type passes any of the statements. If there are no statements, it returns true
         */
        fun check(type: ResourceType): Boolean {
            return statements.isEmpty() || statements.filterValues { it == null || type in it }.any { it.key.evaluate(node) }
        }

        /**
         * @return a list of ResourceTypes that match statements that are currently true, or null if a statement that
         * allows for any ResourceType is currently true
         */
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

    fun copy(node: ResourceNode) = ResourceNodeBehavior(node).apply {
        allowIn = RoutingLanguageIORule(this@ResourceNodeBehavior.allowIn.statements.copy())
        allowOut = RoutingLanguageIORule(this@ResourceNodeBehavior.allowOut.statements.copy())
        forceIn = RoutingLanguageIORule(this@ResourceNodeBehavior.forceIn.statements.copy())
        forceOut = RoutingLanguageIORule(this@ResourceNodeBehavior.forceOut.statements.copy())
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

