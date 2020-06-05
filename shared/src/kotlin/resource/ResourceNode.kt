package resource

import item.Inventory
import level.Hitbox
import level.Level
import level.LevelManager
import level.add
import misc.Geometry
import routing.ResourceRoutingNetwork
import serialization.Id
import java.util.*

/**
 * A node that allows for movement of resources between places on the level. This is not a subclass of LevelObject.
 *
 * An example of a place where they appear is the MinerBlock, which uses one to produce the ore it mines
 * from the ground and either put it into the connected inventory or place it on the ground.
 */
class ResourceNode constructor(
        @Id(1)
        val xTile: Int,
        @Id(2)
        val yTile: Int,
        @Id(3)
        val dir: Int,
        @Id(4)
        val resourceCategory: ResourceCategory,
        @Id(5)
        var attachedContainer: ResourceContainer,
        /**
         * The [Level] this node is in, or will be in if it has not been [added][add] already
         */
        @Id(6)
        var level: Level) {

    private constructor() : this(0, 0, 0, ResourceCategory.ITEM, Inventory(0, 0), LevelManager.EMPTY_LEVEL)

    /**
     * If this node is in the level, meaning it is able to interact with other nodes in the level
     */
    @Id(7)
    var inLevel = false


    /**
     * The adjacent node facing towards this node, with the same [resourceCategory]
     * This is where resources will get sent to when outputting
     */
    @Id(8)
    var attachedNode: ResourceNode? = null

    /**
     * Whether or not this node should be allowed to output resources directly to the level, using the Level.add(ResourceType, Int) method.
     * If false, calls to can/couldOutput will return false if there is no attached node, regardless of whether there is space in the level
     */
    @Id(9)
    var outputToLevel = true

    /**
     * The resource routing network which this is part of
     */
    @Id(10)
    var network = ResourceRoutingNetwork(resourceCategory, level)

    @Id(11)
    var isInternalNetworkNode = false

    /**
     * The input and output behavior of this node. This is where routing language gets put into and evaluated
     */
    @Id(12)
    var behavior = ResourceNodeBehavior(this)
        set(value) {
            if(field != value) {
                field = value
                field.node = this
            }
        }

    @Id(13)
    val id = UUID.randomUUID()

    /**
     * @param mustContainEnough whether or not to check if the attached container has enough resources. Set to false if
     * you know it does or don't care if it doesn't
     * @return if the resource is the right type and this node allows output and the attached container is able to remove the resources.
     * Additionally, if there is an attached node, it must be able to input the resources
     */
    fun canOutput(resources: ResourceList, mustContainEnough: Boolean = true): Boolean {
        if (resources.keys.any { !isRightType(it) || !behavior.allowOut.check(it) }) {
            return false
        }
        if (mustContainEnough) {
            if (!attachedContainer.canRemove(resources)) {
                return false
            }
        }
        if (attachedNode != null) {
            if (!attachedNode!!.canInput(resources)) {
                return false
            }
        } else if (attachedNode == null && !outputToLevel) {
            return false
        }
        return true
    }

    fun canOutput(type: ResourceType, quantity: Int, mustContainEnough: Boolean = true) = canOutput(ResourceList(type to quantity), mustContainEnough)

    /**
     * @param mustHaveSpace whether or not to check if the attached container has enough space. Set to false if you know
     * it does or don't care if it doesn't. This means it will only check the type and behavior of the node (the behavior
     * may still check for space)
     * @param accountForExpected whether or not to include the expected resources when checking for space. Does nothing if [mustHaveSpace] is false
     * @return if the resource is the right type and this node allows input and the attached container can add the resources
     */
    fun canInput(resources: ResourceList, mustHaveSpace: Boolean = true, accountForExpected: Boolean = false): Boolean {
        if (resources.keys.any { !isRightType(it) || !behavior.allowIn.check(it) })
            return false
        if (mustHaveSpace) {
            if (accountForExpected) {
                if (!attachedContainer.canAdd(resources + attachedContainer.expected))
                    return false
            } else {
                if (!attachedContainer.canAdd(resources))
                    return false
            }
        }
        return true
    }

    fun canInput(type: ResourceType, quantity: Int, mustHaveSpace: Boolean = true, accountForExpected: Boolean = false) = canInput(ResourceList(type to quantity), mustHaveSpace, accountForExpected)

    /**
     * Outputs the resources either directly to the level or into a connected node
     * @param checkIfAble whether or not to check if the node is able to output. This should be false only
     * to avoid redundant checks to the attached container or node (i.e., for performance). Setting it to false without being
     * sure about the ability of the attached node to input and the attached container to remove has an undefined behavior
     * and will probably result in duplications or crashes.
     * @param mustContainEnough this specifies whether or not the check for ability to output should check if the attached container
     * contains enough resources. Does nothing if checkIfAble is false
     */
    fun output(resources: ResourceList, checkIfAble: Boolean = true, mustContainEnough: Boolean = true): Boolean {
        if (checkIfAble) {
            if (!canOutput(resources, mustContainEnough))
                return false
        }
        if (attachedNode != null) {
            // we already checked if we were able to, everything here is under the assumption it is successful
            attachedNode!!.input(resources, false)
        } else if (outputToLevel && inLevel) {
            for ((type, quantity) in resources) {
                // TODO make this better some time, have it actually spawn in the center
                val xSign = Geometry.getXSign(dir)
                val ySign = Geometry.getYSign(dir)
                level.add(((xTile shl 4) + 7) + (8 + Hitbox.DROPPED_ITEM.width) * xSign, ((yTile shl 4) + 7) + (8 + Hitbox.DROPPED_ITEM.height) * ySign, type, quantity)
            }
        }
        attachedContainer.remove(resources, this, false)
        return true
    }

    fun output(type: ResourceType, quantity: Int, checkIfAble: Boolean = true, mustContainEnough: Boolean = true) = output(ResourceList(type to quantity), checkIfAble, mustContainEnough)

    /**
     * Inputs the resources into the attached container
     * @param checkIfAble whether or not to check if the node is able to input. This should be false only
     * to avoid redundant checks to the attached container (i.e., for performance). Setting it to false without being
     * sure about the ability of the attached container to add has an undefined behavior and will probably result in duplications or crashes.
     * @param mustHaveSpace this specifies whether or not the check for ability to input should check if the attached container
     * has enough space. Does nothing if checkIfAble is false
     */
    fun input(resources: ResourceList, checkIfAble: Boolean = true, mustHaveSpace: Boolean = true): Boolean {
        if (checkIfAble) {
            if (!canInput(resources, mustHaveSpace))
                return false
        }
        attachedContainer.add(resources, this, false)
        return true
    }

    fun input(type: ResourceType, quantity: Int, checkIfAble: Boolean = true, mustHaveSpace: Boolean = true) = input(ResourceList(type to quantity), checkIfAble, mustHaveSpace)

    fun update() {
        for ((type, quantity) in attachedContainer.toResourceList()) {
            if (behavior.forceIn.check(type)) {
                network.forceSendTo(this, type, quantity)
            }
            if (behavior.forceOut.check(type)) {
                network.forceTakeFrom(this, type, quantity)
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

    override fun toString() = "Resource node at $xTile, $yTile, dir: $dir"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ResourceNode
        if (xTile != other.xTile) return false
        if (yTile != other.yTile) return false
        if (dir != other.dir) return false
        if (resourceCategory != other.resourceCategory) return false
        if (behavior != other.behavior) return false
        return true
    }

    override fun hashCode(): Int {
        var result = xTile
        result = 31 * result + yTile
        result = 31 * result + dir
        result = 31 * result + resourceCategory.hashCode()
        return result
    }

    companion object {
        fun createCorresponding(n: ResourceNode, attachedContainer: ResourceContainer, behavior: ResourceNodeBehavior) =
                ResourceNode(n.xTile + Geometry.getXSign(n.dir), n.yTile + Geometry.getYSign(n.dir), Geometry.getOppositeAngle(n.dir), n.resourceCategory, attachedContainer, n.level).apply {
                    this.behavior = behavior
                }
    }

}