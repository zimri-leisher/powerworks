package level.update

import level.Level
import level.LevelManager
import level.PhysicalLevelObject
import player.Player
import resource.PipeNetwork
import resource.PotentialResourceNetworkVertex
import resource.ResourceNetwork
import resource.ResourceNetworkVertex
import serialization.AsReference
import serialization.Id

class ResourceNetworkRemoveVertices<V : ResourceNetworkVertex<V>>(
    @Id(5)
    @AsReference
    val network: ResourceNetwork<V>,
    @Id(4)
    @AsReference(true)
    val vertices: List<PotentialResourceNetworkVertex<V>>,
    level: Level
) : LevelUpdate(LevelUpdateType.RESOURCE_NETWORK_REMOVE_VERTICES, level){

    private constructor() : this(PipeNetwork() as ResourceNetwork<V>, listOf(), LevelManager.EMPTY_LEVEL)

    override val playersToSendTo: Set<Player>? = null

    override fun getChildren(): List<LevelUpdate> {
        return listOf()
    }

    override fun canAct(): Boolean {
        return vertices.all { network in it.networks }
    }

    override fun act() {
        for(vertex in vertices) {
            network.remove(vertex as PhysicalLevelObject)
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