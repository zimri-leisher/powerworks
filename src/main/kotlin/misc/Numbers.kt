package misc

object Numbers {
    fun genRandom(seed: Long, seed2: Long): Long {
        return ((seed / 7).toDouble() * (seed2 % 31).toDouble() * 0.55349).toLong()
    }
    fun max(vararg nums: Int) = nums.max()!!

    fun sign(num: Int) = if(num < 0) -1 else 1
}