package graphics

import main.Game
import main.ResourceManager
import screen.elements.GUIRecipeDisplay
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Rectangle
import java.awt.Transparency
import java.awt.image.BufferedImage

object Utils {

    private val rectTable = mutableMapOf<Rectangle, BufferedImage>()

    /**
     * @return an image of a rectangle using the default parts in res/textures/gui/default
     */
    fun genRectangle(widthPixels: Int, heightPixels: Int): BufferedImage {
        return genRectangle(Image.GUI.DEFAULT_EDGE_TOP, Image.GUI.DEFAULT_EDGE_BOTTOM, Image.GUI.DEFAULT_EDGE_LEFT, Image.GUI.DEFAULT_EDGE_RIGHT, Image.GUI.DEFAULT_CORNER_TOP_RIGHT,
                Image.GUI.DEFAULT_CORNER_TOP_LEFT, Image.GUI.DEFAULT_CORNER_BOTTOM_RIGHT, Image.GUI.DEFAULT_CORNER_BOTTOM_LEFT, Image.GUI.DEFAULT_BACKGROUND, widthPixels, heightPixels)
    }

    fun singlePixelOfColor(color: Int): BufferedImage {
        val ret = Game.graphicsConfiguration.createCompatibleImage(1, 1, Transparency.TRANSLUCENT)
        val g2d = ret.createGraphics()
        with(g2d) {
            g2d.color = Color(color)
            fillRect(0, 0, 1, 1)
            dispose()
        }
        return ret
    }

    /**
     * @return an image of a rectangle using the textures specified
     */
    fun genRectangle(topEdge: Texture, bottomEdge: Texture, leftEdge: Texture, rightEdge: Texture, topRightCorner: Texture, topLeftCorner: Texture, bottomRightCorner: Texture,
                     bottomLeftCorner: Texture, background: Texture, widthPixels: Int, heightPixels: Int): BufferedImage {
        val rect = Rectangle(widthPixels, heightPixels)
        val previous = rectTable[rect]
        if (previous != null) {
            return previous
        }
        val dest = Game.graphicsConfiguration.createCompatibleImage(widthPixels, heightPixels, Transparency.TRANSLUCENT)
        val g = dest.createGraphics()
        with(g) {
            composite = AlphaComposite.SrcOver
            for (x in 0 until widthPixels step background.widthPixels) {
                for (y in 0 until heightPixels step background.heightPixels) {
                    drawImage(background.currentImage, x, y, null)
                }
            }
            for (i in topLeftCorner.widthPixels until widthPixels - topRightCorner.widthPixels step topEdge.widthPixels)
                drawImage(topEdge.currentImage, i, 0, null)
            for (i in topRightCorner.heightPixels until heightPixels - bottomRightCorner.heightPixels step rightEdge.heightPixels)
                drawImage(rightEdge.currentImage, widthPixels - rightEdge.widthPixels, i, null)
            for (i in bottomLeftCorner.widthPixels until widthPixels - bottomRightCorner.widthPixels step bottomEdge.widthPixels)
                drawImage(bottomEdge.currentImage, i, heightPixels - bottomEdge.heightPixels, null)
            for (i in topLeftCorner.heightPixels until heightPixels - bottomLeftCorner.heightPixels step leftEdge.heightPixels)
                drawImage(leftEdge.currentImage, 0, i, null)
            drawImage(topLeftCorner.currentImage, 0, 0, null)
            drawImage(topRightCorner.currentImage, widthPixels - topRightCorner.widthPixels, 0, null)
            drawImage(bottomRightCorner.currentImage, widthPixels - bottomRightCorner.widthPixels, heightPixels - bottomRightCorner.heightPixels, null)
            drawImage(bottomLeftCorner.currentImage, 0, heightPixels - bottomLeftCorner.heightPixels, null)
            dispose()
        }
        rectTable.put(rect, dest)
        return dest
    }

    /**
     * @param i the original image
     * @return a BufferedImage object modified according to the ImageParams object
     */
    fun modify(i: Image, p: ImageParams): BufferedImage {
        return modify(i.currentImage, p)
    }

    /**
     * @param path the path to the original image. Assumes the image is already registered
     * @return a BufferedImage object modified according to the ImageParams object
     */
    fun modify(path: String, p: ImageParams): BufferedImage {
        return modify(ResourceManager.getImage(path), p)
    }

