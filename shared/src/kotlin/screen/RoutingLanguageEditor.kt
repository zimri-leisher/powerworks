package screen

import graphics.TextureRenderParams
import graphics.text.TextManager
import graphics.text.TextRenderParams
import io.*
import misc.Numbers.ceil
import player.EditResourceNodeBehaviorAction
import player.PlayerManager
import resource.ResourceNode
import resource.ResourceType
import routing.script.CompileException
import routing.script.RoutingLanguage
import routing.script.RoutingLanguageStatement
import screen.elements.*

// TODO make all GUIElements open classes

object RoutingLanguageEditor : GUIWindow("Routing language editor window", { 0 }, { 0 }, { 32 + 4 }, { GUIButton.HEIGHT * 2 + 6 }),
        ControlEventHandler {

    var node: ResourceNode? = null
    private lateinit var inputs: GUIGroup
    private lateinit var outputs: GUIGroup
    private var inputsFormatGroup: AutoFormatGUIGroup
    private lateinit var outputsFormatGroup: AutoFormatGUIGroup
    private var allowInput: GUIRoutingLanguageRule
    private var forceInput: GUIRoutingLanguageRule
    private var allowOutput: GUIRoutingLanguageRule
    private var forceOutput: GUIRoutingLanguageRule
    private lateinit var selector: GUIDefaultTextureRectangle

    init {
        InputManager.register(this, Control.Group.INTERACTION)
        openAtMouse = true
        allowEscapeToClose = true
        selector = GUIDefaultTextureRectangle(this, "Routing language editor background").apply {
            GUIButton(this, "Routing language editor open input config button", { 2 }, { heightPixels - GUIButton.HEIGHT - 2 }, "Input", widthAlignment = { 32 }, onRelease = {
                selector.open = false
                inputs.open = true
                this@RoutingLanguageEditor.alignments.updateDimension()
                this@RoutingLanguageEditor.keepInScreen()
            })
            GUIButton(this, "Routing language editor open output config button", { 2 }, { heightPixels - GUIButton.HEIGHT * 2 - 4 }, "Output", widthAlignment = { 32 }, onRelease = {
                selector.open = false
                outputs.open = true
                this@RoutingLanguageEditor.alignments.updateDimension()
                this@RoutingLanguageEditor.keepInScreen()
            })
        }
        inputs = GUIGroup(this, "Routing language input editor group", { 0 }, { heightPixels }).apply {
            inputsFormatGroup = AutoFormatGUIGroup(this, "Routing language input editor", { 2 }, { 0 }, dir = 2).apply {
                allowInput = GUIRoutingLanguageRule(this, "Routing language input rules", { 0 }, { 0 }, "Allow input if...",
                        { statement ->
                            PlayerManager.takeAction(EditResourceNodeBehaviorAction(PlayerManager.localPlayer, node!!.toReference(), node!!.behavior.copy(node!!).apply { allowIn.setStatement(statement) }))
                        })
                forceInput = GUIRoutingLanguageRule(this, "Routing language output rules", { 0 }, { 0 }, "Force input if...",
                        { statement ->
                            PlayerManager.takeAction(EditResourceNodeBehaviorAction(PlayerManager.localPlayer, node!!.toReference(), node!!.behavior.copy(node!!).apply { forceIn.setStatement(statement) }))
                        })
            }
            GUIDefaultTextureRectangle(this, "Routing language output editor background", { 0 }, { -inputsFormatGroup.alignments.height() - 2 }, { inputsFormatGroup.alignments.width() + 4 }, { inputsFormatGroup.alignments.height() + 4 })
            matchParentOpening = false
        }
        outputs = GUIGroup(this, "Routing language output editor group", { 0 }, { heightPixels }).apply {
            outputsFormatGroup = AutoFormatGUIGroup(this, "Routing language output editor", { 2 }, { 0 }, dir = 2, layer = layer + 2).apply {
                allowOutput = GUIRoutingLanguageRule(this, "Routing language output rules", { 0 }, { 0 }, "Allow output if...",
                        { statement ->
                            println("jklasdfjklasdf")
                            PlayerManager.takeAction(EditResourceNodeBehaviorAction(PlayerManager.localPlayer, node!!.toReference(), node!!.behavior.copy(node!!).apply { allowOut.setStatement(statement) }))
                        })
                forceOutput = GUIRoutingLanguageRule(this, "Routing language output rules", { 0 }, { 0 }, "Force output if...",
                        { statement ->
                            PlayerManager.takeAction(EditResourceNodeBehaviorAction(PlayerManager.localPlayer, node!!.toReference(), node!!.behavior.copy(node!!).apply { forceOut.setStatement(statement) }))
                        })
            }
            GUIDefaultTextureRectangle(this, "Routing language output editor background", { 0 }, { -outputsFormatGroup.alignments.height() - 2 }, { outputsFormatGroup.alignments.width() + 4 }, { outputsFormatGroup.alignments.height() + 4 })
            matchParentOpening = false
        }
        alignments.width = {
            if (inputs.open) {
                inputs.widthPixels
            } else if (outputs.open) {
                outputs.widthPixels
            } else {
                selector.widthPixels
            }
        }
        alignments.height = {
            if (inputs.open) {
                inputs.heightPixels
            } else if (outputs.open) {
                outputs.heightPixels
            } else {
                selector.heightPixels
            }
        }
    }

    override fun handleControlEvent(event: ControlEvent) {
        if (event.type == ControlEventType.PRESS && event.control in Control.Group.INTERACTION) {
            if (!mouseOn && !ResourceTypeSelector.mouseOn) {
                open = false
            }
        }
    }

    override fun onMouseLeave() {
        if (selector.open) {
            open = false
        }
    }

    override fun onOpen() {
        if (node == null) {
            open = false
        } else {
            xPixel -= 2
            yPixel += 2
            val node = node!!
            allowOutput.clearStatements()
            node.behavior.allowOut.statements.forEach { statement, types ->
                allowOutput.addStatement(statement, types)
            }
            allowInput.clearStatements()
            node.behavior.allowIn.statements.forEach { statement, types ->
                allowInput.addStatement(statement, types)
            }
            forceOutput.clearStatements()
            node.behavior.forceOut.statements.forEach { statement, types ->
                forceOutput.addStatement(statement, types)
            }
            forceInput.clearStatements()
            node.behavior.forceIn.statements.forEach { statement, types ->
                forceInput.addStatement(statement, types)
            }
            inputsFormatGroup.reformat()
            outputsFormatGroup.reformat()
            inputs.updateDimensions()
            outputs.updateDimensions()
        }
    }

    override fun update() {
        if (node != null) {
            for (statement in allowInput.statements) {
                val result = statement.currentStatement.evaluate(node!!)
                statement.evaluationText.text = "And the following condition is true... (currently: $result)"
            }
            for (statement in allowOutput.statements) {
                val result = statement.currentStatement.evaluate(node!!)
                statement.evaluationText.text = "And the following condition is true... (currently: $result)"
            }
            for (statement in forceInput.statements) {
                val result = statement.currentStatement.evaluate(node!!)
                statement.evaluationText.text = "And the following condition is true... (currently: $result)"
            }
            for (statement in forceOutput.statements) {
                val result = statement.currentStatement.evaluate(node!!)
                statement.evaluationText.text = "And the following condition is true... (currently: $result)"
            }
        }
    }

    override fun onClose() {
        outputs.open = false
        inputs.open = false
        alignments.updateDimension()
    }
}

