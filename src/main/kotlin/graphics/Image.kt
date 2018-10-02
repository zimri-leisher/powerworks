package graphics

import com.badlogic.gdx.graphics.Texture
import data.ResourceManager

object Image {

    object Misc {
        // TODO definitely rethink how weapon textures are done, apply this to other things in the future like block textures
        val ERROR = ResourceManager.getTextureFromAtlas("misc/error")
        val ARROW = ResourceManager.getTextureFromAtlas("misc/arrow")
        val TELEPORT_ICON = ResourceManager.getTextureFromAtlas("misc/teleport_icon")
        val THIN_ARROW = ResourceManager.getTextureFromAtlas("misc/thin_arrow")
        val BACK_ARROW = ResourceManager.getTextureFromAtlas("misc/back_arrow")
    }

    object Block {
        val CHEST_SMALL = ResourceManager.getTextureFromAtlas("block/chest_small")
        val CHEST_LARGE = ResourceManager.getTextureFromAtlas("block/chest_large")
        val CRAFTER = ResourceManager.getTextureFromAtlas("block/crafter")
        val FURNACE = ResourceManager.getTextureFromAtlas("block/furnace")
        val SOLIDIFIER = ResourceManager.getTextureFromAtlas("block/solidifier")
        val TUBE_4_WAY = ResourceManager.getTextureFromAtlas("block/tube/4_way")
        val TUBE_2_WAY_VERTICAL = ResourceManager.getTextureFromAtlas("block/tube/2_way_vertical")
        val TUBE_2_WAY_HORIZONTAL = ResourceManager.getTextureFromAtlas("block/tube/2_way_horizontal")
        val TUBE_UP_CLOSE = ResourceManager.getTextureFromAtlas("block/tube/up_close")
        val TUBE_RIGHT_CLOSE = ResourceManager.getTextureFromAtlas("block/tube/right_close")
        val TUBE_DOWN_CLOSE = ResourceManager.getTextureFromAtlas("block/tube/down_close")
        val TUBE_LEFT_CLOSE = ResourceManager.getTextureFromAtlas("block/tube/left_close")
        val TUBE_UP_CONNECT = ResourceManager.getTextureFromAtlas("block/tube/up_connect")
        val PIPE_UP_CONNECT = ResourceManager.getTextureFromAtlas("block/pipe/up_connect")
        val PIPE_4_WAY = ResourceManager.getTextureFromAtlas("block/pipe/4_way")
        val PIPE_2_WAY_VERTICAL = ResourceManager.getTextureFromAtlas("block/pipe/2_way_vertical")
        val PIPE_2_WAY_HORIZONTAL = ResourceManager.getTextureFromAtlas("block/pipe/2_way_horizontal")
        val PIPE_UP_CLOSE = ResourceManager.getTextureFromAtlas("block/pipe/up_close")
        val PIPE_RIGHT_CLOSE = ResourceManager.getTextureFromAtlas("block/pipe/right_close")
        val PIPE_DOWN_CLOSE = ResourceManager.getTextureFromAtlas("block/pipe/down_close")
        val PIPE_LEFT_CLOSE = ResourceManager.getTextureFromAtlas("block/pipe/left_close")
    }

