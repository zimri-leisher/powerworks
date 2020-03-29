package resource

/**
 * A group of resource nodes of any type
 *
 * Instead of keeping track of individual nodes, you can just add them to this and depend on this to send resources to
 * and from the correct places
 *
 * For example, every block instance has an instance of this which is where the node templates are put into. In the case of
 * the MinerBlock, instead of the it having an outputNode attribute which is manually set to the correct one, it simply
 * calls nodes.output(resource, quantity) which finds the available node and outputs the resources
 */
typealias ResourceNodeGroup = Collection<ResourceNode>

/**
 * @return a list of all unique attached containers in this node group
 */
fun ResourceNodeGroup.getAttachedContainers() = map { it.attachedContainer }.distinct().toMutableList()

/**
 * @return a [ResourceNodeGroup] of all unique nodes attached to ones in this node group
 */
fun ResourceNodeGroup.getAttachedNodes() = map { it.attachedNode }.distinct()

/**
 * @param onlyTo a predicate for which nodes are considered as options
 * @param mustHaveSpace whether or not to check if the containers the nodes are attached to have enough space.
 * Set to false if you know they do or don't care if they don't
 * @return a [ResourceNodeGroup] of nodes that are able to input the given resource type with the given quantity while matching the onlyTo predicate
 */
fun ResourceNodeGroup.getInputters(type: ResourceType, quantity: Int = 1, onlyTo: (ResourceNode) -> Boolean = { true }, mustHaveSpace: Boolean = true) =
        filter { onlyTo(it) && it.canInput(type, quantity, mustHaveSpace) }

/**
 * @param onlyTo a predicate for which nodes are considered as options
 * @param mustContainEnough whether or not to check if the container the node is attached to has enough resources.
 * Set to false if you know it does or don't care if it doesn't
 * @return a [ResourceNodeGroup] of nodes that are able to output the given resource type with the given quantity while matching the onlyTo predicate
 */
fun ResourceNodeGroup.getOutputters(type: ResourceType, quantity: Int, onlyTo: (ResourceNode) -> Boolean = { true }, mustContainEnough: Boolean = true) =
        filter { onlyTo(it) && it.canOutput(type, quantity, mustContainEnough) }

/**
 * @param onlyTo a predicate for which nodes are considered as options
 * @param mustContainEnough whether or not to check if the container the node is attached to has enough resources.
 * Set to false if you know it does or don't care if it doesn't
 * @return the first node that can output the given resource type with the given quantity while matching the onlyTo predicate
 */
fun ResourceNodeGroup.getOutputter(type: ResourceType, quantity: Int = 1, onlyTo: (ResourceNode) -> Boolean = { true }, mustContainEnough: Boolean = true) =
        firstOrNull {
            onlyTo(it) && it.canOutput(type, quantity, mustContainEnough)
        }

/**
 * @param onlyTo a predicate for which nodes are considered as options
 * @param mustContainEnough whether or not to check if the container the node is attached to has enough resources.
 * Set to false if you know it does or don't care if it doesn't
 * @return the first node that is able to, and trying to force output the given resource type that has the given quantity and matches the onlyTo predicate
 */
fun ResourceNodeGroup.getForceOutputter(type: ResourceType, quantity: Int = 1, onlyTo: (ResourceNode) -> Boolean = { true }, mustContainEnough: Boolean = true) =
        firstOrNull { onlyTo(it) && it.behavior.forceOut.check(type) && it.canOutput(type, quantity, mustContainEnough) }

/**
 * @param onlyTo a predicate for which nodes are considered as options
 * @param mustHaveSpace whether or not to check if the container the node is attached to has enough space.
 * Set to false if you know it does or don't care if it doesn't
 * @return the first node that is able to, and trying to force input the given resource type that has the given quantity and matches the onlyTo predicate
 */
fun ResourceNodeGroup.getForceInputter(type: ResourceType, quantity: Int = 1, onlyTo: (ResourceNode) -> Boolean = { true }, mustHaveSpace: Boolean = true) =
        firstOrNull { onlyTo(it) && it.behavior.forceIn.check(type) && it.canInput(type, quantity, mustHaveSpace) }

/**
 * @param onlyTo a predicate for which nodes are considered as options
 * @param mustHaveSpace whether or not to check if the container the node is attached to has enough space.
 * Set to false if you know it does or don't care if it doesn't
 * @return the first node that can input the given resource type with the given quantity while matching the onlyTo predicate
 */
fun ResourceNodeGroup.getInputter(type: ResourceType, quantity: Int = 1, onlyTo: (ResourceNode) -> Boolean = { true }, mustHaveSpace: Boolean = true) =
        firstOrNull {
            onlyTo(it) && it.canInput(type, quantity, mustHaveSpace)
        }

/**
 * Tries to output the resource with the specified quantity
 * @param mustContainEnough whether or not to check if the node this is outputting through has a container which has
 * enough resources. Set to false if you know it does or don't care if it doesn't
 * @param onlyTo a predicate determining which nodes it will output through
 */
fun ResourceNodeGroup.output(type: ResourceType, quantity: Int, onlyTo: (ResourceNode) -> Boolean = { true }, mustContainEnough: Boolean = true): Boolean {
    return getOutputter(type, quantity, onlyTo, mustContainEnough)?.output(type, quantity, false) == true
}

/**
 * Tries outputting every resource in this list from any available node in this group
 * @param mustContainEnough whether or not to check if the nodes this is outputting through have containers which have
 * enough resources. Set to false if you know it does or don't care if it doesn't
 * @return true if all were successfully outputted
 */
fun ResourceNodeGroup.output(list: ResourceList, onlyTo: (ResourceNode) -> Boolean = { true }, mustContainEnough: Boolean = true): Boolean {
    return !list.any { !output(it.key, it.value, onlyTo, mustContainEnough) }
}

/**
 * Whether or not this node group is able to output the resources with the given quantity
 * @param onlyTo only consider for outputting nodes that match this predicate
 * @param mustContainEnough whether the attached containers of the nodes must contain enough of the given resources
 */
fun ResourceNodeGroup.canOutput(type: ResourceType, quantity: Int, onlyTo: (ResourceNode) -> Boolean = { true }, mustContainEnough: Boolean = true) = getOutputter(type, quantity, onlyTo, mustContainEnough) != null

/**
 * Whether or not this node group is able to output the resources with the given quantity
 * @param onlyTo only consider for outputting nodes that match this predicate
 * @param mustContainEnough whether the attached containers of the nodes must contain enough of the given resources
 * @return true if every (resource, quantity) pair was outputted successfully
 */
fun ResourceNodeGroup.canOutput(list: ResourceList, onlyTo: (ResourceNode) -> Boolean = { true }, mustContainEnough: Boolean = true): Boolean {
    return list.all { (r, q) -> canOutput(r, q, onlyTo, mustContainEnough) }
}