package screen

import graphics.Image
import graphics.Renderer
import graphics.Texture
import io.InputManager
import io.PressType
import misc.GeometryHelper

interface VerticalScrollable {
    var viewHeightPixels: Int
    var maxHeightPixels: Int
    fun onScroll()
}

class GUIVerticalScrollBar(parent: RootGUIElement? = RootGUIElementObject, name: String, xPixel: Int, yPixel: Int, heightPixels: Int, layer: Int = (parent?.layer ?: 0) + 1) : GUIElement(parent, name, xPixel, yPixel, WIDTH, heightPixels, layer) {

    val s = parent as VerticalScrollable
    val currentTextures = arrayOf<Texture>(Image.GUI.SCROLL_BAR_UNHIGHLIGHT_TOP, Image.GUI.SCROLL_BAR_UNHIGHLIGHT_MIDDLE, Image.GUI.SCROLL_BAR_UNHIGHLIGHT_BOTTOM)
    val otherTextures = arrayOf<Texture>(Image.GUI.SCROLL_BAR_UNHIGHLIGHT_TOP, Image.GUI.SCROLL_BAR_UNHIGHLIGHT_MIDDLE, Image.GUI.SCROLL_BAR_UNHIGHLIGHT_BOTTOM,
            Image.GUI.SCROLL_BAR_HIGHLIGHT_TOP, Image.GUI.SCROLL_BAR_HIGHLIGHT_MIDDLE, Image.GUI.SCROLL_BAR_HIGHLIGHT_BOTTOM,
            Image.GUI.SCROLL_BAR_CLICK_TOP, Image.GUI.SCROLL_BAR_CLICK_MIDDLE, Image.GUI.SCROLL_BAR_CLICK_BOTTOM)

    var currentScrollBarHeight = 0
    var currentPos = 0
        set(value) {
            if(value <= maxPos && value > 0)
                field = value
        }
    var dragging = false
    var mYPixelPrev = 0
    var maxPos = heightPixels - 2 - currentScrollBarHeight
        get() = heightPixels - 2 - currentScrollBarHeight

    init {
        updateScrollBarHeight()
    }

    fun updateScrollBarHeight() {
        val s = parent as VerticalScrollable
        currentScrollBarHeight = Math.min((s.viewHeightPixels.toDouble() / (if (s.maxHeightPixels == 0) 1 else s.maxHeightPixels).toDouble() * heightPixels.toDouble()).toInt(), heightPixels - 2)
    }

    private fun setTexture(i: Int) {
        currentTextures[0] = otherTextures[i * 3]
        currentTextures[1] = otherTextures[i * 3 + 1]
        currentTextures[2] = otherTextures[i * 3 + 2]
    }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int) {
        when (type) {
            PressType.PRESSED -> if (GeometryHelper.intersects(xPixel, yPixel, 1, 1, this.xPixel + 1, currentPos + this.yPixel + 1, 4, currentScrollBarHeight)) {
                dragging = true
                mYPixelPrev = yPixel
                setTexture(2)
            }
            PressType.RELEASED -> {
                if (mouseOn && GeometryHelper.intersects(xPixel, yPixel, 1, 1, this.xPixel + 1, currentPos + this.yPixel + 1, 4, currentScrollBarHeight))
                    setTexture(1)
                else
                    setTexture(0)
                dragging = false
            }
        }
    }

    override fun onMouseActionOff(type: PressType, xPixel: Int, yPixel: Int) {
        if (type == PressType.RELEASED) {
            setTexture(0)
            dragging = false
        }
    }

    override fun onParentChange(oldParent: RootGUIElement) {
        updateScrollBarHeight()
    }

    override fun onMouseLeave() {
        if(!dragging)
            setTexture(0)
    }

    override fun update() {
        val mYPixel = InputManager.mouseYPixel
        val mXPixel = InputManager.mouseXPixel
        if (mouseOn && !dragging) {
            if (GeometryHelper.intersects(mXPixel, mYPixel, 1, 1, xPixel + 1, currentPos + yPixel + 1, 4, currentScrollBarHeight))
                setTexture(1)
            else
                setTexture(0)
        }
        if (mYPixel != mYPixelPrev && dragging) {
            if (yPixel + currentPos + (mYPixel - mYPixelPrev) + 1 + currentScrollBarHeight <= yPixel + heightPixels - 1 && yPixel + (currentPos + (mYPixel - mYPixelPrev)) >= yPixel) {
                currentPos += mYPixel - mYPixelPrev
                mYPixelPrev = mYPixel
                s.onScroll()
            }
        }
    }

    override fun render() {
        Renderer.renderTexture(Image.GUI.SCROLL_BAR_TOP, xPixel, yPixel)
        Renderer.renderTexture(Image.GUI.SCROLL_BAR_MIDDLE, xPixel, yPixel + 2, 6, heightPixels - 4)
        Renderer.renderTexture(Image.GUI.SCROLL_BAR_BOTTOM, xPixel, yPixel + heightPixels - 2)
        Renderer.renderTexture(currentTextures[0], xPixel + 1, currentPos + 1 + yPixel)
        for (i in currentTextures[0].heightPixels..currentScrollBarHeight - currentTextures[2].heightPixels - 1)
            Renderer.renderTexture(currentTextures[1], xPixel + 1, i + currentPos + 1 + yPixel)
        Renderer.renderTexture(currentTextures[2], xPixel + 1, currentScrollBarHeight - currentTextures[2].heightPixels + yPixel + currentPos + 1)
    }

    companion object {
        const val WIDTH = 6
    }
}