    object GUI {
        val VIEW_SELECTOR_CLOSE_BUTTON = ResourceManager.getTextureFromAtlas("gui/view_selector_close")
        val VIEW_SELECTOR_CLOSE_BUTTON_HIGHLIGHT = ResourceManager.getTextureFromAtlas("gui/view_selector_close_highlight")
        val VIEW_SELECTOR_OPEN_BUTTON = ResourceManager.getTextureFromAtlas("gui/view_selector_open")
        val VIEW_SELECTOR_OPEN_BUTTON_HIGHLIGHT = ResourceManager.getTextureFromAtlas("gui/view_selector_open_highlight")
        val CLOSE_BUTTON = ResourceManager.getTextureFromAtlas("gui/close_button")
        val CLOSE_BUTTON_HIGHLIGHT = ResourceManager.getTextureFromAtlas("gui/close_button_highlight")
        val DRAG_GRIP = ResourceManager.getTextureFromAtlas("gui/drag_grip")
        val DRAG_GRIP_HIGHLIGHT = ResourceManager.getTextureFromAtlas("gui/drag_grip_highlight")
        val DIMENSION_DRAG_GRIP = ResourceManager.getTextureFromAtlas("gui/dimension_drag_grip")
        val SCROLL_BAR_TOP = ResourceManager.getTextureFromAtlas("gui/scroll_bar_top")
        val SCROLL_BAR_MIDDLE = ResourceManager.getTextureFromAtlas("gui/scroll_bar_middle")
        val SCROLL_BAR_BOTTOM = ResourceManager.getTextureFromAtlas("gui/scroll_bar_bottom")
        val SCROLL_BAR_UNHIGHLIGHT_TOP = ResourceManager.getTextureFromAtlas("gui/scroll_bar_unhighlight_top")
        val SCROLL_BAR_UNHIGHLIGHT_MIDDLE = ResourceManager.getTextureFromAtlas("gui/scroll_bar_unhighlight_mid")
        val SCROLL_BAR_UNHIGHLIGHT_BOTTOM = ResourceManager.getTextureFromAtlas("gui/scroll_bar_unhighlight_bottom")
        val SCROLL_BAR_HIGHLIGHT_TOP = ResourceManager.getTextureFromAtlas("gui/scroll_bar_highlight_top")
        val SCROLL_BAR_HIGHLIGHT_MIDDLE = ResourceManager.getTextureFromAtlas("gui/scroll_bar_highlight_mid")
        val SCROLL_BAR_HIGHLIGHT_BOTTOM = ResourceManager.getTextureFromAtlas("gui/scroll_bar_highlight_bottom")
        val SCROLL_BAR_CLICK_TOP = ResourceManager.getTextureFromAtlas("gui/scroll_bar_click_top")
        val SCROLL_BAR_CLICK_MIDDLE = ResourceManager.getTextureFromAtlas("gui/scroll_bar_click_mid")
        val SCROLL_BAR_CLICK_BOTTOM = ResourceManager.getTextureFromAtlas("gui/scroll_bar_click_bottom")
        val MAIN_MENU_LOGO = ResourceManager.getTextureFromAtlas("gui/main_menu_logo")
        val DEFAULT_EDGE_TOP = ResourceManager.getTextureFromAtlas("gui/default/top_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_EDGE_BOTTOM = ResourceManager.getTextureFromAtlas("gui/default/bottom_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_EDGE_RIGHT = ResourceManager.getTextureFromAtlas("gui/default/right_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_EDGE_LEFT = ResourceManager.getTextureFromAtlas("gui/default/left_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_BACKGROUND = ResourceManager.getTextureFromAtlas("gui/default/background").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val RESOURCE_DISPLAY_SLOT = ResourceManager.getTextureFromAtlas("gui/resource_display_slot")
        val ITEM_SLOT = ResourceManager.getTextureFromAtlas("gui/item_slot")
        val ITEM_SLOT_HIGHLIGHT = ResourceManager.getTextureFromAtlas("gui/item_slot_highlight")
        val ITEM_SLOT_DISPLAY = ResourceManager.getTextureFromAtlas("gui/item_slot_display")
        val ITEM_SLOT_CLICK = ResourceManager.getTextureFromAtlas("gui/item_slot_click")
        val MAIN_MENU_BACKGROUND = ResourceManager.getTextureFromAtlas("gui/main_menu_bg")
        val GREY_FILLER = ResourceManager.getTextureFromAtlas("gui/grey_filler")
        val WHITE_FILLER = ResourceManager.getTextureFromAtlas("gui/white_filler")
        val BLACK_FILLER = ResourceManager.getTextureFromAtlas("gui/black_filler")
        val HOTBAR_SELECTED_SLOT = ResourceManager.getTextureFromAtlas("gui/selected_slot")
        val CRAFTING_ARROW = ResourceManager.getTextureFromAtlas("gui/crafting_arrow")
    }

    object Fluid {
        val MOLTEN_COPPER = ResourceManager.getTextureFromAtlas("fluid/molten_copper")
        val MOLTEN_IRON = ResourceManager.getTextureFromAtlas("fluid/molten_iron")
    }

    object Particle {
        val BLOCK_PLACE = ResourceManager.getTextureFromAtlas("particle/block_place")
    }

    object Item {
        val CIRCUIT = ResourceManager.getTextureFromAtlas("item/circuit")
        val CABLE = ResourceManager.getTextureFromAtlas("item/cable")
        val TUBE = ResourceManager.getTextureFromAtlas("item/tube")
        val PIPE = ResourceManager.getTextureFromAtlas("item/pipe")
        val COPPER_ORE_ITEM = ResourceManager.getTextureFromAtlas("item/copper_ore_raw")
        val IRON_ORE_ITEM = ResourceManager.getTextureFromAtlas("item/iron_ore_raw")
        val IRON_INGOT = ResourceManager.getTextureFromAtlas("item/iron_ingot")
        val COPPER_INGOT = ResourceManager.getTextureFromAtlas("item/copper_ingot")
    }
}
