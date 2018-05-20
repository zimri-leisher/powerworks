package resource

import level.Hitbox
import level.Level
import misc.GeometryHelper

class ResourceNode<R : ResourceType>(val xTile: Int, val yTile: Int, val dir: Int, var allowIn: Boolean = false, var allowOut: Boolean = false, val resourceCategory: ResourceCategory, var attachedContainer: ResourceContainer<R>? = null) {
    var inLevel = false
    var attachedNode: ResourceNode<R>? = null

    /**
     * @return whether the resource is of the right type. Does not check container or attached node for anything
     */
    fun isRightType(resource: ResourceType) = resource.category == resourceCategory

    /**
     * @return whether the container contains adequate amounts of it. If there is no container, it will return false
     */
    fun canOutputFromContainer(resource: ResourceType, quantity: Int): Boolean {
        if (!isRightType(resource))
            return false
        resource as R
        if (!allowOut) return false
        // space in place where it will output to
        if (attachedNode != null && !attachedNode!!.canInputToContainer(resource, quantity))
            return false
        // enough in place where it's taking from, and the resource is valid
        if (attachedContainer != null && attachedContainer!!.canRemove(resource, quantity))
            return true
        return false
    }

    /**
     * @return whether space is available in the container. If there is no container, it will return false
     */
    fun canInputToContainer(resource: ResourceType, quantity: Int): Boolean {
        if (!isRightType(resource))
            return false
        resource as R
        if (!allowIn) return false
        if (attachedContainer != null && attachedContainer!!.canAdd(resource, quantity))
            return true
        return false
    }

    /**
     * @param checkIfAble whether or not to check if space is available and the resource is valid. This function is unsafe
     * when this parameter is false, meaning there are no guarantees as to how it will work given unexpected parameters. Use it
     * only when certain and only for performance
     * @return true if the resources were moved
     */
    fun input(resource: ResourceType, quantity: Int, checkIfAble: Boolean = true): Boolean {
        if (checkIfAble)
            if (!canInputToContainer(resource, quantity))
                return false
        return attachedContainer!!.add(resource, quantity, this)
    }

    /**
     * @param checkIfAble whether or not to check if the container has enough and the resource is valid. This function is unsafe
     * when this parameter is false, meaning there are no guarantees as to how it will work given unexpected parameters. Use it
     * only when certain and only for performance
     * @return true if the resources were moved
     */
    fun output(resource: ResourceType, quantity: Int, checkIfAble: Boolean = true): Boolean {
        if (checkIfAble)
            if (!canOutputFromContainer(resource, quantity))
                return false
        attachedContainer?.remove(resource, quantity, this)
        if (attachedNode != null) {
            val r = attachedNode!!.input(resource, quantity)
            return r
        } else {
            val xSign = GeometryHelper.getXSign(dir)
            val ySign = GeometryHelper.getYSign(dir)
            return Level.add(((xTile shl 4) + 7) + (8 + Hitbox.DROPPED_ITEM.width) * xSign, ((yTile shl 4) + 7) + (8 + Hitbox.DROPPED_ITEM.height) * ySign, resource, quantity) == quantity
        }
    }

    fun copy(xTile: Int = this.xTile, yTile: Int = this.yTile, dir: Int = this.dir, allowIn: Boolean = this.allowIn, allowOut: Boolean = this.allowOut, attachedContainer: ResourceContainer<*>? = this.attachedContainer) =
            ResourceNode(xTile, yTile, dir, allowIn, allowOut, resourceCategory, attachedContainer)

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
        result = 31 * result + (attachedContainer?.hashCode() ?: 0)
        result = 31 * result + inLevel.hashCode()
        result = 31 * result + allowOut.hashCode()
        result = 31 * result + allowIn.hashCode()
        return result
    }

    companion object {
        fun <R : ResourceType> createCorresponding(n: ResourceNode<R>, attachedContainer: ResourceContainer<R>? = null) =
                ResourceNode(n.xTile + GeometryHelper.getXSign(n.dir), n.yTile + GeometryHelper.getYSign(n.dir), GeometryHelper.getOppositeAngle(n.dir), n.allowOut, n.allowIn, n.resourceCategory, attachedContainer)
    }
}

