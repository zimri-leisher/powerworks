package misc

import java.awt.Point
import java.awt.Polygon

object Geometry {
    fun intersects(xPixel: Int, yPixel: Int, width: Int, height: Int, xPixel2: Int, yPixel2: Int, width2: Int, height2: Int): Boolean {
        if (xPixel + width <= xPixel2 || yPixel + height <= yPixel2 || xPixel >= xPixel2 + width2 || yPixel >= yPixel2 + height2)
            return false
        return true
    }

    fun contains(xPixel: Int, yPixel: Int, width: Int, height: Int, xPixelIn: Int, yPixelIn: Int, widthIn: Int, heightIn: Int): Boolean {
        if (xPixelIn >= xPixel && yPixelIn >= yPixel && xPixelIn + widthIn <= xPixel + width && yPixelIn + heightIn <= yPixel + height)
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

    fun rotate(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int, dir: Int): TileCoord {
        return when (dir % 4) {
            1 -> TileCoord(widthTiles - yTile - 1, heightTiles - xTile)
            2 -> TileCoord(widthTiles - xTile - 1, yTile + 1)
            3 -> TileCoord(yTile, xTile + 1)
            else -> TileCoord(xTile, yTile)
        }
    }

    fun distance(x: Int, y: Int, x2: Int, y2: Int) = Numbers.sqrt(distanceSq(x, y, x2, y2))

    fun distanceSq(x: Int, y: Int, x2: Int, y2: Int) = Numbers.square(x - x2) + Numbers.square(y - y2)

    /*

    fun doesRectangleIntersectRectangle(x1: Int, y1: Int, x1b: Int, y1b: Int, x2: Int, y2: Int, x2b: Int, y2b: Int): Boolean { // credit to herohuyongtao on SO
        for (x in 0..1) {
            val polygon: Polygon = if (x == 0) a else b
            for (i1 in polygon.getPoints().indices) {
                val i2: Int = (i1 + 1) % polygon.getPoints().size
                val p1: Point = polygon.getPoints().get(i1)
                val p2: Point = polygon.getPoints().get(i2)
                val normal = Point(p2.y - p1.y, p1.x - p2.x)
                var minA = Double.POSITIVE_INFINITY
                var maxA = Double.NEGATIVE_INFINITY
                for (p in a.getPoints()) {
                    val projected: Double = (normal.x * p.x + normal.y * p.y).toDouble()
                    if (projected < minA) minA = projected
                    if (projected > maxA) maxA = projected
                }
                var minB = Double.POSITIVE_INFINITY
                var maxB = Double.NEGATIVE_INFINITY
                for (p in b.getPoints()) {
                    val projected: Double = (normal.x * p.x + normal.y * p.y).toDouble()
                    if (projected < minB) minB = projected
                    if (projected > maxB) maxB = projected
                }
                if (maxA < minB || maxB < minA) return false
            }
        }
        return true
    }
     */
}