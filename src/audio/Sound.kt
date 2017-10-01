package audio

import com.adonax.audiocue.AudioCue

enum class Sound(path: String, maxConcurrent: Int) {
    GRASS_FOOTSTEP("/sounds/footstep/grass.wav", 6), CONVEYOR_BELT("/sounds/block/machine/conveyor_belt.wav", 4);

    internal var a: AudioCue = AudioCue.makeStereoCue(Sound::class.java.getResource(path), maxConcurrent)

    internal fun play(): Int {
        val s = a.play()
        if (s != -1)
            a.setRecycleWhenDone(s, true)
        return a.play()
    }

    internal fun play(vol: Double): Int {
        val s = a.play(vol)
        if (s != -1)
            a.setRecycleWhenDone(s, true)
        return s
    }

    internal fun play(vol: Double, pan: Double, speed: Double, loops: Int): Int {
        val s = a.play(vol, pan, speed, loops)
        if (s != -1)
            a.setRecycleWhenDone(s, true)
        return s
    }

    internal fun play(vol: Double, loop: Int): Int {
        val s = a.play(vol, 0.0, 1.0, loop)
        if (s != -1)
            a.setRecycleWhenDone(s, true)
        return s
    }

    internal fun setVolume(vol: Double, instance: Int) {
        a.setVolume(instance, vol)
    }

    internal fun setPan(pan: Double, instance: Int) {
        a.setPan(instance, pan)
    }

    internal fun setLoop(loops: Int, instance: Int) {
        a.setLooping(instance, loops)
    }

    internal fun close(instance: Int) {
        a.stop(instance)
        a.releaseInstance(instance)
    }

    internal fun stop(instance: Int) {
        a.stop(instance)
    }

    internal fun start(instance: Int) {
        a.start(instance)
    }

    companion object {
        internal fun load() {
            values().forEach { it.a.open() }
        }

        internal fun close() {
            values().forEach { it.a.close() }
        }
    }
}