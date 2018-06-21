package screen.animations

import screen.elements.GUIElement
import screen.elements.RootGUIElement

class BounceAnimation(subject: GUIElement) : Animation<GUIElement>(subject) {

    /**
     * If this is true, the animation is scaling down
     */
    var inwards = false

    private lateinit var original: RootGUIElement.ElementAlignments

    var currentScale = 1f

    override fun onStart() {
        original = subject.alignments.copy()
        with(subject.alignments) {
            width = { (original.width() * currentScale).toInt() }
            height = { (original.height() * currentScale).toInt() }
            x = { original.x() - (width() - original.width()) / 2}
            y = { original.y() - (height() - original.height()) / 2}
        }
    }

    override fun onStop() {
        subject.alignments = original
        subject.alignments.update()
    }

    override fun update() {
        if (!inwards) {
            if (currentScale * 1.01f > 2f) {
                inwards = true
                return
            }
            currentScale *= 1.01f
        } else {
            if (currentScale * 0.99f < 1f) {
                playing = false
                return
            }
            currentScale *= 0.99f
        }
        subject.update()
    }
}