    /**
     * @param image the original image
     * @return a BufferedImage object modified according to the ImageParams object
     */
    fun modify(image: BufferedImage, p: ImageParams): BufferedImage {
        var newImg: BufferedImage = image
        if (p.scale != 1.0) {
            newImg = Game.graphicsConfiguration.createCompatibleImage((image.width * p.scale).toInt(), (image.height * p.scale).toInt(), Transparency.TRANSLUCENT)
            val g2d = newImg.createGraphics()
            with(g2d) {
                drawImage(image, 0, 0, (image.width * p.scale).toInt(), (image.height * p.scale).toInt(), null)
                dispose()
            }
        }
        if (p.scaleWidth != 1.0) {
            newImg = Game.graphicsConfiguration.createCompatibleImage((image.width * p.scaleWidth).toInt(), image.height, Transparency.TRANSLUCENT)
            val g2d = newImg.createGraphics()
            with(g2d) {
                drawImage(image, 0, 0, (image.width * p.scaleWidth).toInt(), image.height, null)
                dispose()
            }
        }
        if (p.scaleHeight != 1.0) {
            newImg = Game.graphicsConfiguration.createCompatibleImage(image.width, (image.height * p.scaleHeight).toInt(), Transparency.TRANSLUCENT)
            val g2d = newImg.createGraphics()
            with(g2d) {
                drawImage(image, 0, 0, image.width, (image.height * p.scaleHeight).toInt(), null)
                dispose()
            }
        }
        if (p.alphaMultiplier != -1) {
            newImg = Game.graphicsConfiguration.createCompatibleImage(image.width, image.height, Transparency.TRANSLUCENT)
            for (y in 0..image.height - 1) {
                for (x in 0..image.width - 1) {
                    val c = Color(image.getRGB(x, y), true)
                    val newC = Color(c.red, c.green, c.blue, p.alphaMultiplier * (c.alpha / 255))
                    newImg.setRGB(x, y, newC.rgb)
                }
            }
        }
        if (p.toRed) {
            newImg = Game.graphicsConfiguration.createCompatibleImage(image.width, image.height, Transparency.TRANSLUCENT)
            for (y in 0..image.height - 1) {
                for (x in 0..image.width - 1) {
                    val c = Color(image.getRGB(x, y))
                    val newC = Color(c.red, 0, 0, c.rgb shr 24 and 0xFF)
                    newImg.setRGB(x, y, newC.rgb)
                }
            }
        }
        if (p.toGreen) {
            newImg = Game.graphicsConfiguration.createCompatibleImage(image.width, image.height, Transparency.TRANSLUCENT)
            for (y in 0..image.height - 1) {
                for (x in 0..image.width - 1) {
                    val c = Color(image.getRGB(x, y))
                    val newC = Color(0, c.green, 0, c.rgb shr 24 and 0xFF)
                    newImg.setRGB(x, y, newC.rgb)
                }
            }
        }
        if (p.toBlue) {
            newImg = Game.graphicsConfiguration.createCompatibleImage(image.width, image.height, Transparency.TRANSLUCENT)
            for (y in 0..image.height - 1) {
                for (x in 0..image.width - 1) {
                    val c = Color(image.getRGB(x, y))
                    val newC = Color(0, 0, c.blue, c.rgb shr 24 and 0xFF)
                    newImg.setRGB(x, y, newC.rgb)
                }
            }
        }
        if (p.redValue != -1) {
            newImg = Game.graphicsConfiguration.createCompatibleImage(image.width, image.height, Transparency.TRANSLUCENT)
            for (y in 0..image.height - 1) {
                for (x in 0..image.width - 1) {
                    val c = Color(image.getRGB(x, y))
                    val newC = Color(p.redValue, c.green, c.blue, c.rgb shr 24 and 0xFF)
                    newImg.setRGB(x, y, newC.rgb)
                }
            }
        }
        if (p.greenValue != -1) {
            newImg = Game.graphicsConfiguration.createCompatibleImage(image.width, image.height, Transparency.TRANSLUCENT)
            for (y in 0..image.height - 1) {
                for (x in 0..image.width - 1) {
                    val c = Color(image.getRGB(x, y))
                    val newC = Color(c.red, p.greenValue, c.blue, c.rgb shr 24 and 0xFF)
                    newImg.setRGB(x, y, newC.rgb)
                }
            }
        }
        if (p.blueValue != -1) {
            newImg = Game.graphicsConfiguration.createCompatibleImage(image.width, image.height, Transparency.TRANSLUCENT)
            for (y in 0..image.height - 1) {
                for (x in 0..image.width - 1) {
                    val c = Color(image.getRGB(x, y))
                    val newC = Color(c.red, c.green, p.blueValue, c.rgb shr 24 and 0xFF)
                    newImg.setRGB(x, y, newC.rgb)
                }
            }
        }
        if (p.rotation != 0) {
            if (p.rotation == 1 || p.rotation == 3) {
                newImg = Game.graphicsConfiguration.createCompatibleImage(image.height, image.width, Transparency.TRANSLUCENT)
                val g2d = newImg.createGraphics()
                with(g2d) {
                    rotate(Math.toRadians(p.rotation.toDouble() * 90), image.width.toDouble() / 2, image.height.toDouble() / 2)
                    drawImage(image, 0, 0, null)
                    dispose()
                }
            } else {
                newImg = Game.graphicsConfiguration.createCompatibleImage(image.width, image.height, Transparency.TRANSLUCENT)
                val g2d = newImg.createGraphics()
                with(g2d) {
                    rotate(Math.toRadians(180.0), image.width.toDouble() / 2, image.height.toDouble() / 2)
                    drawImage(image, 0, 0, null)
                    dispose()
                }
            }
        }
        if (p.brightnessMultiplier != 1.0) {
            newImg = Game.graphicsConfiguration.createCompatibleImage(image.width, image.height, Transparency.TRANSLUCENT)
            val tempImg = Game.graphicsConfiguration.createCompatibleImage(image.width, image.height, Transparency.TRANSLUCENT)
            tempImg.createGraphics().drawImage(image, 0, 0, null)
            val wr = tempImg.raster
            val pixel = IntArray(4)
            for (i in 0 until wr.width) {
                for (j in 0 until wr.height) {
                    wr.getPixel(i, j, pixel)
                    pixel[0] = (pixel[0] * p.brightnessMultiplier).toInt()
                    pixel[1] = (pixel[1] * p.brightnessMultiplier).toInt()
                    pixel[2] = (pixel[2] * p.brightnessMultiplier).toInt()
                    wr.setPixel(i, j, pixel)
                }
            }
            newImg.createGraphics().drawImage(tempImg, 0, 0, null)
        }
        return newImg
    }
}

