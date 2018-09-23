package resource

import level.Hitbox
import level.Level
import misc.GeometryHelper
import routing.ResourceRoutingNetwork

/**
 * A node that allows for movement of resources between places on the level. While it exists in the level, it is not rendered
 * by default and is not a subclass of LevelObject.
 *
 * An example of a place where they appear is the MinerBlock, which uses one with allowOut = true to produce the ore it mines
 * from the ground and either put it into the connected inventory or place it on the ground.
 */
class ResourceNode<R : ResourceType>(
        val xTile: Int, val yTile: Int,
        val dir: Int,
        val resourceCategory: ResourceCategory,
        var allowIn: Boolean = false, var allowOut: Boolean = false,
        var attachedContainer: ResourceContainer<R>) {

    /**
     * If this node is in the level, meaning it is able to interact with other nodes in the level
     */
    var inLevel = false
    /**
     * An adjacent node facing towards this node which can receive input (if this can output) and can output (if this can receive inputs)
     * This is where resources will get sent to when outputting
     */
    var attachedNode: ResourceNode<R>? = null

    /**
     * Whether or not this node should be allowed to output resources directly to the level, using the Level.add(ResourceType, Quantity) method.
     * If false, calls to can/couldOutput will return false if there is no attached node, regardless of whether there is space in the level
     */
    var outputToLevel = true

    /**
     * The resource routing network to which this is part of
     */
    var network: ResourceRoutingNetwork<R> = ResourceRoutingNetwork()

    /**
     * @return if the resource is the right type and this node allows output and the attached container is able to remove the resources.
     * Additionally, if there is an attached node, it must be able to input the resources
     */
    fun canOutput(resource: ResourceType, quantity: Int): Boolean {
        if (!isRightType(resource))
            return false
        resource as R
        if (!allowOut)
            return false
        if (!attachedContainer.canRemove(resource, quantity))
            return false
        if (attachedNode != null) {
            if (!attachedNode!!.canInput(resource, quantity))
                return false
        } else if (attachedNode == null && !outputToLevel)
            return false
        return true
    }

    /**
     * @return if the resource is the right type and this node allows input and the attached container can add the resources
     */
    fun canInput(resource: ResourceType, quantity: Int): Boolean {
        if (!isRightType(resource))
            return false
        resource as R
        if (!allowIn)
            return false
        if (!attachedContainer.canAdd(resource, quantity))
            return false
        return true
    }

    /**
     * If this node could output the resources assuming its attached container has enough.
     * To summarize, the difference between this and canOutput is this doesn't check if the attached container can remove the resources
     *
     * @return if the resource is the right type and this node allows output and the attached node can input the resources
     */
    fun couldOuput(resource: ResourceType, quantity: Int): Boolean {
        if (!isRightType(resource))
            return false
        resource as R
        if (!allowOut)
            return false
        if (attachedNode != null) {
            if (!attachedNode!!.canInput(resource, quantity))
                return false
        } else if (attachedNode == null && !outputToLevel)
            return false
        return true
    }

    /**
     * If this node could input the resources assuming its attached container has space.
     * To summzrize, the difference between this and canInput is this doesn't check if the attached container can add the resources
     *
     * @return if the resource is the right type and this node allows input
     */
    fun couldInput(resource: ResourceType, quantity: Int): Boolean {
        if (!isRightType(resource))
            return false
        resource as R
        if (!allowIn)
            return false
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
            if (mustContainEnough) {
                if (!canOutput(resource, quantity))
                    return false
            } else {
                if (!couldOuput(resource, quantity))
                    return false
            }
        }
        attachedContainer.remove(resource, quantity, this, false)
        if (attachedNode != null) {
            // we already checked if we were able to, everything here is under the assumption it is successful
            attachedNode!!.input(resource, quantity, false)
        } else if (outputToLevel) {
            // TODO make this better some time, have it actually spawn in the center
            val xSign = GeometryHelper.getXSign(dir)
            val ySign = GeometryHelper.getYSign(dir)
            Level.add(((xTile shl 4) + 7) + (8 + Hitbox.DROPPED_ITEM.width) * xSign, ((yTile shl 4) + 7) + (8 + Hitbox.DROPPED_ITEM.height) * ySign, resource, quantity) == quantity
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
            if (mustHaveSpace) {
                if (!canInput(resource, quantity))
                    return false
            } else {
                if (!couldInput(resource, quantity))
                    return false
            }
        }
        attachedContainer.add(resource, quantity, this, false)
        return true
    }

    /**
     * @return if the resource parameter is of the right resource category
     */
    fun isRightType(resource: ResourceType) = resource.category == resourceCategory

    fun copy(xTile: Int = this.xTile, yTile: Int = this.yTile, dir: Int = this.dir, allowIn: Boolean = this.allowIn, allowOut: Boolean = this.allowOut, attachedContainer: ResourceContainer<*> = this.attachedContainer, outputToLevel: Boolean = this.outputToLevel) =
            ResourceNode(xTile, yTile, dir, resourceCategory, allowIn, allowOut, attachedContainer).apply { this.outputToLevel = outputToLevel }

    override fun toString() = "Resource node at $xTile, $yTile, out: $allowOut, in: $allowIn, dir: $dir"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ResourceNode<*>
        if (xTile != other.xTile) return false
        if (yTile != other.yTile) return false
        if (dir != other.dir) return false
        if (resourceCategory != other.resourceCategory) return false
        if (attachedContainer != other.attachedContainer) return false
        if (inLevel != other.inLevel) return false
        if (allowOut != other.allowOut) return false
        if (allowIn != other.allowIn) return false
        return true
    }

    override fun hashCode(): Int {
        var result = xTile
        result = 31 * result + yTile
        result = 31 * result + dir
        result = 31 * result + resourceCategory.hashCode()
        result = 31 * result + attachedContainer.hashCode()
        result = 31 * result + inLevel.hashCode()
        result = 31 * result + allowOut.hashCode()
        result = 31 * result + allowIn.hashCode()
        return result
    }

    companion object {
        fun <R : ResourceType> createCorresponding(n: ResourceNode<R>, attachedContainer: ResourceContainer<R>) =
                ResourceNode(n.xTile + GeometryHelper.getXSign(n.dir), n.yTile + GeometryHelper.getYSign(n.dir), GeometryHelper.getOppositeAngle(n.dir), n.resourceCategory, n.allowOut, n.allowIn, attachedContainer)
    }

}