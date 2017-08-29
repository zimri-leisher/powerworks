package level

import java.util.Random


class SimplexNoise(largestFeature: Int, internal var persistence: Double, seed: Long) {

    internal var octaves: Array<SimplexNoise_octave>
    internal var frequencys: DoubleArray
    internal var amplitudes: DoubleArray

    init {
        // recieves a number (eg 128) and calculates what power of 2 it is (eg
        // 2^7)
        val numberOfOctaves = Math.ceil(Math.log10(largestFeature.toDouble()) / Math.log10(2.0)).toInt()
        val goctaves = arrayOfNulls<SimplexNoise_octave>(numberOfOctaves)
        frequencys = DoubleArray(numberOfOctaves)
        amplitudes = DoubleArray(numberOfOctaves)
        val rnd = Random(seed)
        for (i in 0..numberOfOctaves - 1) {
            goctaves[i] = SimplexNoise_octave(rnd.nextInt())
            frequencys[i] = Math.pow(2.0, i.toDouble())
            amplitudes[i] = Math.pow(persistence, (goctaves.size - i).toDouble())
        }
        octaves = goctaves.requireNoNulls()
    }

    fun getNoise(x: Int, y: Int): Double {
        var result = 0.0
        for (i in octaves.indices) {
            // double frequency = Math.pow(2,i);
            // double amplitude = Math.pow(persistence,octaves.length-i);
            result = result + octaves[i].noise(x / frequencys[i], y / frequencys[i]) * amplitudes[i]
        }
        return result
    }

    fun getNoise(x: Int, y: Int, z: Int): Double {
        var result = 0.0
        for (i in octaves.indices) {
            val frequency = Math.pow(2.0, i.toDouble())
            val amplitude = Math.pow(persistence, (octaves.size - i).toDouble())
            result = result + octaves[i].noise(x / frequency, y / frequency, z / frequency) * amplitude
        }
        return result
    }
}