data class ImageParams(
        /** 1.0 is default, 2.0 is double size, 0.5 is half size */
        val scale: Double = 1.0, val scaleWidth: Double = 1.0, val scaleHeight: Double = 1.0,
        /** -1 is default, 0 is totally translucent, 255 is totally opaque. Note this is a multiplier, not a universal setter, so previously transparent pixels will not be affected */
        val alphaMultiplier: Int = -1,
        /** false is default, true sets all other color bands to 0 and retains this color */
        val toRed: Boolean = false, val toGreen: Boolean = false, val toBlue: Boolean = false,
        /** -1 is default, 255 is max of this color and 0 removes this color. Note this is not a multiplier*/
        val redValue: Int = -1, val greenValue: Int = -1, val blueValue: Int = -1,
        /** 0 is default, 1 is 90 degrees, 2 is 180, etc.*/
        val rotation: Int = 0,
        /** 1.0 is default, 2.0 is double brightness and 0.5 is half brightness*/
        val brightnessMultiplier: Double = 1.0)

class Image constructor(image: BufferedImage) : Texture {

    override var currentImage = image

    override val widthPixels = currentImage.width

    override val heightPixels = currentImage.height

    object Misc {
        // TODO definitely rethink how weapon textures are done, apply this to other things in the future like block textures
        val ERROR = ResourceManager.registerImage("misc/error")
        val ARROW = ResourceManager.registerImage("misc/arrow", Utils.modify(ResourceManager.registerImage("misc/arrow"), ImageParams(alphaMultiplier = 100)))
        val TELEPORT_ICON = ResourceManager.registerImage("misc/teleport_icon")
        val THIN_ARROW = ResourceManager.registerImage("misc/thin_arrow")
    }

