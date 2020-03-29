package level.particle

import graphics.Renderer
import graphics.TextureRenderParams
import level.Level
import misc.Geometry

class Particle(
        val type: ParticleType,
        var xPixel: Int,
        var yPixel: Int, var rotation: Int = 0, var level: Level) {

    var ticksLeftBeforeRotation = -1
    var ticksExisted = 0

    init {
        ticksLeftBeforeRotation = type.ticksToRotate
    }

    fun render() {
        if (rotation != 0)
            Renderer.renderTexture(type.texture, xPixel, yPixel, TextureRenderParams(rotation = 90f * rotation))
        else
            Renderer.renderTexture(type.texture, xPixel, yPixel)
    }

    fun update() {
        if (type.ticksToRotate != -1) {
            if (ticksLeftBeforeRotation == 0) {
                ticksLeftBeforeRotation = type.ticksToRotate
                rotation = Geometry.addAngles(1, rotation)
            } else {
                ticksLeftBeforeRotation--
            }
        }
        if (ticksExisted >= type.minTicksToDisappear) {
            val remove = (Math.random() * (type.maxTicksToDisappear - type.minTicksToDisappear)).toInt() == 0
            if (remove) {
                level.remove(this)
            }
        }
        ticksExisted++
    }
}