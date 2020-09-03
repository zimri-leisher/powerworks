package level

import level.moving.MovingObject

interface MovementListener {
    fun onMove(m: MovingObject, prevX: Int, prevY: Int)
}