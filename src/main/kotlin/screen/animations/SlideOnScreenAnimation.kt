package screen.animations

import main.Game
import screen.elements.GUIWindow

class SlideOnScreenAnimation(subject: GUIWindow, onStart: () -> Unit = {}, onStop: () -> Unit = {}) : GUIAnimation<GUIWindow>(subject, onStart, onStop) {
    private val move = MoveToAnimation(subject, 0, 0)

    override fun onStart() {
        with(subject.alignments) {
            x = { Game.WIDTH }
            y = { 0 }
        }
        move.playing = true
    }

    override fun update() {
        if (!move.playing) {
            playing = false
        }
    }

    override fun onStop() {
    }
}
