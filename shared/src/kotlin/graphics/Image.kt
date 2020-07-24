package graphics

import com.badlogic.gdx.graphics.Texture
import data.ResourceManager

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
        val ERROR = ResourceManager.getAtlasTexture("misc/error")
        val ARROW = ResourceManager.getAtlasTexture("misc/arrow")
        val TELEPORT_ICON = ResourceManager.getAtlasTexture("misc/teleport_icon")
        val THIN_ARROW = ResourceManager.getAtlasTexture("misc/thin_arrow")
        val BACK_ARROW = ResourceManager.getAtlasTexture("misc/back_arrow")
    }

    object Block {
        val ARMORY = ResourceManager.getAtlasTexture("block/armory")
        val CHEST_SMALL = ResourceManager.getAtlasTexture("block/chest_small")
        val CHEST_LARGE = ResourceManager.getAtlasTexture("block/chest_large")
        val CRAFTER = ResourceManager.getAtlasTexture("block/crafter")
        val FURNACE = ResourceManager.getAtlasTexture("block/furnace")
        val ROBOT_CRAFTER = ResourceManager.getAtlasTexture("block/robot_crafter")
        val TUBE_4_WAY = ResourceManager.getAtlasTexture("block/tube/4_way")
        val TUBE_2_WAY_VERTICAL = ResourceManager.getAtlasTexture("block/tube/2_way_vertical")
        val TUBE_2_WAY_HORIZONTAL = ResourceManager.getAtlasTexture("block/tube/2_way_horizontal")
        val TUBE_UP_CLOSE = ResourceManager.getAtlasTexture("block/tube/up_close")
        val TUBE_RIGHT_CLOSE = ResourceManager.getAtlasTexture("block/tube/right_close")
        val TUBE_DOWN_CLOSE = ResourceManager.getAtlasTexture("block/tube/down_close")
        val TUBE_LEFT_CLOSE = ResourceManager.getAtlasTexture("block/tube/left_close")
        val TUBE_UP_CONNECT = ResourceManager.getAtlasTexture("block/tube/up_connect")
        val PIPE_UP_CONNECT = ResourceManager.getAtlasTexture("block/pipe/up_connect")
        val PIPE_4_WAY = ResourceManager.getAtlasTexture("block/pipe/4_way")
        val PIPE_2_WAY_VERTICAL = ResourceManager.getAtlasTexture("block/pipe/2_way_vertical")
        val PIPE_2_WAY_HORIZONTAL = ResourceManager.getAtlasTexture("block/pipe/2_way_horizontal")
        val PIPE_UP_CLOSE = ResourceManager.getAtlasTexture("block/pipe/up_close")
        val PIPE_RIGHT_CLOSE = ResourceManager.getAtlasTexture("block/pipe/right_close")
        val PIPE_DOWN_CLOSE = ResourceManager.getAtlasTexture("block/pipe/down_close")
        val PIPE_LEFT_CLOSE = ResourceManager.getAtlasTexture("block/pipe/left_close")
    }

    object GUI {
        val SELECT_ENTITIES = ResourceManager.getAtlasTexture("gui/select_entities")
        val MINUS = ResourceManager.getAtlasTexture("gui/minus")
        val PLUS = ResourceManager.getAtlasTexture("gui/plus")
        val VIEW_SELECTOR_CLOSE_BUTTON = ResourceManager.getAtlasTexture("gui/view_selector_close")
        val VIEW_SELECTOR_CLOSE_BUTTON_HIGHLIGHT = ResourceManager.getAtlasTexture("gui/view_selector_close_highlight")
        val VIEW_SELECTOR_OPEN_BUTTON = ResourceManager.getAtlasTexture("gui/view_selector_open")
        val VIEW_SELECTOR_OPEN_BUTTON_HIGHLIGHT = ResourceManager.getAtlasTexture("gui/view_selector_open_highlight")
        val CLOSE_BUTTON = ResourceManager.getAtlasTexture("gui/close_button")
        val CLOSE_BUTTON_HIGHLIGHT = ResourceManager.getAtlasTexture("gui/close_button_highlight")
        val DRAG_GRIP = ResourceManager.getAtlasTexture("gui/drag_grip")
        val DRAG_GRIP_HIGHLIGHT = ResourceManager.getAtlasTexture("gui/drag_grip_highlight")
        val DIMENSION_DRAG_GRIP = ResourceManager.getAtlasTexture("gui/dimension_drag_grip")
        val SCROLL_BAR_TOP = ResourceManager.getAtlasTexture("gui/scroll_bar_top")
        val SCROLL_BAR_MIDDLE = ResourceManager.getAtlasTexture("gui/scroll_bar_middle")
        val SCROLL_BAR_BOTTOM = ResourceManager.getAtlasTexture("gui/scroll_bar_bottom")
        val SCROLL_BAR_UNHIGHLIGHT_TOP = ResourceManager.getAtlasTexture("gui/scroll_bar_unhighlight_top")
        val SCROLL_BAR_UNHIGHLIGHT_MIDDLE = ResourceManager.getAtlasTexture("gui/scroll_bar_unhighlight_mid")
        val SCROLL_BAR_UNHIGHLIGHT_BOTTOM = ResourceManager.getAtlasTexture("gui/scroll_bar_unhighlight_bottom")
        val SCROLL_BAR_HIGHLIGHT_TOP = ResourceManager.getAtlasTexture("gui/scroll_bar_highlight_top")
        val SCROLL_BAR_HIGHLIGHT_MIDDLE = ResourceManager.getAtlasTexture("gui/scroll_bar_highlight_mid")
        val SCROLL_BAR_HIGHLIGHT_BOTTOM = ResourceManager.getAtlasTexture("gui/scroll_bar_highlight_bottom")
        val SCROLL_BAR_CLICK_TOP = ResourceManager.getAtlasTexture("gui/scroll_bar_click_top")
        val SCROLL_BAR_CLICK_MIDDLE = ResourceManager.getAtlasTexture("gui/scroll_bar_click_mid")
        val SCROLL_BAR_CLICK_BOTTOM = ResourceManager.getAtlasTexture("gui/scroll_bar_click_bottom")
        val MAIN_MENU_LOGO = ResourceManager.getAtlasTexture("gui/main_menu_logo")
        val DEFAULT_EDGE_TOP = ResourceManager.getAtlasTexture("gui/default/top_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_EDGE_BOTTOM = ResourceManager.getAtlasTexture("gui/default/bottom_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_EDGE_RIGHT = ResourceManager.getAtlasTexture("gui/default/right_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_EDGE_LEFT = ResourceManager.getAtlasTexture("gui/default/left_edge").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val DEFAULT_BACKGROUND = ResourceManager.getAtlasTexture("gui/default/background").apply { texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        val RESOURCE_DISPLAY_SLOT = ResourceManager.getAtlasTexture("gui/resource_display_slot")
        val ITEM_SLOT = ResourceManager.getAtlasTexture("gui/item_slot")
        val ITEM_SLOT_HIGHLIGHT = ResourceManager.getAtlasTexture("gui/item_slot_highlight")
        val ITEM_SLOT_DISPLAY = ResourceManager.getAtlasTexture("gui/item_slot_display")
        val ITEM_SLOT_CLICK = ResourceManager.getAtlasTexture("gui/item_slot_click")
        val MAIN_MENU_BACKGROUND = ResourceManager.getAtlasTexture("gui/main_menu_bg")
        val GREY_FILLER = ResourceManager.getAtlasTexture("gui/grey_filler")
        val WHITE_FILLER = ResourceManager.getAtlasTexture("gui/white_filler")
        val BLACK_FILLER = ResourceManager.getAtlasTexture("gui/black_filler")
        val HOTBAR_SELECTED_SLOT = ResourceManager.getAtlasTexture("gui/selected_slot")
        val WARNING_STRIPES = ResourceManager.getAtlasTexture("gui/warning_stripes")
        val LEVEL_SELECTOR_BUTTON_HIGHLIGHT = ResourceManager.getAtlasTexture("gui/level_selector_button_highlight")
        val CRAFTING_ARROW = ResourceManager.getAtlasTexture("gui/crafting_arrow")
        val ENTITY_CONTROLLER_MENU = ResourceManager.getAtlasTexture("gui/entity_controller")
        val ENTITY_CONTROLLER_MENU_STOP_SELECTED = ResourceManager.getAtlasTexture("gui/entity_controller_stop_selected")
        val ENTITY_CONTROLLER_MENU_DEFEND_SELECTED = ResourceManager.getAtlasTexture("gui/entity_controller_defend_selected")
        val ENTITY_CONTROLLER_MENU_ATTACK_SELECTED = ResourceManager.getAtlasTexture("gui/entity_controller_attack_selected")
        val ENTITY_CONTROLLER_MENU_MOVE_SELECTED = ResourceManager.getAtlasTexture("gui/entity_controller_move_selected")
    }

    object Fluid {
        val MOLTEN_COPPER = ResourceManager.getAtlasTexture("fluid/molten_copper")
        val MOLTEN_IRON = ResourceManager.getAtlasTexture("fluid/molten_iron")
    }

    object Particle {
        val BLOCK_PLACE = ResourceManager.getAtlasTexture("particle/block_place")
    }

    object Item {
        val CIRCUIT = ResourceManager.getAtlasTexture("item/circuit")
        val CABLE = ResourceManager.getAtlasTexture("item/cable")
        val TUBE = ResourceManager.getAtlasTexture("item/tube")
        val PIPE = ResourceManager.getAtlasTexture("item/pipe")
        val COPPER_ORE_ITEM = ResourceManager.getAtlasTexture("item/copper_ore_raw")
        val IRON_ORE_ITEM = ResourceManager.getAtlasTexture("item/iron_ore_raw")
        val IRON_INGOT = ResourceManager.getAtlasTexture("item/iron_ingot")
        val COPPER_INGOT = ResourceManager.getAtlasTexture("item/copper_ingot")
    }
}
