package data

import level.LevelObject
import java.io.DataOutputStream

class LevelSaveStream(val out: DataOutputStream) {
    fun writeLevelObject(o: LevelObject) {
        o.save(out)
    }


}