    object Block {
        val CHEST_SMALL = ResourceManager.registerImage("block/chest_small")
        val CHEST_LARGE = ResourceManager.registerImage("block/chest_large")
        val CRAFTER = ResourceManager.registerImage("block/crafter")
        val FURNACE = ResourceManager.registerImage("block/furnace")
        val TUBE_4_WAY = ResourceManager.registerImage("block/tube/4_way")
        val TUBE_2_WAY_VERTICAL = ResourceManager.registerImage("block/tube/2_way_vertical")
        val TUBE_2_WAY_HORIZONTAL = ResourceManager.registerImage("block/tube/2_way_horizontal")
        val TUBE_UP_CLOSE = ResourceManager.registerImage("block/tube/up_close")
        val TUBE_RIGHT_CLOSE = ResourceManager.registerImage("block/tube/right_close")
        val TUBE_DOWN_CLOSE = ResourceManager.registerImage("block/tube/down_close")
        val TUBE_LEFT_CLOSE = ResourceManager.registerImage("block/tube/left_close")
        val TUBE_UP_CONNECT = ResourceManager.registerImage("block/tube/up_connect")
        val PIPE_4_WAY = ResourceManager.registerImage("block/pipe/4_way")
        val PIPE_2_WAY_VERTICAL = ResourceManager.registerImage("block/pipe/2_way_vertical")
        val PIPE_2_WAY_HORIZONTAL = ResourceManager.registerImage("block/pipe/2_way_horizontal")
        val PIPE_UP_CLOSE = ResourceManager.registerImage("block/pipe/up_close")
        val PIPE_RIGHT_CLOSE = ResourceManager.registerImage("block/pipe/right_close")
        val PIPE_DOWN_CLOSE = ResourceManager.registerImage("block/pipe/down_close")
        val PIPE_LEFT_CLOSE = ResourceManager.registerImage("block/pipe/left_close")
    }

