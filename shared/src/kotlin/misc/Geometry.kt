package misc

import kotlin.math.*

object Geometry {
    fun intersects(x: Int, y: Int, width: Int, height: Int, x2: Int, y2: Int, width2: Int, height2: Int): Boolean {
        if (x + width <= x2 || y + height <= y2 || x >= x2 + width2 || y >= y2 + height2)
            return false
        return true
    }

    fun contains(x: Int, y: Int, width: Int, height: Int, xIn: Int, yIn: Int, widthIn: Int, heightIn: Int): Boolean {
        if (xIn >= x && yIn >= y && xIn + widthIn <= x + width && yIn + heightIn <= y + height)
            return true
        return false
    }

    fun isAdjacentOrIntersecting(x1: Int, y1: Int, x2: Int, y2: Int): Boolean {
        return Math.abs(x1 - x2) <= 1 && Math.abs(y1 - y2) <= 1
    }

    fun isOppositeAngle(a1: Int, a2: Int): Boolean {
        return Math.max(a1, a2) - 2 == Math.min(a1, a2)
    }

    fun getOppositeAngle(a1: Int): Int {
        return (a1 + 2) % 4
    }

    fun getXSign(dir: Int): Int {
        return if (dir == 1) 1 else if (dir == 3) -1 else 0
    }

    fun addAngles(dir1: Int, dir2: Int): Int = (dir1 + dir2) % 4

    fun getYSign(dir: Int): Int {
        return if (dir == 0) 1 else if (dir == 2) -1 else 0
    }

    fun getDir(x: Int, y: Int): Int {
        if ((x == 0 && y == 0) || (x != 0 && y != 0))
            return -1
        if (x <= -1)
            return 3
        if (x >= 1)
            return 1
        if (y <= -1)
            return 2
        return 0
    }

    fun getDegrees(angle: Int): Float {
        return when (angle % 4) {
            0 -> 90f
            1 -> 0f
            2 -> -90f
            3 -> 180f
            else -> 0f
        }
    }

    fun distance(x1: Int, y1: Int, x1b: Int, y1b: Int, x2: Int, y2: Int, x2b: Int, y2b: Int): Double { // credit to Maxim on SO im a lazy fuck
        val left = x2b < x1
        val right = x1b < x2
        val bottom = y2b < y1
        val top = y1b < y2
        if (top && left)
            return distance(x1, y1b, x2b, y2)
        else if (left && bottom)
            return distance(x1, y1, x2b, y2b)
        else if (bottom && right)
            return distance(x1b, y1, x2, y2b)
        else if (right && top)
            return distance(x1b, y1b, x2, y2)
        else if (left)
            return (x1 - x2b).toDouble()
        else if (right)
            return (x2 - x1b).toDouble()
        else if (bottom)
            return (y1 - y2b).toDouble()
        else if (top)
            return (y2 - y1b).toDouble()
        else
            return 0.0
    }

    fun rotate(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int, angle: Int): TileCoord {
        if (angle == 0) {
            return TileCoord(xTile, yTile)
        }
        val centeredX = xTile + 0.5
        val centeredY = yTile + 0.5

        val originX = widthTiles.toDouble() / 2
        val originY = heightTiles.toDouble() / 2

        val rebasedX = centeredX - originX
        val rebasedY = centeredY - originY

        val radians = angle * (-PI / 2)

        val rotatedX = rebasedX * cos(radians) - rebasedY * sin(radians)
        val rotatedY = rebasedX * sin(radians) + rebasedY * cos(radians)

        val translatedX = rotatedX + originX - 0.5
        val translatedY = rotatedY + originY - 0.5

        return TileCoord(translatedX.roundToInt(), translatedY.roundToInt())
    }

    fun manhattanDist(x: Int, y: Int, x2: Int, y2: Int) = (x - x2).absoluteValue + (y - y2).absoluteValue

    fun distance(x: Int, y: Int, x2: Int, y2: Int) = Numbers.sqrt(distanceSq(x, y, x2, y2))

    fun distanceSq(x: Int, y: Int, x2: Int, y2: Int) = Numbers.square(x - x2) + Numbers.square(y - y2)

    fun doesLineCollideWithRect(x1: Float, y1: Float, x2: Float, y2: Float, rx: Float, ry: Float, rw: Float, rh: Float): Boolean { // credit jefferythompson.org
                return doesLineCollideWithLine(x1, y1, x2, y2, rx, ry, rx, ry + rh) ||
                        doesLineCollideWithLine(x1, y1, x2, y2, rx + rw, ry, rx + rw, ry + rh) ||
                        doesLineCollideWithLine(x1, y1, x2, y2, rx, ry, rx + rw, ry) ||
                        doesLineCollideWithLine(x1, y1, x2, y2, rx, ry + rh, rx + rw, ry + rh)
    }


    fun doesLineCollideWithLine(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float): Boolean {// credit jefferythompson.org

        // calculate the direction of the lines
        val uA = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / ((y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1))
        val uB = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / ((y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1))

        // if uA and uB are between 0-1, lines are colliding
        return uA in 0.0..1.0 && uB in 0.0..1.0
    }
}