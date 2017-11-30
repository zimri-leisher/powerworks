package misc

object GeometryHelper {
    fun intersects(xPixel: Int, yPixel: Int, width: Int, height: Int, xPixel2: Int, yPixel2: Int, width2: Int, height2: Int): Boolean {
        if (xPixel + width <= xPixel2 || yPixel + height <= yPixel2 || xPixel >= xPixel2 + width2 || yPixel >= yPixel2 + height2)
            return false
        return true
    }

    fun contains(xPixel: Int, yPixel: Int, width: Int, height: Int, xPixelIn: Int, yPixelIn: Int, widthIn: Int, heightIn: Int): Boolean {
        if (xPixelIn >= xPixel && yPixelIn >= yPixel && xPixelIn + widthIn < xPixel + width && yPixelIn + heightIn < yPixel + height)
            return true
        return false
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

    fun getYSign(dir: Int): Int {
        return if (dir == 0) -1 else if (dir == 2) 1 else 0
    }

    fun getDir(x: Int, y: Int): Int {
        if(x == -1)
            return 3
        if(x == 1)
            return 1
        if(y == -1)
            return 0
        return 2
    }
}