package screen.animations

import screen.elements.GUIElement

class MoveAnimation(subject: GUIElement, val xPixelGoal: Int, val yPixelGoal: Int, val speed: Int) : Animation<GUIElement>(subject) {

    lateinit var subjectXAlignment: () -> Int

    var currentXOffset = 0
    var currentYOffset = 0

    override fun onStart() {
    }

    override fun onStop() {
    }
}
