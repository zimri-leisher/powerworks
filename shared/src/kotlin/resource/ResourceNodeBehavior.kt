package resource

import item.Inventory
import level.LevelManager
import level.updateResourceNodeAttachments
import main.GameState
import main.joinToString
import routing.script.RoutingLanguage
import routing.script.RoutingLanguageStatement
import serialization.Id

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
class ResourceNodeBehavior(
        @Id(1)
        var node: ResourceNodeOld) {

    private constructor() : this(ResourceNodeOld(0, 0, 0, ResourceCategory.ITEM, Inventory(0, 0), LevelManager.EMPTY_LEVEL))

    @Id(2)
    var allowIn = RoutingLanguageIORule(this)
        private set

    @Id(3)
    var allowOut = RoutingLanguageIORule(this)
        private set

    @Id(4)
    var forceIn = RoutingLanguageIORule(this, RoutingLanguage.FALSE)
        private set

    @Id(5)
    var forceOut = RoutingLanguageIORule(this, RoutingLanguage.FALSE)
        private set

    @Id(6)
    var allowModification = true

    fun updateAttachments() {
        if (GameState.currentState == GameState.INGAME) {
            node.level.updateResourceNodeAttachments(node)
        }
    }

    private fun copy(statements: MutableMap<RoutingLanguageStatement, List<ResourceType>>): MutableMap<RoutingLanguageStatement, List<ResourceType>> {
        val new = mutableMapOf<RoutingLanguageStatement, List<ResourceType>>()
        for ((statement, types) in statements) {
            val newTypes = mutableListOf<ResourceType>()
            types.forEach { newTypes.add(it) }
            new.put(statement, newTypes)
        }
        return new
    }

    fun copy(node: ResourceNodeOld) = ResourceNodeBehavior(node).apply {
        allowIn = RoutingLanguageIORule(this, copy(this@ResourceNodeBehavior.allowIn.statements))
        allowOut = RoutingLanguageIORule(this, copy(this@ResourceNodeBehavior.allowOut.statements))
        forceIn = RoutingLanguageIORule(this, copy(this@ResourceNodeBehavior.forceIn.statements))
        forceOut = RoutingLanguageIORule(this, copy(this@ResourceNodeBehavior.forceOut.statements))
    }

    override fun toString() = "allow in: $allowIn\nallow out: $allowOut\nforce in: $forceIn\nforce out: $forceOut"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResourceNodeBehavior

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

    companion object {
        val EMPTY_BEHAVIOR = ResourceNodeBehavior(ResourceNodeOld(0, 0, 0, ResourceCategory.ITEM, Inventory(0, 0), LevelManager.EMPTY_LEVEL))
    }
}

/**
 * A rule specifying any number of [RoutingLanguageStatement]s and the [ResourceType]s they correspond to.
 * [check] returns true if there are no statements, so by default, this will always evaluate to true
 */
class RoutingLanguageIORule(
        @Id(1)
        val parent: ResourceNodeBehavior,
        @Id(2)
        val statements: MutableMap<RoutingLanguageStatement, List<ResourceType>>) {
    private constructor() : this(ResourceNodeBehavior.EMPTY_BEHAVIOR, mutableMapOf())
    constructor(parent: ResourceNodeBehavior, statement: RoutingLanguageStatement) : this(parent, mutableMapOf<RoutingLanguageStatement, List<ResourceType>>(statement to listOf()))
    constructor(parent: ResourceNodeBehavior) : this(parent, mutableMapOf())

    /**
     * Adds a statement and type pair to the IO rule.
     * @param statement the [RoutingLanguageStatement] that, when evaluated to true, will allow the [types]
     * @param types the list of [ResourceType] to allow when the [statement] is true. If empty, it will allow all types
     */
    fun addStatement(statement: RoutingLanguageStatement, types: List<ResourceType> = listOf()) {
        if (parent.allowModification) {
            statements.put(statement, types)
            parent.updateAttachments()
        }
    }

    /**
     * Removes all statements from the IO rule. This means that nothing will be allowed in or out
     */
    fun clearStatements() {
        if (parent.allowModification) {
            statements.clear()
            parent.updateAttachments()
        }
    }

    /**
     * Removes all other statements and adds the specified one to the IO rule
     * @param statement the [RoutingLanguageStatement] that, when evaluated to true, will allow the [types]
     * @param types the list of [ResourceType] to allow when the [statement] is true. If empty, it will allow all types
     */
    fun setStatement(statement: RoutingLanguageStatement, types: List<ResourceType> = listOf()) {
        clearStatements()
        addStatement(statement, types)
    }

    /**
     * Checks whether this resource type passes any of the statements. If there are no statements, it returns false
     */
    fun check(type: ResourceType): Boolean {
        if (statements.isEmpty()) {
            return false
        }
        // FIXME
//        if (statements.filterValues { it.isEmpty() || type in it }.any { it.key.evaluate(parent.node) }) {
//            return true
//        }
        return false
    }

    /**
     * @return a list of ResourceTypes that match statements that are currently true, or an empty list if a statement that
     * allows for any ResourceType is currently true, or null if none are allowed to be true
     */
    fun possible(): List<ResourceType>? {
        val trueStatements = statements // FIXME statements.filterKeys { it.evaluate(parent.node) }
        val types = mutableListOf<ResourceType>()
        for (typeList in trueStatements.values) {
            if (typeList.isEmpty()) {
                return listOf()
            }
            types.addAll(typeList)
        }
        if(types.isEmpty()) {
            return null
        }
        return types
    }

    override fun toString() = statements.joinToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutingLanguageIORule

        if (other.statements.size != statements.size)
            return false

        for ((statement, types) in statements) {
            val otherTypes = other.statements[statement]
            if (otherTypes != types)
                return false
        }

        return true
    }

    override fun hashCode(): Int {
        return statements.hashCode()
    }
}
