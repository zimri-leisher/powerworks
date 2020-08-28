package screen

import graphics.TextureRenderParams
import graphics.text.TextManager
import resource.ResourceType
import resource.ResourceTypeGroup
import screen.elements.*

object ResourceTypeSelector : GUIWindow("Resource type selector", 0, 0, (GUIResourceTypesDisplayList.OPTIONS_PER_ROW * (17)) + 8 + GUIVerticalScrollBar.WIDTH, 110) {

    private val background = GUIDefaultTextureRectangle(this, "Resource type selector background")

    private val searchBar = GUITextInputField(background, "Resource type selector search bar", { 2 }, { (background.heightPixels - TextManager.getFont().charHeight - 4).toInt() },
            (widthPixels / TextManager.getFont().charWidth).toInt() - 5, 1, "Search for a resource...", allowTextScrolling = true,
            onPressEnter = {
                this.selected = false
            }, onEnterText = { currentText, _ -> onSearchTextChange(currentText) }, onDeleteText = { currentText -> onSearchTextChange(currentText) }
    )

    private var possibleSingleTypes = ResourceType.ALL.filter { !it.hidden }.map { listOf(it) }
    private var possibleGroups = ResourceTypeGroup.values().map { it.types }

    private val singleTypesGroup: GUIResourceTypesDisplayList
    private val groupTypeGroup: GUIResourceTypesDisplayList

    private var searchingSingleTypes = true

    var possibleTypePredicate: ((ResourceType) -> Boolean)? = null

    private var selectedTypes: List<ResourceType>? = null

    init {
        openAtMouse = true
        val tabSelector = GUIDefaultTextureRectangle(background, "Resource type selector tab list background", { 2 }, { searchBar.alignments.y() - 3 - GUITabList.TAB_HEIGHT }).apply {
            val tablist = GUITabList(this, "Resource type selector tab list of single type VS group", { 1 }, { 1 },
                    arrayOf(Tab("single", "Single types"), Tab("group", "Groups of types")),
                    onSelectTab = { tabID ->
                        searchingSingleTypes = tabID == "single"
                        onChangeTab()
                    })
            this.alignments.width = { tablist.alignments.width() + 2 }
            this.alignments.height = { tablist.alignments.height() + 2 }
        }
        GUIDefaultTextureRectangle(this, "type selection list background", { 2 }, { 2 }, { GUIResourceTypesDisplayList.OPTIONS_PER_ROW * (17) + 3 }, { tabSelector.alignments.y() - 2 }).apply {
            localRenderParams = TextureRenderParams(rotation = 180f, brightness = 0.7f)
            singleTypesGroup = GUIResourceTypesDisplayList(this, "resource type selector single type list",
                    { 2 }, { heightPixels - 17 }, possibleSingleTypes, true, ::onSelect)
            singleTypesGroup.matchParentOpening = false
            groupTypeGroup = GUIResourceTypesDisplayList(this, "resource type selector type group list",
                    { 2 }, { heightPixels - 17 }, possibleGroups, true, ::onSelect)
            groupTypeGroup.matchParentOpening = false
        }

        generateCloseButton(layer = layer + 2)
        generateDragGrip(layer = layer + 2)
    }

    private fun onSelect(types: List<ResourceType>) {
        if (types.size == 1) {
            groupTypeGroup.selectedIcon = -1
        } else {
            singleTypesGroup.selectedIcon = -1
        }
        selectedTypes = types
    }

    fun onSearchTextChange(text: String) {
        val actualText = text.toLowerCase()
        val types = ResourceType.ALL.filter { possibleTypePredicate?.invoke(it) ?: true && !it.hidden && it.name.toLowerCase().startsWith(actualText) }.toMutableSet()
        types.addAll(ResourceType.ALL.filter { possibleTypePredicate?.invoke(it) ?: true && !it.hidden && it.name.toLowerCase().contains(actualText) })
        println(types)
        possibleSingleTypes = types.map { listOf(it) }
        val groups = ResourceTypeGroup.values().filter {
            it.displayName.toLowerCase().startsWith(actualText)
        }.map { it.types.filter { possibleTypePredicate?.invoke(it) ?: true } }.toMutableSet()
        groups.addAll(ResourceTypeGroup.values().filter {
            it.displayName.toLowerCase().contains(actualText)
        }.map { it.types.filter { possibleTypePredicate?.invoke(it) ?: true } })
        possibleGroups = groups.toList()
        singleTypesGroup.typesOrGroups = possibleSingleTypes
        groupTypeGroup.typesOrGroups = possibleGroups
    }

