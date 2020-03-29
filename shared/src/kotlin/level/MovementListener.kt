package level

import level.moving.MovingObject

interface MovementListener {
    fun onMove(m: MovingObject, pXPixel: Int, pYPixel: Int)
}