class GUIRoutingLanguageRule(parent: RootGUIElement, name: String, xAlignment: Alignment, yAlignment: Alignment, val displayName: String, val onEnterStatement: (statement: RoutingLanguageStatement) -> Unit) : GUIElement(parent, name, xAlignment, yAlignment, { 0 }, { 0 }) {

    private var nextStatementId = 0
    val statements = mutableListOf<GUIRoutingLanguageStatement>()
    private val statementsGroup: AutoFormatGUIGroup
    private val background: GUIDefaultTextureRectangle
    private var hidden = false

    init {
        background = GUIDefaultTextureRectangle(this, "routing language rule background", { 0 }, { 0 }).apply {
            statementsGroup = AutoFormatGUIGroup(this, "routing language rule statement auto format group", { 1 }, { heightPixels - 2 }, dir = 2).apply {
                GUIText(this, "statement name text", { 0 }, { 0 }, displayName)
            }
            alignments.width = { statementsGroup.alignments.width() + 2 }
            alignments.height = { statementsGroup.alignments.height() + 4 }
        }
        statementsGroup.alignments.updatePosition()
        alignments.width = { background.alignments.width() }
        alignments.height = { background.alignments.height() }
    }

    fun clearStatements() {
        statements.clear()
        statementsGroup.children.clear()
        GUIText(statementsGroup, "statement name text", { 0 }, { 0 }, displayName)
        statementsGroup.reformat()
        background.alignments.updateDimension()
        alignments.updateDimension()
    }

