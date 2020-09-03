package screen.mouse.tool

import io.Control
import io.ControlEvent
import io.ControlEventType
import level.LevelManager
import level.getResourceNodesAt
import resource.ResourceNode

object ResourceNodeEditor : Tool(Control.EDIT_RESOURCE_NODE) {

    var selectedNodes = setOf<ResourceNode>()

    init {
        activationPredicate = {
            selectedNodes.isNotEmpty()
        }
    }

    override fun onUse(event: ControlEvent, mouseLevelX: Int, mouseLevelY: Int): Boolean {
        if (event.type == ControlEventType.RELEASE) {
            // TODO open resource node editor
            return true
        }
        return false
    }

    override fun update() {
        selectedNodes = LevelManager.levelUnderMouse?.getResourceNodesAt(LevelManager.mouseLevelX shr 4, LevelManager.mouseLevelY shr 4)
                ?: emptySet()
    }
}