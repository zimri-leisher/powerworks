package audio

import audiocue.AudioCue
import main.ResourceManager

enum class Sound(path: String, maxConcurrent: Int) {
    GRASS_FOOTSTEP("/sounds/footstep/grass.wav", 6),
    MOTHERLODE_SPARK("/sounds/misc/motherlode_spark.wav", 2);

    /**
     * API related instance
     */
    var a: AudioCue = AudioCue.makeStereoCue(ResourceManager.getResource(path), maxConcurrent)

    /**
     * Note - use AudioManager.play(Sound) instead
     *
     * Plays the sound with the specified volume, at the specified speed, loops + 1 number of times
     * @return the sound id. Keep track of this if you want to modify it later
     */
    fun play(vol: Double = 1.0, pan: Double = 0.0, speed: Double = 1.0, loops: Int = 0): Int {
        val s = a.play(vol, pan, speed, loops)
        if (s != -1)
            a.setRecycleWhenDone(s, true)
        return s
    }

    fun setVolume(vol: Double, instance: Int) {
        a.setVolume(instance, vol)
    }

    fun setPan(pan: Double, instance: Int) {
        a.setPan(instance, pan)
    }

    fun setLoop(loops: Int, instance: Int) {
        a.setLooping(instance, loops)
    }

    fun close(instance: Int) {
        a.stop(instance)
        a.releaseInstance(instance)
    }

    fun stop(instance: Int) {
        a.stop(instance)
    }

    fun start(instance: Int) {
        a.start(instance)
    }

    companion object {
        fun load() {
            values().forEach { it.a.open() }
        }

        fun close() {
            values().forEach { it.a.close() }
        }
    }
}