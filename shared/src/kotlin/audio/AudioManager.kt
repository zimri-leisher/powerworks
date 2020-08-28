package audio

import data.ConcurrentlyModifiableMutableList
import level.LevelObject
import level.MovementListener
import level.moving.MovingObject
import java.util.*
import serialization.Id

private fun Sound.play(vol: Double = 1.0, pan: Double = 0.0, speed: Double = 1.0, loops: Int = 0): Int {
    val s = a.play(vol, pan, speed, loops)
    if (s != -1)
        a.setRecycleWhenDone(s, true)
    return s
}

object AudioManager : MovementListener {

    override fun onMove(m: MovingObject, pXPixel: Int, pYPixel: Int) {
        levelSounds.forEach {
            val vol = getVolume(it.xPixel, it.yPixel)
            if (vol != 0.0) {
                if (!it.playing)
                    it.playing = true
                it.sound.setVolume(getVolume(it.xPixel, it.yPixel), it.instance)
                it.sound.setPan(getPan(it.xPixel), it.instance)
            } else {
                if (it.playing)
                    it.playing = false
            }
        }
    }

    var VOLUME_MULTIPLIER = 1.0

    /**
     * The distance after which sound cannot be heard
     */
    val MAX_HEARING_DISTANCE_PIXELS = 100
    var soundEnabled = true
    /**
     * Whether or not to play level sounds
     */
    var LEVEL_SOUNDS_PAUSED = false
    var ears: LevelObject? = null
        set(value) {
            if (value is MovingObject) {
                value.moveListeners.add(this)
            }
            if (field != null && field is MovingObject) {
                val g = field as MovingObject
                g.moveListeners.remove(this)
            }
            field = value
        }
    var levelSounds = ConcurrentlyModifiableMutableList<SoundSource>()
    var forceUpdate = ConcurrentlyModifiableMutableList<SoundSource>()
    var otherSounds: MutableMap<Sound, Int> = EnumMap(Sound::class.java)

    /**
     * Prepares all sounds for playing, must do before using them
     */
    fun load() {
        Sound.load()
    }

    /**
     * Frees all sound resources, should happen before quit
     */
    fun close() {
        Sound.close()
    }

    fun closeSoundSources() {
        levelSounds.forEach { it.close() }
    }

    class SoundSource(xPixel: Int, yPixel: Int,
                      @Id(1)
                      var instance: Int,
                      @Id(2)
                      var sound: Sound,
                      @Id(3)
                      var loop: Boolean) {

        private constructor() : this(0, 0, 0, Sound.MOTHERLODE_SPARK, false)

        @Id(4)
        var playing = true
            set(value) {
                if (!value && field) {
                    sound.stop(instance)
                } else if (value && !field) {
                    sound.start(instance)
                }
                field = value
            }
        @Id(5)
        var xPixel = xPixel
            set(value) {
                field = value
                forceUpdate()
            }
        @Id(6)
        var yPixel = yPixel
            set(value) {
                field = value
                forceUpdate()
            }

        val isLegitimate: Boolean
            get() = instance != -1

        fun close() {
            sound.a.stop(instance)
            sound.a.releaseInstance(instance)
            levelSounds.remove(this)
            forceUpdate.remove(this)
        }

        private fun forceUpdate() {
            forceUpdate.add(this)
        }
    }

    /**
     * Plays a sound with full volume, no pan and no looping
     */
    fun play(s: Sound) {
        if (!soundEnabled)
            return
        val i = s.play()
        if (i != -1)
            otherSounds.put(s, i)
    }

    /**
     * Starts a sound at a specific point in the level
     * @param loop continuously play this
     * @return a SoundSource with relevant methods and information
     */
    fun play(s: Sound, xPixel: Int, yPixel: Int, loop: Boolean): SoundSource? {
        if (!soundEnabled)
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
        if (!soundEnabled)
            return
        if (ears == null)
            return
        forceUpdate.forEach {
            it.sound.setVolume(getVolume(it.xPixel, it.yPixel), it.instance)
            it.sound.setPan(getPan(it.xPixel), it.instance)
            forceUpdate.remove(it)
        }
    }
}