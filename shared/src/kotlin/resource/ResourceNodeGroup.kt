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
fun ResourceNodeGroup.getAttachedContainers() = map { it.attachedContainer }.distinct()

/**
 * @return a [ResourceNodeGroup] of all unique nodes attached to ones in this node group
 */
fun ResourceNodeGroup.getAttachedNodes() = map { it.attachedNode }.distinct()

/**
 * @param onlyTo a predicate for which nodes are considered as options
 * @param mustHaveSpace whether or not to check if the containers the nodes are attached to have enough space.
 * Set to false if you know they do or don't care if they don't
 * @return a [ResourceNodeGroup] of nodes that are able to input the given resource type with the given quantity
 */
fun ResourceNodeGroup.getInputters(type: ResourceType, quantity: Int = 1,  mustHaveSpace: Boolean = true, accountForExpected: Boolean = false) =
        filter { it.canInput(type, quantity, mustHaveSpace, accountForExpected) }

/**
 * @param onlyTo a predicate for which nodes are considered as options
 * @param mustContainEnough whether or not to check if the container the node is attached to has enough resources.
 * Set to false if you know it does or don't care if it doesn't
 * @return a [ResourceNodeGroup] of nodes that are able to output the given resource type with the given quantity
 */
fun ResourceNodeGroup.getOutputters(type: ResourceType, quantity: Int, mustContainEnough: Boolean = true) =
        filter { it.canOutputAll(type, quantity, mustContainEnough) }

/**
 * @param onlyTo a predicate for which nodes are considered as options
 * @param mustContainEnough whether or not to check if the container the node is attached to has enough resources.
 * Set to false if you know it does or don't care if it doesn't
 * @return the first node that can output the given resource type with the given quantity
 */
fun ResourceNodeGroup.getOutputter(type: ResourceType, quantity: Int = 1, mustContainEnough: Boolean = true) =
        firstOrNull {
            it.canOutputAll(type, quantity, mustContainEnough)
        }
fun ResourceNodeGroup.getForceOutputters(type: ResourceType, quantity: Int = 1, mustContainEnough: Boolean = true) =
        filter { it.behavior.forceOut.check(type) && it.canOutputAll(type, quantity, mustContainEnough) }

/**
 * @param mustContainEnough whether or not to check if the container the node is attached to has enough resources.
 * Set to false if you know it does or don't care if it doesn't
 * @return the first node that is able to, and trying to force output the given resource type that has the given quantity
 */
fun ResourceNodeGroup.getForceOutputter(type: ResourceType, quantity: Int = 1, mustContainEnough: Boolean = true) =
        firstOrNull { it.behavior.forceOut.check(type) && it.canOutputAll(type, quantity, mustContainEnough) }

fun ResourceNodeGroup.getForceInputters(type: ResourceType, quantity: Int = 1, mustHaveSpace: Boolean = true, accountForExpected: Boolean = false) =
        filter { it.behavior.forceIn.check(type) && it.canInput(type, quantity, mustHaveSpace, accountForExpected) }


/**
 * @param mustHaveSpace whether or not to check if the container the node is attached to has enough space.
 * Set to false if you know it does or don't care if it doesn't
 * @return the first node that is able to, and trying to force input the given resource type that has the given quantity
 */
fun ResourceNodeGroup.getForceInputter(type: ResourceType, quantity: Int = 1, mustHaveSpace: Boolean = true, accountForExpected: Boolean = false) =
        firstOrNull { it.behavior.forceIn.check(type) && it.canInput(type, quantity, mustHaveSpace, accountForExpected) }

/**
 * @param mustHaveSpace whether or not to check if the container the node is attached to has enough space.
 * Set to false if you know it does or don't care if it doesn't
 * @return the first node that can input the given resource type with the given quantity
 */
fun ResourceNodeGroup.getInputter(type: ResourceType, quantity: Int = 1, mustHaveSpace: Boolean = true, accountForExpected: Boolean = false) =
        firstOrNull {
            it.canInput(type, quantity, mustHaveSpace, accountForExpected)
        }

/**
 * Tries to output the resource with the specified quantity
 * @param mustContainEnough whether or not to check if the node this is outputting through has a container which has
 * enough resources. Set to false if you know it does or don't care if it doesn't
 */
fun ResourceNodeGroup.output(type: ResourceType, quantity: Int, mustContainEnough: Boolean = true): Boolean {
    return getOutputter(type, quantity, mustContainEnough)?.output(type, quantity, false) == true
}

/**
 * Tries outputting every resource in this list from any available node in this group
 * @param mustContainEnough whether or not to check if the nodes this is outputting through have containers which have
 * enough resources. Set to false if you know it does or don't care if it doesn't
 * @return true if all were successfully outputted
 */
fun ResourceNodeGroup.output(list: ResourceList, mustContainEnough: Boolean = true): Boolean {
    return !list.any { !output(it.key, it.value, mustContainEnough) }
}

/**
 * Whether or not this node group is able to output the resources with the given quantity
 * @param mustContainEnough whether the attached containers of the nodes must contain enough of the given resources
 */
fun ResourceNodeGroup.canOutputAll(type: ResourceType, quantity: Int, mustContainEnough: Boolean = true) = getOutputter(type, quantity, mustContainEnough) != null

/**
 * Whether or not this node group is able to output the resources with the given quantity
 * @param mustContainEnough whether the attached containers of the nodes must contain enough of the given resources
 * @return true if every (resource, quantity) pair was outputted successfully
 */
fun ResourceNodeGroup.canOutputAll(list: ResourceList, mustContainEnough: Boolean = true): Boolean {
    return list.all { (r, q) -> canOutputAll(r, q, mustContainEnough) }
}

fun ResourceNodeGroup.getPartialOutputters(resources: ResourceList, mustContainEnough: Boolean = true) =
        mapNotNull {
            val possible = it.mostPossibleToOutput(resources, mustContainEnough)
            if(possible.totalQuantity == 0) {
                null
            } else {
                it to possible
            }
        }.toMap()

fun ResourceNodeGroup.getPartialInputters(resources: ResourceList, mustContainEnough: Boolean = true) =
        mapNotNull {
            val possible = it.mostPossibleToInput(resources, mustContainEnough)
            if(possible.isEmpty()) {
                null
            } else {
                it to possible
            }
        }.toMap()