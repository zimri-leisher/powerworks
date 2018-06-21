package screen.animations

import main.Game
import screen.elements.GUIElement

class VelocityAnimation(subject: GUIElement,
                        var xVel: Int, var yVel: Int,
                        var dragMultiplier: Float = 1f,
                        val moveBackOnStop: Boolean = false,
                        val keepInsideScreen: Boolean = true) : Animation<GUIElement>(subject) {

    private val original = subject.alignments.copy()

    private var totalXOffset = 0
    private var totalYOffset = 0

    override fun onStart() {
        with(subject.alignments) {
            x = { original.x() + totalXOffset }
            y = { original.y() + totalYOffset }
        }
    }

    override fun onStop() {
        if (moveBackOnStop) {
            subject.alignments = original
            subject.alignments.updatePosition()
        }
    }

    override fun update() {
        if (keepInsideScreen) {
            if (subject.xPixel + xVel + subject.widthPixels > Game.WIDTH) {
                xVel = Game.WIDTH - subject.widthPixels - subject.xPixel
            } else if (subject.xPixel + xVel < 0) {
                xVel = -subject.xPixel
            }
            if (subject.yPixel + yVel + subject.heightPixels > Game.HEIGHT) {
                yVel = Game.HEIGHT - subject.heightPixels - subject.yPixel
            } else if (subject.yPixel + yVel < 0) {
                yVel = -subject.yPixel
            }
        }
        totalXOffset += xVel
        totalYOffset += yVel
        xVel = (xVel * dragMultiplier).toInt()
        yVel = (yVel * dragMultiplier).toInt()
        subject.alignments.updatePosition()
        if (xVel == 0 && yVel == 0)
            playing = false
    }
}