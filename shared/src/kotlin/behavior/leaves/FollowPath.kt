package behavior.leaves

import behavior.*
import level.update.EntitySetPath
import level.entity.Entity
import misc.Coord
import network.MovingObjectReference

class FollowPath(parent: BehaviorTree, val pathVar: Variable) : Leaf(parent) {

    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        val path = getData<EntityPath>(pathVar)
        if (path == null) {
            state = NodeState.FAILURE
            return
        } else if (path.steps.isEmpty()) {
            state = NodeState.SUCCESS
            return
        }
        setData(DefaultVariable.PATH_BEING_FOLLOWED, path)
        entity.level.modify(EntitySetPath(entity, Coord(entity.x, entity.y), path, entity.level))
    }

    override fun updateState(entity: Entity): NodeState {
        var path = getData<EntityPath>(DefaultVariable.PATH_BEING_FOLLOWED)
        if (path == null) {
            val recheckPath = getData<EntityPath>(pathVar)
            if (recheckPath == null) {
                return NodeState.FAILURE
            } else {
                path = recheckPath
                setData(DefaultVariable.PATH_BEING_FOLLOWED, path)
                entity.level.modify(EntitySetPath(entity, Coord(entity.x, entity.y), path, entity.level))
                if (recheckPath.steps.isEmpty()) {
                    return NodeState.SUCCESS
                }
            }
        }
        if (!entity.behavior.isFollowingPath()) {
            return NodeState.SUCCESS
        } else {
            return NodeState.RUNNING
        }
    }

    override fun execute(entity: Entity) {
        // entity already handles following of path
    }

    override fun toString() = "FollowPath: (pathVar: $pathVar)"
}