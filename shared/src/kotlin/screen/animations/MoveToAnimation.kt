package screen.animations

import screen.elements.RootGUIElement

class MoveToAnimation(subject: RootGUIElement, val xDest: Int, val yDest: Int, val ticksToDest: Int = 7, onStart: () -> Unit = {}, onStop: () -> Unit = {}) :
        GUIAnimation<RootGUIElement>(subject, onStart, onStop) {

    var xVel = 0f
    var yVel = 0f
    var xOffset = 0f
    var yOffset = 0f
    var ticks = 0

    override fun onStart() {
        val xDist = xDest - subject.alignments.x()
        val yDist = yDest - subject.alignments.y()
        xVel = xDist.toFloat() / ticksToDest
        yVel = yDist.toFloat() / ticksToDest
        with(subject.alignments) {
            val currX = x()
            val currY = y()
            x = { (currX + xOffset).toInt() }
            y = { (currY + yOffset).toInt() }
        }
    }

    override fun update() {
        if (ticks == ticksToDest) {
            playing = false
            return
        }
        xOffset += xVel
        yOffset += yVel
        subject.alignments.update()
        ticks++
    }

    override fun onStop() {
        with(subject.alignments) {
            val xDest = xDest
            val yDest = yDest
            x = { xDest }
            y = { yDest }
        }
    }
}