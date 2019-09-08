package resource

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.Hitbox
import level.Level
import level.add
import misc.Geometry
import routing.ResourceRoutingNetwork

/**
 * A node that allows for movement of resources between places on the level. This is not a subclass of LevelObject.
 *
 * An example of a place where they appear is the MinerBlock, which uses one to produce the ore it mines
 * from the ground and either put it into the connected inventory or place it on the ground.
 */
class ResourceNode(
        @Tag(1)
        val xTile: Int,
        @Tag(2)
        val yTile: Int,
        @Tag(3)
        val dir: Int,
        @Tag(4)
        val resourceCategory: ResourceCategory,
        @Tag(5)
        var attachedContainer: ResourceContainer,
        /**
         * The [Level] this node is in, or will be in if it has not been [added][add] already
         */
        @Tag(6)
        val level: Level) {

    /**
     * If this node is in the level, meaning it is able to interact with other nodes in the level
     */
    @Tag(7)
    var inLevel = false


    /**
     * Adjacent nodes facing towards this node.
     * This is where resources will get sent to when outputting
     */
    @Tag(8)
    var attachedNodes: List<ResourceNode> = listOf()

    /**
     * Whether or not this node should be allowed to output resources directly to the level, using the Level.add(ResourceType, Int) method.
     * If false, calls to can/couldOutput will return false if there is no attached node, regardless of whether there is space in the level
     */
    @Tag(9)
    var outputToLevel = true

    /**
     * The resource routing network which this is part of
     */
    @Tag(10)
    var network = ResourceRoutingNetwork(resourceCategory, level)
    @Tag(11)
    var isInternalNetworkNode = false

    /**
     * The input and output behavior of this node. This is where routing language gets put into and evaluated
     */
    @Tag(12)
    var behavior = ResourceNodeBehavior(this)
        private set

    init {
        ALL.add(this)
    }

    /**
     * @param mustContainEnough whether or not to check if the attached container has enough resources. Set to false if
     * you know it does or don't care if it doesn't
     * @return if the resource is the right type and this node allows output and the attached container is able to remove the resources.
     * Additionally, if there is an attached node, it must be able to input the resources
     */
    fun canOutput(resource: ResourceType, quantity: Int, mustContainEnough: Boolean = true): Boolean {
        if (!isRightType(resource))
            return false
        if (!behavior.allowOut.check(resource)) {
            return false
        }
        if (mustContainEnough)
            if (!attachedContainer.canRemove(resource, quantity))
                return false
        if (attachedNodes.isNotEmpty()) {
            if (attachedNodes.none { it.canInput(resource, quantity) })
                return false
        } else if (attachedNodes.isEmpty() && !outputToLevel) {
            return false
        }
        return true
    }

    /**
     * @param mustHaveSpace whether or not to check if the attached container has enough space. Set to false if you know
     * it does or don't care if it doesn't. This means it will only check the type and behavior of the node (the behavior
     * may still check for space)
     * @return if the resource is the right type and this node allows input and the attached container can add the resources
     */
    fun canInput(resource: ResourceType, quantity: Int, mustHaveSpace: Boolean = true): Boolean {
        if (!isRightType(resource))
            return false
        if (!behavior.allowIn.check(resource)) {
            return false
        }
        if (mustHaveSpace) {
            if (!attachedContainer.canAdd(resource, quantity))
                return false
        }
        return true
    }

    /**
     * Outputs the resources either directly to the level or into a connected node
     * @param resource the resource type to be outputted. Will only output ones that match the resource category
     * @param quantity the amount of resources to be outputted
     * @param checkIfAble whether or not to check if the node is able to output. This should be false only
     * to avoid redundant checks to the attached container or node (i.e., for performance). Setting it to false without being
     * sure about the ability of the attached node to input and the attached container to remove has an undefined behavior
     * and will probably result in duplications or crashes.
     * @param mustContainEnough this specifies whether or not the check for ability to output should check if the attached container
     * contains enough resources. Does nothing if checkIfAble is false
     */
    fun output(resource: ResourceType, quantity: Int, checkIfAble: Boolean = true, mustContainEnough: Boolean = true): Boolean {
        if (checkIfAble) {
            if (!canOutput(resource, quantity, mustContainEnough))
                return false
        }
        attachedContainer.remove(resource, quantity, this, false)
        if (attachedNodes.isNotEmpty()) {
            // we already checked if we were able to, everything here is under the assumption it is successful
            for (attachedNode in attachedNodes) {
                if (attachedNode.input(resource, quantity, false)) {
                    break
                }
            }
        } else if (outputToLevel && inLevel) {
            // TODO make this better some time, have it actually spawn in the center
            val xSign = Geometry.getXSign(dir)
            val ySign = Geometry.getYSign(dir)
            level.add(((xTile shl 4) + 7) + (8 + Hitbox.DROPPED_ITEM.width) * xSign, ((yTile shl 4) + 7) + (8 + Hitbox.DROPPED_ITEM.height) * ySign, resource, quantity)
        }
        return true
    }

    /**
     * Inputs the resources into the attached container
     * @param resource the resource type to be inputted. Will only input ones that match the resource category
     * @param quantity the amount of resources to be inputted
     * @param checkIfAble whether or not to check if the node is able to input. This should be false only
     * to avoid redundant checks to the attached container (i.e., for performance). Setting it to false without being
     * sure about the ability of the attached container to add has an undefined behavior and will probably result in duplications or crashes.
     * @param mustHaveSpace this specifies whether or not the check for ability to input should check if the attached container
     * has enough space. Does nothing if checkIfAble is false
     */
    fun input(resource: ResourceType, quantity: Int, checkIfAble: Boolean = true, mustHaveSpace: Boolean = true): Boolean {
        if (checkIfAble) {
            if (!canInput(resource, quantity, mustHaveSpace))
                return false
        }
        attachedContainer.add(resource, quantity, this, false)
        return true
    }

    fun update() {
        for (resource in attachedContainer.typeList()) {
            if (behavior.forceIn.check(resource)) {
                network.sendTo(this, resource, 1)
            }
            if (behavior.forceOut.check(resource)) {
                network.takeFrom(this, resource, 1)
            }
        }
    }

    /**
     * @return if the resource parameter is of the right resource category
     */
    fun isRightType(resource: ResourceType) = resource.category == resourceCategory

    fun copy(xTile: Int = this.xTile, yTile: Int = this.yTile, dir: Int = this.dir, attachedContainer: ResourceContainer = this.attachedContainer, outputToLevel: Boolean = this.outputToLevel) =
            ResourceNode(xTile, yTile, dir, resourceCategory, attachedContainer, level).apply {
                this.behavior = this@ResourceNode.behavior.copy(this)
                this.outputToLevel = outputToLevel
            }

    override fun toString() = "Resource node at $xTile, $yTile, dir: $dir, behavior: \n$behavior"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ResourceNode
        if (xTile != other.xTile) return false
        if (yTile != other.yTile) return false
        if (dir != other.dir) return false
        if (resourceCategory != other.resourceCategory) return false
        if (attachedContainer != other.attachedContainer) return false
        if (inLevel != other.inLevel) return false
        if (behavior != other.behavior) return false
        return true
    }

    override fun hashCode(): Int {
        var result = xTile
        result = 31 * result + yTile
        result = 31 * result + dir
        result = 31 * result + resourceCategory.hashCode()
        result = 31 * result + attachedContainer.hashCode()
        result = 31 * result + inLevel.hashCode()
        return result
    }

    companion object {
        val ALL = mutableListOf<ResourceNode>()

        fun update() {
            ALL.forEach { it.update() }
        }

        fun createCorresponding(n: ResourceNode, attachedContainer: ResourceContainer, behavior: ResourceNodeBehavior) =
                ResourceNode(n.xTile + Geometry.getXSign(n.dir), n.yTile + Geometry.getYSign(n.dir), Geometry.getOppositeAngle(n.dir), n.resourceCategory, attachedContainer, n.level).apply {
                    this.behavior = behavior
                }
    }

}