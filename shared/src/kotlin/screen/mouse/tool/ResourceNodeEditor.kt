package screen.mouse.tool

import io.Control
import io.ControlEvent
import io.ControlEventType
import level.LevelManager
import level.getResourceNodesAt
import resource.ResourceNode
import screen.RoutingLanguageEditor

object ResourceNodeEditor : Tool(Control.EDIT_RESOURCE_NODE) {

    var selectedNodes = setOf<ResourceNode>()

    init {
        activationPredicate = {
            selectedNodes.isNotEmpty()
        }
    }

    override fun onUse(event: ControlEvent, mouseLevelXPixel: Int, mouseLevelYPixel: Int): Boolean {
        if (event.type == ControlEventType.RELEASE) {
            RoutingLanguageEditor.node = selectedNodes.first()
            RoutingLanguageEditor.open = true
            return true
        }
        return false
    }

    override fun update() {
        selectedNodes = LevelManager.levelUnderMouse?.getResourceNodesAt(LevelManager.mouseLevelXPixel shr 4, LevelManager.mouseLevelYPixel shr 4)
                ?: emptySet()
    }
}