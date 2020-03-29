package screen

import resource.ResourceNode
import routing.EvaluationException
import routing.RoutingLanguage
import screen.elements.*

object RoutingLanguageEditor : AutoFormatGUIWindow("Routing language editor window", { 0 }, { 0 }, ScreenManager.Groups.PLAYER_UTIL) {

    var node: ResourceNode? = null

    class GUIRoutingLanguageRule(parent: RootGUIElement, name: String) : GUIElement(parent, name, 0, 0, GUIButton.WIDTH, GUIButton.HEIGHT + 16) {
        init {
            GUIDefaultTextureRectangle(this, "routing language rule background", 0, 0).apply {

            }
        }
    }

    init {
        openAtMouse = true
        GUIText(group, "allow output if text", 0, 0, "Allow output if:")
        GUITextInputField(group, "text input for routing language", { 2 }, { 2 }, 30, 1, allowTextScrolling = true, onPressEnter = { currentText ->
            try {
                //node!!.behavior.allowOut = RoutingLanguage.parse(currentText)
                positiveFlashOutline()
            } catch (e: EvaluationException) {
                println(e.message)
                negativeFlashOutline()
            }
        })
        GUIText(group, "force output if text", 0, 0, "Push out if:")
        generateCloseButton()
        generateDragGrip()
    }
}