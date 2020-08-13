package graphics

import com.badlogic.gdx.graphics.Texture
import data.GameResourceManager

object Image {

    init {
        Misc
        Block
        GUI
        Fluid
        Particle
        Item
    }

    object Misc {
        // TODO definitely rethink how weapon textures are done, apply this to other things in the future like block textures
        val ERROR = GameResourceManager.getAtlasTexture("misc/error")
        val ARROW = GameResourceManager.getAtlasTexture("misc/arrow")
        val TELEPORT_ICON = GameResourceManager.getAtlasTexture("misc/teleport_icon")
        val THIN_ARROW = GameResourceManager.getAtlasTexture("misc/thin_arrow")
        val BACK_ARROW = GameResourceManager.getAtlasTexture("misc/back_arrow")
    }

    object Block {
        val ARMORY = GameResourceManager.getAtlasTexture("block/armory")
        val CHEST_SMALL = GameResourceManager.getAtlasTexture("block/chest_small")
        val CHEST_LARGE = GameResourceManager.getAtlasTexture("block/chest_large")
        val CRAFTER = GameResourceManager.getAtlasTexture("block/crafter")
        val FURNACE = GameResourceManager.getAtlasTexture("block/furnace")
        val ROBOT_CRAFTER = GameResourceManager.getAtlasTexture("block/robot_crafter")
        val TUBE_4_WAY = GameResourceManager.getAtlasTexture("block/tube/4_way")
        val TUBE_2_WAY_VERTICAL = GameResourceManager.getAtlasTexture("block/tube/2_way_vertical")
        val TUBE_2_WAY_HORIZONTAL = GameResourceManager.getAtlasTexture("block/tube/2_way_horizontal")
        val TUBE_UP_CLOSE = GameResourceManager.getAtlasTexture("block/tube/up_close")
        val TUBE_RIGHT_CLOSE = GameResourceManager.getAtlasTexture("block/tube/right_close")
        val TUBE_DOWN_CLOSE = GameResourceManager.getAtlasTexture("block/tube/down_close")
        val TUBE_LEFT_CLOSE = GameResourceManager.getAtlasTexture("block/tube/left_close")
        val TUBE_UP_CONNECT = GameResourceManager.getAtlasTexture("block/tube/up_connect")
        val PIPE_UP_CONNECT = GameResourceManager.getAtlasTexture("block/pipe/up_connect")
        val PIPE_4_WAY = GameResourceManager.getAtlasTexture("block/pipe/4_way")
        val PIPE_2_WAY_VERTICAL = GameResourceManager.getAtlasTexture("block/pipe/2_way_vertical")
        val PIPE_2_WAY_HORIZONTAL = GameResourceManager.getAtlasTexture("block/pipe/2_way_horizontal")
        val PIPE_UP_CLOSE = GameResourceManager.getAtlasTexture("block/pipe/up_close")
        val PIPE_RIGHT_CLOSE = GameResourceManager.getAtlasTexture("block/pipe/right_close")
        val PIPE_DOWN_CLOSE = GameResourceManager.getAtlasTexture("block/pipe/down_close")
        val PIPE_LEFT_CLOSE = GameResourceManager.getAtlasTexture("block/pipe/left_close")
    }

