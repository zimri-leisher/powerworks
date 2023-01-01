package level.update

import level.Level
import level.LevelManager
import level.PhysicalLevelObject
import player.Player
import resource.*
import serialization.AsReference
import serialization.Id
import serialization.TryToResolveReferences

@TryToResolveReferences
class ResourceNetworkAddVertices<V : ResourceNetworkVertex<V>>(
    @Id(5)
    @AsReference
    val network: ResourceNetwork<V>,
    @Id(4)
    @AsReference(true)
    val vertices: List<PotentialResourceNetworkVertex<V>>,
    level: Level
) : LevelUpdate(LevelUpdateType.RESOURCE_NETWORK_ADD_VERTICES, level){

    private constructor() : this(PipeNetwork() as ResourceNetwork<V>, listOf<PotentialResourceNetworkVertex<V>>(), LevelManager.EMPTY_LEVEL)

    override val playersToSendTo: Set<Player>? = null

    override fun getChildren(): List<LevelUpdate> {
        if(network.level == LevelManager.EMPTY_LEVEL) {
            return listOf(LevelObjectAdd(network, level))
        }
        return listOf()
    }

    override fun canAct(): Boolean {
        val levels = vertices.map { it.level }.distinct()
        if(levels.size != 1 || levels.first() != level) {
            return false
        }
        // if the network isn't in the empty level (if it is, we will add it) and the network isn't in
        if(network.level != LevelManager.EMPTY_LEVEL && network.level != levels.first()) {
            return false
        }
        return true
    }

    override fun act() {
        for(vertex in vertices) {
            network.add(vertex as PhysicalLevelObject)
        }
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        return other is ResourceNetworkAddVertices<*> && other.network == network && other.vertices == vertices && other.level == level
    }

}