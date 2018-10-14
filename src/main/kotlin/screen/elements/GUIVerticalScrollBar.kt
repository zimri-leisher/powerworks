package screen.elements

import graphics.Image
import graphics.Renderer
import io.PressType
import main.heightPixels
import main.widthPixels
import misc.Geometry
import screen.mouse.Mouse

interface VerticalScrollable {
    val viewHeightPixels: Int
    val maxHeightPixels: Int
    fun onScroll()
}

class GUIVerticalScrollBar(parent: RootGUIElement,
                           name: String,
                           xAlignment: Alignment, yAlignment: Alignment,
                           heightAlignment: Alignment,
                           open: Boolean = false,
                           layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { WIDTH }, heightAlignment, open, layer) {

    val s = parent as VerticalScrollable
    val currentTextures = arrayOf(Image.GUI.SCROLL_BAR_UNHIGHLIGHT_TOP, Image.GUI.SCROLL_BAR_UNHIGHLIGHT_MIDDLE, Image.GUI.SCROLL_BAR_UNHIGHLIGHT_BOTTOM)
    val otherTextures = arrayOf(Image.GUI.SCROLL_BAR_UNHIGHLIGHT_TOP, Image.GUI.SCROLL_BAR_UNHIGHLIGHT_MIDDLE, Image.GUI.SCROLL_BAR_UNHIGHLIGHT_BOTTOM,
            Image.GUI.SCROLL_BAR_HIGHLIGHT_TOP, Image.GUI.SCROLL_BAR_HIGHLIGHT_MIDDLE, Image.GUI.SCROLL_BAR_HIGHLIGHT_BOTTOM,
            Image.GUI.SCROLL_BAR_CLICK_TOP, Image.GUI.SCROLL_BAR_CLICK_MIDDLE, Image.GUI.SCROLL_BAR_CLICK_BOTTOM)

    var currentScrollBarHeight = 0
    // the top of the scroll bar thingy itself
    var currentPos = 0
        set(value) {
            if (field != value) {
                field = Math.min(Math.max(value, 0), maxPos)
                s.onScroll()
            }
        }
    var dragging = false
    var mYPixelPrev = 0
    val maxPos: Int
        get() = heightPixels - 2 - currentScrollBarHeight

    init {
        updateScrollBarHeight()
    }

    fun updateScrollBarHeight() {
        val s = parent as VerticalScrollable
        // i know, fuck you
        currentPos = currentPos
        currentScrollBarHeight = Math.min((s.viewHeightPixels.toDouble() / (if (s.maxHeightPixels == 0) 1 else s.maxHeightPixels).toDouble() * heightPixels.toDouble()).toInt(), heightPixels - 2)
    }

    private fun setTexture(i: Int) {
        currentTextures[0] = otherTextures[i * 3]
        currentTextures[1] = otherTextures[i * 3 + 1]
        currentTextures[2] = otherTextures[i * 3 + 2]
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        when (type) {
            PressType.PRESSED -> {
                if (intersectsScrollBar(xPixel, yPixel)) {
                    dragging = true
                    mYPixelPrev = yPixel
                    setTexture(2)
                }
            }
            PressType.RELEASED -> {
                if (mouseOn && intersectsScrollBar(xPixel, yPixel))
                    setTexture(1)
                else
                    setTexture(0)
                dragging = false
            }
        }
    }

    private fun intersectsScrollBar(xPixel: Int, yPixel: Int) =
            Geometry.intersects(xPixel, yPixel, 1, 1, this.xPixel + 1, this.yPixel + heightPixels - currentPos - 1 - currentScrollBarHeight, 4, currentScrollBarHeight)

    override fun onScroll(dir: Int) {
        currentPos += dir * GUIElementList.SCROLL_SENSITIVITY
    }

    override fun onParentChange(oldParent: RootGUIElement) {
        if (parent !is VerticalScrollable)
            throw Exception("Parent must be VerticalScrollable")
        updateScrollBarHeight()
    }

    override fun onParentDimensionChange(oldWidth: Int, oldHeight: Int) {
        updateScrollBarHeight()
    }

    override fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
        updateScrollBarHeight()
    }

    override fun onMouseLeave() {
        if (!dragging)
            setTexture(0)
    }

    override fun update() {
        val mYPixel = Mouse.yPixel
        val mXPixel = Mouse.xPixel
        if (mouseOn && !dragging) {
            if (intersectsScrollBar(Mouse.xPixel, Mouse.yPixel))
                setTexture(1)
            else
                setTexture(0)
        }
        if (mYPixel != mYPixelPrev && dragging) {
            currentPos -= mYPixel - mYPixelPrev
            mYPixelPrev = mYPixel
        }
    }

    override fun render() {
        Renderer.renderTexture(Image.GUI.SCROLL_BAR_TOP, xPixel, yPixel + heightPixels - 2)
        Renderer.renderTexture(Image.GUI.SCROLL_BAR_MIDDLE, xPixel, yPixel + 2, WIDTH, heightPixels - 4)
        Renderer.renderTexture(Image.GUI.SCROLL_BAR_BOTTOM, xPixel, yPixel)
        Renderer.renderTexture(currentTextures[0], xPixel + 1, yPixel + heightPixels - currentPos - currentTextures[0].heightPixels - 1)
        Renderer.renderTexture(currentTextures[1], xPixel + 1, yPixel + heightPixels - currentPos - currentScrollBarHeight + currentTextures[2].heightPixels - 2, currentTextures[1].widthPixels, currentScrollBarHeight - currentTextures[0].heightPixels)
        Renderer.renderTexture(currentTextures[2], xPixel + 1, yPixel + heightPixels - currentPos - currentScrollBarHeight - 1)
    }

    companion object {
        const val WIDTH = 6
    }
}