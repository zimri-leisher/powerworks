package screen

import graphics.Image
import graphics.Renderer
import io.*
import main.Game
import misc.GeometryHelper

object IngameDefaultGUI : GUI("In game default gui", 0, 0, Game.WIDTH, Game.HEIGHT), ControlPressHandler {

    var viewCount = 1
    val viewControls = mutableListOf<GUIElement>()
    const val MAX_VIEWS = 4
    val DEFAULT_VIEW_WIDTH = 300
    val DEFAULT_VIEW_HEIGHT = (DEFAULT_VIEW_WIDTH.toDouble() / 16 * 9).toInt()
    val VIEW_SELECTOR_BUTTON_WIDTH = 5
    val VIEW_SELECTOR_BUTTON_HEIGHT = 5
    var viewControlsOpen = false

    init {
        InputManager.registerControlPressHandler(this, Control.TOGGLE_VIEW_CONTROLS)
        adjustDimensions = true
        GUITexturePane(this, "In game default gui background", 0, 0, Image.GUI.MAIN_MENU_BACKGROUND, Game.WIDTH, Game.HEIGHT).run {
            adjustDimensions = true
            object : GUIElement(this, "In game default view selector", Game.WIDTH - 30, Game.HEIGHT - 7, 28, 5) {

                val views = arrayOf<GUIView>(newView(), newView(), newView(), newView())

                var viewHighlighted = -1

                init {
                    matchParentOpening = false
                    views[0].open = true
                    viewControls.add(this)
                }

                private fun newView(): GUIView {
                    val parent = this@IngameDefaultGUI.get("In game default gui background")
                    GUIView(parent, "In game default view ${viewCount++}", 0, 0, DEFAULT_VIEW_WIDTH, DEFAULT_VIEW_HEIGHT, Game.player,
                            layer = parent!!.layer + viewCount * 3 + 1).run {
                        GUIDragGrip(this, "In game default view $viewCount drag grip", DEFAULT_VIEW_WIDTH - 5, DEFAULT_VIEW_HEIGHT - 5, this.layer + 2).run {
                            matchParentOpening = false
                            viewControls.add(this)
                        }
                        GUIOutline(this, "In game default view $viewCount outline")
                        GUICloseButton(this, "In game default view $viewCount close button", DEFAULT_VIEW_WIDTH - 11, DEFAULT_VIEW_HEIGHT - 5, this.layer + 2).run {
                            matchParentOpening = false
                            viewControls.add(this)
                        }
                        matchParentOpening = false
                        return this
                    }
                }

                override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
                    if (type == PressType.PRESSED) {
                        // If it is on the space in between one of the four buttons, don't do anything
                        if ((xPixel - this.xPixel) % (VIEW_SELECTOR_BUTTON_WIDTH + 2) > VIEW_SELECTOR_BUTTON_WIDTH) return
                        // Get which view number it is on
                        val viewNum = (xPixel - this.xPixel) / (VIEW_SELECTOR_BUTTON_WIDTH + 2)
                        val view = views[viewNum]
                        view.toggle()
                        if (view.open) {
                            view.children.filter { viewControls.contains(it) }.forEach { it.open = true }
                        }
                    }
                }

                override fun update() {
                    if (GeometryHelper.intersects(Mouse.xPixel, Mouse.yPixel, 0, 0, xPixel, yPixel, widthPixels, heightPixels)) {
                        if ((Mouse.xPixel - this.xPixel) % (VIEW_SELECTOR_BUTTON_WIDTH + 2) > 5) {
                            viewHighlighted = -1; return
                        }
                        val viewNum = (Mouse.xPixel - this.xPixel) / (VIEW_SELECTOR_BUTTON_WIDTH + 2)
                        viewHighlighted = viewNum
                    } else
                        viewHighlighted = -1
                }

                override fun onParentDimensionChange(oldWidth: Int, oldHeight: Int) {
                    relXPixel = Game.WIDTH - 30
                    relYPixel = Game.HEIGHT - 7
                }

                override fun render() {
                    var i = 0
                    for (v in views) {
                        if (i != viewHighlighted)
                            Renderer.renderTexture(if (v.open) Image.GUI.VIEW_SELECTOR_CLOSE_BUTTON else Image.GUI.VIEW_SELECTOR_OPEN_BUTTON, xPixel + i * 7, yPixel)
                        else
                            Renderer.renderTexture(if (v.open) Image.GUI.VIEW_SELECTOR_CLOSE_BUTTON_HIGHLIGHT else Image.GUI.VIEW_SELECTOR_OPEN_BUTTON_HIGHLIGHT, xPixel + i * 7, yPixel)
                        i++
                    }
                }
            }
        }
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.TOGGLE_VIEW_CONTROLS && open && p.pressType == PressType.PRESSED) {
            viewControlsOpen = true
            viewControls.filter { it.parent.open }.forEach { it.toggle() }
        }
    }

}