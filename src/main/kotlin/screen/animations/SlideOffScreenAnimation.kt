package screen.animations

import main.Game
import screen.elements.GUIWindow

class SlideOffScreenAnimation(subject: GUIWindow, onStart: () -> Unit = {}, onStop: () -> Unit = {}) : GUIAnimation<GUIWindow>(subject, onStart, onStop) {
    private val move = MoveToAnimation(subject, 0, Game.HEIGHT)

    override fun onStart() {
        with(subject.alignments) {
            x = { 0 }
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
