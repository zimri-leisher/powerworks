package screen.animations

import screen.ScreenManager
import screen.elements.RootGUIElement

abstract class Animation<E : RootGUIElement>(val subject: E) {
    var playing = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    ScreenManager.playingAnimations.add(this)
                    onStart()
                } else {
                    ScreenManager.playingAnimations.remove(this)
                    onStop()
                }
            }
        }

    abstract fun onStart()
    abstract fun onStop()
    open fun update() {}
}