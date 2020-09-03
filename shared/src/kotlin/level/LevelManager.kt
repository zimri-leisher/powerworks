package level

import audio.AudioManager
import data.DirectoryChangeWatcher
import data.FileManager
import data.GameDirectoryIdentifier
import io.MouseMovementListener
import level.generator.LevelType
import main.Game
import main.GameState
import network.User
import resource.ResourceNode
import screen.CameraMovementListener
import screen.element.ElementLevelView
import screen.ScreenManager
import screen.mouse.Mouse
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.*

// TODO use mouse movement listener
// TODO Design choice: all listeners are held in their managers
// TODO events are pushed to the manager which pushes out the event

object LevelManager : DirectoryChangeWatcher, MouseMovementListener, CameraMovementListener, LevelEventListener {

    val allLevels = mutableSetOf<Level>()

    val loadedLevels = mutableSetOf<Level>()

    /**
     * The default [Level] for [LevelObject]s and [ResourceNode]s. They are never added to it and only store it after their
     * instantiation and before they get [added][Level.add] to a [Level]
     */
    val EMPTY_LEVEL = UnknownLevel(UUID.nameUUIDFromBytes(ByteArray(1) { 0 }))

    /**
     * The [Level] that the mouse is over, or null if the mouse is not over any [Level]
     */
    var levelUnderMouse: Level? = null

    /**
     * The [GUILevelView] that the mouse is currently over, or null if the mouse is not over any [GUILevelView]
     */
    var levelViewUnderMouse: ElementLevelView? = null
        private set

    /**
     * The [LevelObject] that the mouse is currently over, or null if the mouse is not over any [LevelObject]. If there are
     * multiple [LevelObject]s under the [Mouse], it will return [MovingObject]s first (in an arbitrary order) and [Block]s
     * last
     */
    var levelObjectUnderMouse: LevelObject? = null
        private set

    var mouseLevelX = 0
        private set(value) {
            if (field != value) {
                field = value
                mouseLevelXTile = value shr 4
                mouseLevelXChunk = value shr CHUNK_EXP
            }
        }

    var mouseLevelY = 0
        private set(value) {
            if (field != value) {
                field = value
                mouseLevelYTile = value shr 4
                mouseLevelYChunk = value shr CHUNK_EXP
            }
        }
    var mouseLevelXTile = 0
        private set
    var mouseLevelYTile = 0
        private set

    var mouseLevelXChunk = 0
        private set
    var mouseLevelYChunk = 0
        private set

    val mouseMovementListeners = mutableSetOf<MouseLevelMovementListener>()

    val levelEventListeners = mutableListOf<LevelEventListener>()

    init {
        if (Game.IS_SERVER) {
            FileManager.fileSystem.registerDirectoryChangeWatcher(this, FileManager.fileSystem.getPath(GameDirectoryIdentifier.SAVES))
        } else {
            Mouse.mouseMovementListeners.add(this)
        }
        levelEventListeners.add(this)
    }

    fun pushLevelStateEvent(level: Level, event: LevelEvent) {
        levelEventListeners.forEach { it.onLevelEvent(level, event) }
    }

    override fun onLevelEvent(level: Level, event: LevelEvent) {
        if(event == LevelEvent.LOAD) {
        } else if(event == LevelEvent.UNLOAD) {
            loadedLevels.remove(level)
        }
    }

    fun getLevelById(levelId: UUID) = allLevels.first { it.id == levelId }

    fun getLevelByIdOrNull(levelId: UUID) = allLevels.firstOrNull { it.id == levelId }

    fun isLevelInitialized(levelId: UUID) = allLevels.any { it.id == levelId }

    fun isLevelLoaded(levelId: UUID) = loadedLevels.any { it.id == levelId }

    override fun onMouseMove(prevX: Int, prevY: Int) {
        if (GameState.currentState == GameState.INGAME) {
            updateMouseLevelPosition()
        }
    }

    override fun onCameraMove(view: ElementLevelView, prevX: Int, prevY: Int) {
        updateMouseLevelPosition()
    }

    fun newLevelId() = UUID.randomUUID()

    fun newLevelInfoForFile(user: User): LevelInfo = LevelInfo(user, user.id.toString(), LocalDateTime.now().toString(), LevelType.DEFAULT_SIMPLEX, (Math.random() * 4096).toLong())

    fun doesLevelInfoExistFile(levelId: UUID, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.fileExists(dir, "$levelId/main.info")

    fun tryLoadLevelInfoFile(levelId: UUID, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.tryLoadObject(dir, "$levelId/main.info", LevelInfo::class.java)

    fun saveLevelInfoFile(levelId: UUID, info: LevelInfo, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.saveObject(dir, "$levelId/main.info", info)

    fun doesLevelDataFileExist(levelId: UUID, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.fileExists(dir, "$levelId/main.level")

    fun tryLoadLevelDataFile(levelId: UUID, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.tryLoadObject(dir, "$levelId/main.level", LevelData::class.java)

    fun saveLevelDataFile(levelId: UUID, data: LevelData, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.saveObject(dir, "$levelId/main.level", data)

    private fun updateLevelAndViewInformation() {
        val topViewUnderMouse = ScreenManager.elementsUnderMouse.filterIsInstance<ElementLevelView>().firstOrNull { it.open }
        // should be the highest level view under the mouse, even if it is hidden. null if none
        levelViewUnderMouse = topViewUnderMouse
        // the level under the mouse will be the level of this view. null if mouse not on level
        if (levelUnderMouse != levelViewUnderMouse?.level) {
            AudioManager.ears = levelViewUnderMouse?.camera
            updateMouseLevelPosition()
            levelUnderMouse = levelViewUnderMouse?.level
        }
        // the level object under the mouse will be the level object at the mouse's level position in the level under the mouse
        val levelObjectsUnderMouse = levelUnderMouse?.getLevelObjectsAt(mouseLevelX, mouseLevelY)
        val newLevelObjectUnderMouse = levelObjectsUnderMouse?.firstOrNull()
        if (newLevelObjectUnderMouse != levelObjectUnderMouse) {
            levelObjectUnderMouse = newLevelObjectUnderMouse
        }
    }

    private fun updateMouseLevelPosition() {
        if (levelViewUnderMouse != null) {
            val zoom = levelViewUnderMouse!!.zoomMultiplier
            val viewRectangle = levelViewUnderMouse!!.viewRectangle
            val prevX = mouseLevelX
            val prevY = mouseLevelY
            mouseLevelX = ((Mouse.x - levelViewUnderMouse!!.absoluteX) / zoom).toInt() + viewRectangle.x
            mouseLevelY = ((Mouse.y - levelViewUnderMouse!!.absoluteY) / zoom).toInt() + viewRectangle.y
            if (prevX != mouseLevelX || prevY != mouseLevelY) {
                mouseMovementListeners.forEach { it.onMouseMoveRelativeToLevel(prevX, prevY) }
            }
        }
    }

    fun update() {
        if (!Game.IS_SERVER) {
            updateLevelAndViewInformation()
        }
        loadedLevels.forEach { it.update() }
    }

    override fun onDirectoryChange(dir: Path) {
        if (dir == FileManager.fileSystem.getPath(GameDirectoryIdentifier.SAVES)) {
        }
    }

    fun saveLevels() {
        for (level in loadedLevels) {
            saveLevelInfoFile(level.id, level.info)
            saveLevelDataFile(level.id, level.data)
        }
    }
}