    private fun onChangeTab() {
        singleTypesGroup.open = searchingSingleTypes
        groupTypeGroup.open = !searchingSingleTypes
    }

    override fun onOpen() {
        if (searchingSingleTypes) {
            singleTypesGroup.open = true
        } else {
            groupTypeGroup.open = true
        }
        possibleSingleTypes = ResourceType.ALL.filter { !it.hidden && possibleTypePredicate?.invoke(it) ?: true }.map { listOf(it) }
        possibleGroups = ResourceTypeGroup.values().map { it.types }
        singleTypesGroup.typesOrGroups = possibleSingleTypes
        groupTypeGroup.typesOrGroups = possibleGroups
    }

    override fun onClose() {
        singleTypesGroup.selectedIcon = -1
        groupTypeGroup.selectedIcon = -1
    }

    fun getSelection(): List<ResourceType>? {
        if (selectedTypes != null) {
            val saved = selectedTypes
            selectedTypes = null
            return saved
        }
        return null
    }
}

private class GUIResourceTypesDisplayList(parent: RootGUIElement, name: String,
                                          xAlignment: Alignment, yAlignment: Alignment,
                                          types: List<List<ResourceType>>,
                                          allowSelection: Boolean = false,
                                          onSelect: (List<ResourceType>) -> Unit = {}) :
        GUIIconList(parent, name, xAlignment, yAlignment,
                OPTIONS_PER_ROW, 3,
                { _, _, _ -> }, types.size, allowSelection = allowSelection,
                getToolTip = {
                    val typeOrGroup = types[it]
                    if (typeOrGroup.size == 1) {
                        typeOrGroup[0].name
                    } else {
                        val group = ResourceTypeGroup.values().firstOrNull { typeOrGroup.all { type -> type in it.types } }
                        group?.displayName ?: typeOrGroup.joinToString { it.name }
                    }
                }),
        VerticalScrollable {

    var typesOrGroups = types
        set(value) {
            if (field != value) {
                field = value
                iconCount = field.size
            }
        }

    override val maxHeightPixels get() = (typesOrGroups.size / OPTIONS_PER_ROW)
    override val viewHeightPixels get() = heightPixels

    init {
        renderIcon = { xPixel, yPixel, index ->
            renderIconAt(xPixel, yPixel, this@GUIResourceTypesDisplayList.typesOrGroups[index])
        }
        onSelectIcon = {
            onSelect(this@GUIResourceTypesDisplayList.typesOrGroups[it])
        }
        alignments.width = { this.columns * (iconSize + ICON_PADDING) - ICON_PADDING + 3 + GUIVerticalScrollBar.WIDTH }
        GUIVerticalScrollBar(this, "resource type or group display list scroll bar", { widthPixels - GUIVerticalScrollBar.WIDTH }, { 2 }, { heightPixels })
    }

    fun renderIconAt(xPixel: Int, yPixel: Int, typeOrGroup: List<ResourceType>) {
        if (typeOrGroup.size == 1) {
            typeOrGroup[0].icon.render(xPixel, yPixel, iconSize, iconSize, true, localRenderParams)
        } else {
            val firstFour = typeOrGroup.subList(0, Integer.min(4, typeOrGroup.size))
            for ((index, innerType) in firstFour.withIndex()) {
                innerType.icon.render((index % 2) * 8 + xPixel, ((3 - index) / 2) * 8 + yPixel, iconSize / 2, iconSize / 2, true, localRenderParams)
            }
        }
    }

    override fun onScroll() {
    }

    companion object {
        const val OPTIONS_PER_ROW = 8
    }
}