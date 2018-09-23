package screen.animations

import main.Game
import screen.elements.GUIWindow

class SlideOpenAnimation(subject: GUIWindow, val closeOnFinish: GUIWindow) : GUIAnimation<GUIWindow>(subject) {

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
        closeOnFinish.open = false
    }
}