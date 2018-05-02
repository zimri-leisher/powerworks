package screen

import graphics.Image
import graphics.Utils
import level.LevelInfo
import main.Game
import screen.elements.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object LevelSelectorGUI : GUIWindow("Level selector window", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, windowGroup = ScreenManager.Groups.BACKGROUND) {

    lateinit var infoGroup: AutoFormatGUIGroup

    val levelInfos = mutableListOf<LevelInfo>()

    class GUILevelInfoDisplay(val levelName: String) : GUIElement(infoGroup, "level info for level $levelName", 0, 0, WIDTH, HEIGHT) {

        init {
            GUITexturePane(this, name + " background", 0, 0, Image(Utils.genRectangle(widthPixels, heightPixels))).run {
                GUIText(this, this@GUILevelInfoDisplay.name + " level name text", 6, 4, levelName)

            }
        }

        companion object {
            val WIDTH = GUIButton.WIDTH
            val HEIGHT = GUIButton.HEIGHT + 8
        }
    }

    init {
        adjustDimensions = true
        indexLevels()
        GUITexturePane(this.rootChild, "background texture", { 0 }, { 0 }, Image.GUI.MAIN_MENU_BACKGROUND_FILLER, { widthPixels }, { heightPixels }).run {
            GUIButton(this, "main menu return button", 4, 4, "Return to main menu", onRelease = {
                this@LevelSelectorGUI.open = false
                MainMenuGUI.open = true
            })
            infoGroup
        }
    }

    fun indexLevels() {
        val directory = Paths.get(Game.ENCLOSING_FOLDER_PATH, "/data/save")
        if (Files.notExists(directory))
            Files.createDirectory(directory)
        val calInstance = Calendar.getInstance()
        val fileName = "${Game.ENCLOSING_FOLDER_PATH}/screenshots/${calInstance.get(Calendar.MONTH) + 1}-${calInstance.get(Calendar.DATE)}-${calInstance.get(Calendar.YEAR)}"
        val allFiles = Files.walk(directory).filter { Files.isRegularFile(it) }.map { it.toFile() }.toArray() as Array<File>
        val levelFileInfoFilePairs = mutableMapOf<File, File>()
        for (file in allFiles) {
            if (file.name.endsWith(".level"))
                if (file !in levelFileInfoFilePairs) {
                    levelFileInfoFilePairs.put(file, allFiles.first { it.name.removeSuffix(".info") == file.name.removeSuffix(".level") })
                }
        }
        for((level, info) in levelFileInfoFilePairs) {
            levelInfos.add(LevelInfo.parse(info.readLines(), level, info))
        }
    }
}