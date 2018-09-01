package audio

import data.ResourceManager
import src.main.java.com.adonax.audiocue.AudioCue

enum class Sound(path: String, maxConcurrent: Int) {
    GRASS_FOOTSTEP("/sounds/footstep/grass.wav", 6),
    MOTHERLODE_SPARK("/sounds/misc/motherlode_spark.wav", 2);

    /**
     * API related instance
     */
    var a: AudioCue = AudioCue.makeStereoCue(ResourceManager.getRawResource(path), maxConcurrent)

    fun setVolume(vol: Double, instance: Int) {
        a.setVolume(instance, vol)
    }

    fun setPan(pan: Double, instance: Int) {
        a.setPan(instance, pan)
    }

    fun setLoop(loops: Int, instance: Int) {
        a.setLooping(instance, loops)
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