    object GUI {
        val SELECT_ENTITIES = GameResourceManager.getAtlasTexture("gui/select_entities")
        val MINUS = GameResourceManager.getAtlasTexture("gui/minus")
        val PLUS = GameResourceManager.getAtlasTexture("gui/plus")
        val VIEW_SELECTOR_CLOSE_BUTTON = GameResourceManager.getAtlasTexture("gui/view_selector_close")
        val VIEW_SELECTOR_CLOSE_BUTTON_HIGHLIGHT = GameResourceManager.getAtlasTexture("gui/view_selector_close_highlight")
        val VIEW_SELECTOR_OPEN_BUTTON = GameResourceManager.getAtlasTexture("gui/view_selector_open")
        val VIEW_SELECTOR_OPEN_BUTTON_HIGHLIGHT = GameResourceManager.getAtlasTexture("gui/view_selector_open_highlight")
        val CLOSE_BUTTON = GameResourceManager.getAtlasTexture("gui/close_button")
        val CLOSE_BUTTON_HIGHLIGHT = GameResourceManager.getAtlasTexture("gui/close_button_highlight")
        val DRAG_GRIP = GameResourceManager.getAtlasTexture("gui/drag_grip")
        val DRAG_GRIP_HIGHLIGHT = GameResourceManager.getAtlasTexture("gui/drag_grip_highlight")
        val DIMENSION_DRAG_GRIP = GameResourceManager.getAtlasTexture("gui/dimension_drag_grip")
        val SCROLL_BAR_TOP = GameResourceManager.getAtlasTexture("gui/scroll_bar_top")
        val SCROLL_BAR_MIDDLE = GameResourceManager.getAtlasTexture("gui/scroll_bar_middle")
        val SCROLL_BAR_BOTTOM = GameResourceManager.getAtlasTexture("gui/scroll_bar_bottom")
        val SCROLL_BAR_UNHIGHLIGHT_TOP = GameResourceManager.getAtlasTexture("gui/scroll_bar_unhighlight_top")
        val SCROLL_BAR_UNHIGHLIGHT_MIDDLE = GameResourceManager.getAtlasTexture("gui/scroll_bar_unhighlight_mid")
        val SCROLL_BAR_UNHIGHLIGHT_BOTTOM = GameResourceManager.getAtlasTexture("gui/scroll_bar_unhighlight_bottom")
        val SCROLL_BAR_HIGHLIGHT_TOP = GameResourceManager.getAtlasTexture("gui/scroll_bar_highlight_top")
        val SCROLL_BAR_HIGHLIGHT_MIDDLE = GameResourceManager.getAtlasTexture("gui/scroll_bar_highlight_mid")
        val SCROLL_BAR_HIGHLIGHT_BOTTOM = GameResourceManager.getAtlasTexture("gui/scroll_bar_highlight_bottom")
        val SCROLL_BAR_CLICK_TOP = GameResourceManager.getAtlasTexture("gui/scroll_bar_click_top")
        val SCROLL_BAR_CLICK_MIDDLE = GameResourceManager.getAtlasTexture("gui/scroll_bar_click_mid")
        val SCROLL_BAR_CLICK_BOTTOM = GameResourceManager.getAtlasTexture("gui/scroll_bar_click_bottom")
        val MAIN_MENU_LOGO = GameResourceManager.getAtlasTexture("gui/main_menu_logo")
        val DEFAULT_EDGE_TOP = GameResourceManager.getAtlasTexture("gui/default/top_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_EDGE_BOTTOM = GameResourceManager.getAtlasTexture("gui/default/bottom_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_EDGE_RIGHT = GameResourceManager.getAtlasTexture("gui/default/right_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_EDGE_LEFT = GameResourceManager.getAtlasTexture("gui/default/left_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_BACKGROUND = GameResourceManager.getAtlasTexture("gui/default/background").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val RESOURCE_DISPLAY_SLOT = GameResourceManager.getAtlasTexture("gui/resource_display_slot")
        val ITEM_SLOT = GameResourceManager.getAtlasTexture("gui/item_slot")
        val ITEM_SLOT_HIGHLIGHT = GameResourceManager.getAtlasTexture("gui/item_slot_highlight")
        val ITEM_SLOT_DISPLAY = GameResourceManager.getAtlasTexture("gui/item_slot_display")
        val ITEM_SLOT_CLICK = GameResourceManager.getAtlasTexture("gui/item_slot_click")
        val MAIN_MENU_BACKGROUND = GameResourceManager.getAtlasTexture("gui/main_menu_bg")
        val GREY_FILLER = GameResourceManager.getAtlasTexture("gui/grey_filler")
        val WHITE_FILLER = GameResourceManager.getAtlasTexture("gui/white_filler")
        val BLACK_FILLER = GameResourceManager.getAtlasTexture("gui/black_filler")
        val HOTBAR_SELECTED_SLOT = GameResourceManager.getAtlasTexture("gui/selected_slot")
        val WARNING_STRIPES = GameResourceManager.getAtlasTexture("gui/warning_stripes")
        val LEVEL_SELECTOR_BUTTON_HIGHLIGHT = GameResourceManager.getAtlasTexture("gui/level_selector_button_highlight")
        val CRAFTING_ARROW = GameResourceManager.getAtlasTexture("gui/crafting_arrow")
        val ENTITY_CONTROLLER_MENU = GameResourceManager.getAtlasTexture("gui/entity_controller")
        val ENTITY_CONTROLLER_MENU_STOP_SELECTED = GameResourceManager.getAtlasTexture("gui/entity_controller_stop_selected")
        val ENTITY_CONTROLLER_MENU_DEFEND_SELECTED = GameResourceManager.getAtlasTexture("gui/entity_controller_defend_selected")
        val ENTITY_CONTROLLER_MENU_ATTACK_SELECTED = GameResourceManager.getAtlasTexture("gui/entity_controller_attack_selected")
        val ENTITY_CONTROLLER_MENU_MOVE_SELECTED = GameResourceManager.getAtlasTexture("gui/entity_controller_move_selected")
    }

    object Fluid {
        val MOLTEN_COPPER = GameResourceManager.getAtlasTexture("fluid/molten_copper")
        val MOLTEN_IRON = GameResourceManager.getAtlasTexture("fluid/molten_iron")
    }

    object Particle {
        val BLOCK_PLACE = GameResourceManager.getAtlasTexture("particle/block_place")
    }

    object Item {
        val CIRCUIT = GameResourceManager.getAtlasTexture("item/circuit")
        val CABLE = GameResourceManager.getAtlasTexture("item/cable")
        val TUBE = GameResourceManager.getAtlasTexture("item/tube")
        val PIPE = GameResourceManager.getAtlasTexture("item/pipe")
        val COPPER_ORE_ITEM = GameResourceManager.getAtlasTexture("item/copper_ore_raw")
        val IRON_ORE_ITEM = GameResourceManager.getAtlasTexture("item/iron_ore_raw")
        val IRON_INGOT = GameResourceManager.getAtlasTexture("item/iron_ingot")
        val COPPER_INGOT = GameResourceManager.getAtlasTexture("item/copper_ingot")
    }
}
