package screen.mouse.tool

import graphics.Renderer
import graphics.TextureRenderParams
import io.Control
import io.PressType
import item.EntityItemType
import level.LevelManager
import level.getCollisionsWith
import main.toColor
import player.PlaceLevelObject
import player.PlayerManager
import screen.mouse.Mouse

object EntityPlacer : Tool(Control.SPAWN_ENTITY) {

    var type: EntityItemType? = null
    var canSpawn = false

    init {
        activationPredicate = {
            val item = Mouse.heldItemType
            LevelManager.levelObjectUnderMouse == null && item != null && item is EntityItemType && !Selector.dragging
        }
    }

    override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int): Boolean {
        if (type == PressType.RELEASED && control == Control.SPAWN_ENTITY) {
            if (canSpawn) {
                PlayerManager.takeAction(PlaceLevelObject(PlayerManager.localPlayer, EntityPlacer.type!!.spawnedEntity, mouseLevelXPixel, mouseLevelYPixel, 0, LevelManager.levelUnderMouse!!))
            }
            return true
        }
        return false
    }

    override fun renderAbove() {
        if(type != null) {
            val entity = type!!.spawnedEntity
            val texture = entity.textures[0]
            texture.render(LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel, params = TextureRenderParams(color = toColor(alpha = 0.4f)))
            Renderer.renderEmptyRectangle(LevelManager.mouseLevelXPixel + entity.hitbox.xStart, LevelManager.mouseLevelYPixel + entity.hitbox.yStart, entity.hitbox.width, entity.hitbox.height,
                    params = TextureRenderParams(color = toColor(
                            if (canSpawn) 0f else 1f, if (canSpawn) 1f else 0f, 0f, 0.4f)))
        }
    }

    override fun update() {
        type = Mouse.heldItemType as? EntityItemType
        if(type != null) {
            canSpawn = LevelManager.levelObjectUnderMouse == null && LevelManager.levelUnderMouse?.getCollisionsWith(type!!.spawnedEntity.hitbox, LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel)?.none() ?: false
        }
    }
}