    object GUI {
        val VIEW_SELECTOR_CLOSE_BUTTON = ResourceManager.registerImage("gui/view_selector_close")
        val VIEW_SELECTOR_CLOSE_BUTTON_HIGHLIGHT = ResourceManager.registerImage("gui/view_selector_close_highlight") // WTF is going on here, for some reason when I use Utils.modify it comes up as black and not red TODO
        val VIEW_SELECTOR_OPEN_BUTTON = ResourceManager.registerImage("gui/view_selector_open")
        val VIEW_SELECTOR_OPEN_BUTTON_HIGHLIGHT = ResourceManager.registerImage("gui/view_selector_open_highlight", Utils.modify(VIEW_SELECTOR_OPEN_BUTTON, ImageParams(brightnessMultiplier = 1.2)))
        val CLOSE_BUTTON = ResourceManager.registerImage("gui/close_button")
        val DRAG_GRIP = ResourceManager.registerImage("gui/drag_grip")
        val DIMENSION_DRAG_GRIP = ResourceManager.registerImage("gui/dimension_drag_grip")
        val SCROLL_BAR_TOP = ResourceManager.registerImage("gui/scroll_bar_top")
        val SCROLL_BAR_MIDDLE = ResourceManager.registerImage("gui/scroll_bar_middle")
        val SCROLL_BAR_BOTTOM = ResourceManager.registerImage("gui/scroll_bar_bottom")
        val SCROLL_BAR_UNHIGHLIGHT_TOP = ResourceManager.registerImage("gui/scroll_bar_unhighlight_top")
        val SCROLL_BAR_UNHIGHLIGHT_MIDDLE = ResourceManager.registerImage("gui/scroll_bar_unhighlight_mid")
        val SCROLL_BAR_UNHIGHLIGHT_BOTTOM = ResourceManager.registerImage("gui/scroll_bar_unhighlight_bottom")
        val SCROLL_BAR_HIGHLIGHT_TOP = ResourceManager.registerImage("gui/scroll_bar_highlight_top")
        val SCROLL_BAR_HIGHLIGHT_MIDDLE = ResourceManager.registerImage("gui/scroll_bar_highlight_mid")
        val SCROLL_BAR_HIGHLIGHT_BOTTOM = ResourceManager.registerImage("gui/scroll_bar_highlight_bottom")
        val SCROLL_BAR_CLICK_TOP = ResourceManager.registerImage("gui/scroll_bar_click_top")
        val SCROLL_BAR_CLICK_MIDDLE = ResourceManager.registerImage("gui/scroll_bar_click_mid")
        val SCROLL_BAR_CLICK_BOTTOM = ResourceManager.registerImage("gui/scroll_bar_click_bottom")
        val MAIN_MENU_LOGO = ResourceManager.registerImage("gui/main_menu_logo")
        val MAIN_MENU_LOGO_2 = ResourceManager.registerImage("gui/main_menu_logo_2")
        val MAIN_MENU_LOGO_3 = ResourceManager.registerImage("gui/main_menu_logo_3")
        val DEFAULT_CORNER_TOP_RIGHT = ResourceManager.registerImage("gui/default/top_right_corner")
        val DEFAULT_CORNER_TOP_LEFT = ResourceManager.registerImage("gui/default/top_left_corner")
        val DEFAULT_CORNER_BOTTOM_RIGHT = ResourceManager.registerImage("gui/default/bottom_right_corner")
        val DEFAULT_CORNER_BOTTOM_LEFT = ResourceManager.registerImage("gui/default/bottom_left_corner")
        val DEFAULT_EDGE_TOP = ResourceManager.registerImage("gui/default/top_edge")
        val DEFAULT_EDGE_BOTTOM = ResourceManager.registerImage("gui/default/bottom_edge")
        val DEFAULT_EDGE_RIGHT = ResourceManager.registerImage("gui/default/right_edge")
        val DEFAULT_EDGE_LEFT = ResourceManager.registerImage("gui/default/left_edge")
        val DEFAULT_BACKGROUND = ResourceManager.registerImage("gui/default/background")
        val RESOURCE_DISPLAY_SLOT = ResourceManager.registerImage("gui/resource_display_slot")
        val ITEM_SLOT = ResourceManager.registerImage("gui/item_slot")
        val ITEM_SLOT_HIGHLIGHT = ResourceManager.registerImage("gui/item_slot_highlight")
        val ITEM_SLOT_DISPLAY = ResourceManager.registerImage("gui/item_slot_display")
        val ITEM_SLOT_CLICK = ResourceManager.registerImage("gui/item_slot_click")
        val MAIN_MENU_BACKGROUND = ResourceManager.registerImage("gui/main_menu_bg")
        val MAIN_MENU_BUTTON_BOX = ResourceManager.registerImage("gui/main_menu_button_box")
        val MAIN_MENU_BACKGROUND_FILLER = ResourceManager.registerImage("gui/main_menu_background_filler", Utils.singlePixelOfColor(0x515151))
        val HOTBAR_SELECTED_SLOT = ResourceManager.registerImage("gui/selected_slot")
        val CRAFTING_ARROW = ResourceManager.registerImage("gui/crafting_arrow")
        val RECIPE_BUTTON_BACKGROUND = ResourceManager.registerImage("gui/recipe_button_background", Utils.genRectangle(GUIRecipeDisplay.WIDTH, GUIRecipeDisplay.HEIGHT))
    }

    object Fluid {
        val MOLTEN_COPPER = ResourceManager.registerImage("fluid/molten_copper")
        val MOLTEN_IRON = ResourceManager.registerImage("fluid/molten_iron")
    }

    object Particle {
        val BLOCK_PLACE = ResourceManager.registerImage("particle/block_place")
    }

    object Item {
        val CIRCUIT = ResourceManager.registerImage("item/circuit")
        val CABLE = ResourceManager.registerImage("item/cable")
        val TUBE = ResourceManager.registerImage("item/tube")
        val PIPE = ResourceManager.registerImage("item/pipe")
        val COPPER_ORE_ITEM = ResourceManager.registerImage("item/copper_ore_raw")
        val IRON_ORE_ITEM = ResourceManager.registerImage("item/iron_ore_raw")
        val IRON_INGOT = ResourceManager.registerImage("item/iron_ingot")
        val COPPER_INGOT = ResourceManager.registerImage("item/copper_ingot")
    }
}
