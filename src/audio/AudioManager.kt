package audio

import level.LevelObject
import level.MovementListener
import level.moving.MovingObject
import main.Game
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object AudioManager : MovementListener{
    override fun onMove(m: MovingObject, pXPixel: Int, pYPixel: Int) {
        for (s in levelSounds) {
            val vol = getVolume(s.xPixel, s.yPixel)
            if (vol != 0.0) {
                if (!s.playing)
                    s.playing = true
                s.s!!.setVolume(getVolume(s.xPixel, s.yPixel), s.instance)
                s.s!!.setPan(getPan(s.xPixel), s.instance)
            } else {
                if (s.playing)
                    s.playing = false
            }
        }
    }

    var VOLUME_MULTIPLIER = 1.0
    val MAX_HEARING_DISTANCE_PIXELS = 100
    var SOUND_ENABLED = true
    var LEVEL_SOUNDS_PAUSED = false
    var ears: LevelObject? = null
        set(value) {
            if(value is MovingObject) {
                value.moveListeners.add(this)
            }
            if(field != null && field is MovingObject) {
                val g = field as MovingObject
                g.moveListeners.remove(this)
            }
            field = value
        }
    var levelSounds = CopyOnWriteArrayList<SoundSource>()
    var forceUpdate = CopyOnWriteArrayList<SoundSource>()
    var otherSounds: MutableMap<Sound, Int> = HashMap()

    fun load() {
        Sound.load()
    }

    fun close() {
        Sound.close()
    }

    fun closeSoundSources() {
        levelSounds.forEach { it.close() }
    }

    class SoundSource internal constructor(xPixel: Int, yPixel: Int, internal var instance: Int, internal var s: Sound?, internal var loop: Boolean) {
        internal var playing = true
            set(value) {
                if (!value && field) {
                    s!!.stop(instance)
                } else if (value && !field) {
                    s!!.start(instance)
                }
                field = value
            }
        var xPixel = xPixel
            set(value) {
                field = value
                forceUpdate()
            }
        var yPixel = yPixel
            set(value) {
                field = value
                forceUpdate()
            }

        val isLegitimate: Boolean
            get() = instance != -1

        fun close() {
            s!!.close(instance)
            levelSounds.remove(this)
            forceUpdate.remove(this)
            s = null
        }

        private fun forceUpdate() {
            forceUpdate.add(this)
        }
    }

    /**
     * Plays a sound with full volume, no pan and no looping
     */
    fun play(s: Sound) {
        if (!SOUND_ENABLED)
            return
        val i = s.play()
        if (i != -1)
            otherSounds.put(s, i)
    }

    fun play(s: Sound, xPixel: Int, yPixel: Int, loop: Boolean): SoundSource? {
        if (!SOUND_ENABLED)
            return null
        if (ears == null)
            return null
        val src = SoundSource(xPixel, yPixel, s.play(
                getVolume(xPixel, yPixel),
                getPan(xPixel), 1.0, if (loop) -1 else 0), s, loop)
        if (src.isLegitimate)
            levelSounds.add(src)
        return if (src.isLegitimate) src else null
    }

    private fun getVolume(xPixel: Int, yPixel: Int): Double {
        return Math.max(MAX_HEARING_DISTANCE_PIXELS - Math.sqrt(Math.pow((xPixel - ears!!.xPixel).toDouble(), 2.0) + Math.pow((yPixel - ears!!.yPixel).toDouble(), 2.0)), 0.0) / MAX_HEARING_DISTANCE_PIXELS * VOLUME_MULTIPLIER
    }

    private fun getPan(xPixel: Int): Double {
        return Math.max(Math.min((xPixel - ears!!.xPixel).toFloat() / MAX_HEARING_DISTANCE_PIXELS, 1f), -1f).toDouble()
    }

    fun update() {
        if (!SOUND_ENABLED)
            return
        if (ears == null)
            return
        if (Game.PAUSE_LEVEL_IN_ESCAPE_MENU && Game.LEVEL_PAUSED && !LEVEL_SOUNDS_PAUSED) {
            LEVEL_SOUNDS_PAUSED = true
            for (s in levelSounds)
                s.playing = false
            return
        } else if (Game.PAUSE_LEVEL_IN_ESCAPE_MENU && !Game.LEVEL_PAUSED && LEVEL_SOUNDS_PAUSED) {
            LEVEL_SOUNDS_PAUSED = false
            for (s in levelSounds)
                s.playing = true
        }
        for (s in forceUpdate) {
            s.s!!.setVolume(getVolume(s.xPixel, s.yPixel), s.instance)
            s.s!!.setPan(getPan(s.xPixel), s.instance)
            forceUpdate.remove(s)
        }
    }
}