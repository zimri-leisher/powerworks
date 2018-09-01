package screen.animations

import screen.ScreenManager
import screen.elements.RootGUIElement

/**
 * An animation is something that acts over time on a RootGUIElement (either a GUIElement or a GUIWindow) or a subclass
 * For example, it could make the subject appear to have a velocity, it could make the subject appear to bounce, etc.
 *
 * To use, instantiate an GUIAnimation subclass and then set it's playing variable to true.
 *
 * @param subject the element on which to act
 */
abstract class GUIAnimation<E : RootGUIElement>(val subject: E) {
    /**
     * Set this to true to begin the animation and false to stop it. Note, this means setting this to false won't just pause
     * it
     */
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
    /**
     * Called every tick while playing is true
     */
    open fun update() {}
}