    fun addStatement(statement: RoutingLanguageStatement, types: List<ResourceType>) {
        val statementGUI = GUIRoutingLanguageStatement(statementsGroup, "routing language statement $nextStatementId", statement, types, onEnterStatement)
        statements.add(statementGUI)
        nextStatementId++
        statementsGroup.reformat()
        background.alignments.updateDimension()
        alignments.updateDimension()
    }
}

class GUIRoutingLanguageStatement(parent: RootGUIElement, name: String, var currentStatement: RoutingLanguageStatement, types: List<ResourceType>, val onEnterStatement: (statement: RoutingLanguageStatement) -> Unit) :
        GUIElement(parent, name,
                { 0 }, { 0 },
                { (TextManager.getFont().charWidth * 30).toInt() + 2 },
                { ceil(TextManager.getFont().charHeight) * 3 + 15 }) {

    val textInput = GUITextInputField(this, "statement text input",
            { 0 }, { 0 },
            30, 1,
            onPressEnter = { compile(it) }, allowTextScrolling = true, defaultValue = currentStatement.text)

    val evaluationText: GUIText

    init {
        GUIText(this, "statement type selector name text", 2, heightPixels - ceil(TextManager.getFont().charHeight), "Type is one of...", TextRenderParams(size = 12))
        evaluationText = GUIText(this, "statement type selector language box name", 2, textInput.heightPixels + 1, "And the following condition is true... (currently: false)", TextRenderParams(size = 12))
        GUIResourceTypeSelection(this, "statement type selection",
                { 3 }, { evaluationText.alignments.y() + evaluationText.heightPixels + 4 },
                columns = (textInput.widthPixels) / 9,
                allowRowGrowth = true, maxRows = 1, layer = this.layer + 2, startingTypes = types.toMutableList()).apply {
            GUIDefaultTextureRectangle(this, "Statement type selection background",
                    { -2 }, { -3 }, { alignments.width() + 4 }, { alignments.height() + 4 }, layer = this.layer - 1).apply {
                localRenderParams = TextureRenderParams(rotation = 180f, brightness = 0.7f)
            }
        }

        /*
        GUIButton(this, "routing statement removal button",
                { textInput.alignments.width() + 1 }, { -1 },
                "<img=gui/minus>",
                true,
                { ceil(TextManager.getFont().charHeight) + 4 }, { ceil(TextManager.getFont().charHeight) + 4 },
                onRelease = { removeThisStatement() })
         */
    }

    fun compile(text: String) {
        try {
            currentStatement = RoutingLanguage.parse(text)
            textInput.positiveFlashOutline()
            onEnterStatement(currentStatement)
        } catch (e: CompileException) {
            println(e.message)
            textInput.negativeFlashOutline()
        }
    }
}