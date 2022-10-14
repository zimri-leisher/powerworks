package screen.mouse.tool

import graphics.Renderer
import graphics.TextureRenderParams
import io.Control
import io.ControlEvent
import io.ControlEventType
import item.EntityItemType
import level.Level
import level.LevelManager
import level.getCollisionsWith
import main.toColor
import player.ActionLevelObjectPlace
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

    override fun onUse(event: ControlEvent, mouseLevelX: Int, mouseLevelY: Int): Boolean {
        if (event.type == ControlEventType.RELEASE && event.control == Control.SPAWN_ENTITY) {
            if (canSpawn) {
                PlayerManager.takeAction(ActionLevelObjectPlace(PlayerManager.localPlayer, type!!.spawnedEntity, mouseLevelX, mouseLevelY, LevelManager.levelUnderMouse!!))
            }
            return true
        }
        return false
    }

    override fun renderAbove(level: Level) {
        if(level == LevelManager.levelUnderMouse) {
            if (type != null) {
                val entity = type!!.spawnedEntity
                val texture = entity.textures[0]
                texture.render(LevelManager.mouseLevelX, LevelManager.mouseLevelY, params = TextureRenderParams(color = toColor(alpha = 0.4f)))
                Renderer.renderEmptyRectangle(LevelManager.mouseLevelX + entity.hitbox.xStart, LevelManager.mouseLevelY + entity.hitbox.yStart, entity.hitbox.width, entity.hitbox.height,
                        params = TextureRenderParams(color = toColor(
                                if (canSpawn) 0f else 1f, if (canSpawn) 1f else 0f, 0f, 0.4f)))
            }
        }
    }

    override fun update() {
        type = Mouse.heldItemType as? EntityItemType
        if (type != null) {
            canSpawn = LevelManager.levelObjectUnderMouse == null && LevelManager.levelUnderMouse?.getCollisionsWith(type!!.spawnedEntity.hitbox, LevelManager.mouseLevelX, LevelManager.mouseLevelY)?.none() ?: false
        }
    }
}