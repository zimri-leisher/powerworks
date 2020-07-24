package level

import audio.AudioManager
import crafting.Recipe
import data.DirectoryChangeWatcher
import data.FileManager
import data.GameDirectoryIdentifier
import data.WeakMutableList
import io.ControlPressHandler
import io.InputManager
import io.MouseMovementListener
import level.block.CrafterBlock
import level.entity.robot.Robot
import level.entity.robot.RobotType
import level.generator.LevelType
import main.Game
import main.State
import network.User
import resource.ResourceNode
import screen.CameraMovementListener
import screen.ScreenManager
import screen.elements.GUILevelView
import screen.mouse.Mouse
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.*

object LevelManager : DirectoryChangeWatcher, MouseMovementListener, CameraMovementListener {

    val allLevels = mutableSetOf<Level>()

    private val _loadedLevels = mutableSetOf<Level>()
    val loadedLevels = object : MutableSet<Level> by _loadedLevels {
        override fun add(element: Level): Boolean {
            val result = _loadedLevels.add(element)
            if(result) {
                onLevelLoad(element)
            }
            return result
        }
    }

    /**
     * The default [Level] for [LevelObject]s and [ResourceNode]s. They are never added to it and only store it after their
     * instantiation and before they get [added][Level.add] to a [Level]
     */
    val EMPTY_LEVEL = ActualLevel(UUID.nameUUIDFromBytes(ByteArray(2, { 0 })), LevelInfo(User(UUID.nameUUIDFromBytes(ByteArray(3, { 0 })), ""), "EMPTY_LEVEL", "-1", LevelType.EMPTY, 0))

    val EMPTY_CHUNK = Chunk(0, 0)

    /**
     * The [Level] that the mouse is over, or null if the mouse is not over any [Level]
     */
    var levelUnderMouse: Level? = null

    /**
     * The last [Level] that was clicked on. Before any have been clicked on, defaults to the [localLevel]
     */
    lateinit var levelLastInteractedWith: Level

    private val allViews = WeakMutableList<GUILevelView>()

    /**
     * The [GUILevelView] that the mouse is currently over, or null if the mouse is not over any [GUILevelView]
     */
    var levelViewUnderMouse: GUILevelView? = null
        private set

    /**
     * The last [GUILevelView] that was clicked on. Before any have been clicked on, defaults to the first [GUILevelView]
     * created
     */
    lateinit var levelViewLastInteractedWith: GUILevelView
        private set
    /**
     * The [LevelObject] that the mouse is currently over, or null if the mouse is not over any [LevelObject]. If there are
     * multiple [LevelObject]s under the [Mouse], it will return [MovingObject]s first (in an undefined order) and [Block]s
     * last. However, this is a pretty rare case
     */
    var levelObjectUnderMouse: LevelObject? = null
        private set

    /**
     * The last [LevelObject] that was clicked on. Before any have been clicked on, defaults to the camera of the first [GUILevelView]
     */
    lateinit var levelObjectLastInteractedWith: LevelObject
        private set

    var mouseLevelXPixel = 0
        private set(value) {
            if (field != value) {
                field = value
                mouseLevelXTile = value shr 4
                mouseLevelXChunk = value shr CHUNK_PIXEL_EXP
            }
        }

    var mouseLevelYPixel = 0
        private set(value) {
            if (field != value) {
                field = value
                mouseLevelYTile = value shr 4
                mouseLevelYChunk = value shr CHUNK_PIXEL_EXP
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

    init {
        if (Game.IS_SERVER) {
            FileManager.fileSystem.registerDirectoryChangeWatcher(this, FileManager.fileSystem.getPath(GameDirectoryIdentifier.SAVES))
        } else {
            InputManager.mouseMovementListeners.add(this)
        }
    }

    fun addLevelView(view: GUILevelView) {
        if (allViews.size == 0) {
            levelViewLastInteractedWith = view
        }
        view.moveListeners.add(this)
        allViews.add(view)
    }

    private fun onLevelLoad(level: Level) {
    }

    override fun onMouseMove(pXPixel: Int, pYPixel: Int) {
        if (State.CURRENT_STATE == State.INGAME) {
            updateMouseLevelPosition()
        }
    }

    override fun onCameraMove(view: GUILevelView, pXPixel: Int, pYPixel: Int) {
        updateMouseLevelPosition()
    }

    fun newLevelId() = UUID.randomUUID()

    fun newLevelInfoFor(user: User): LevelInfo = LevelInfo(user, user.id.toString(), LocalDateTime.now().toString(), LevelType.DEFAULT_SIMPLEX, (Math.random() * 4096).toLong())

    fun doesLevelInfoExist(levelId: UUID, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.fileExists(dir, "$levelId/main.info")

    fun tryLoadLevelInfo(levelId: UUID, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.tryLoadObject(dir, "$levelId/main.info", LevelInfo::class.java)

    fun saveLevelInfo(levelId: UUID, info: LevelInfo, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.saveObject(dir, "$levelId/main.info", info)

    fun doesLevelDataExist(levelId: UUID, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.fileExists(dir, "$levelId/main.level")

    fun tryLoadLevelData(levelId: UUID, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.tryLoadObject(dir, "$levelId/main.level", LevelData::class.java)

    fun saveLevelData(levelId: UUID, data: LevelData, dir: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = FileManager.saveObject(dir, "$levelId/main.level", data)

    /**
     * Updates the audio engine so you hear sound from last the selected view
     */
    private fun updateAudioEars() {
        AudioManager.ears = levelViewLastInteractedWith.camera
    }

    fun updateLevelAndViewInformation() {
        val topViewUnderMouse = allViews.filter { it.open }.sortedByDescending { it.parent.layer }.firstOrNull { it.mouseOn }
        // should be the highest level view under the mouse, even if it is hidden. null if none
        levelViewUnderMouse = topViewUnderMouse
        // the level under the mouse will be the level of this view. null if mouse not on level
        if (levelUnderMouse != levelViewUnderMouse?.level) {
            updateAudioEars()
            updateMouseLevelPosition()
            levelUnderMouse = levelViewUnderMouse?.level
        }
        // the level object under the mouse will be the level object at the mouse's level position in the level under the mouse
        val levelObjectsUnderMouse = levelUnderMouse?.getLevelObjectsAt(mouseLevelXPixel, mouseLevelYPixel)
        val newLevelObjectUnderMouse = levelObjectsUnderMouse?.firstOrNull()
        if (newLevelObjectUnderMouse != levelObjectUnderMouse) {
            levelObjectUnderMouse?.mouseOn = false
            newLevelObjectUnderMouse?.mouseOn = true
            levelObjectUnderMouse = newLevelObjectUnderMouse
        }
        // if the last element that was clicked on is a gui level view and it has changed
        if (ScreenManager.elementLastInteractedWith is GUILevelView && ScreenManager.elementLastInteractedWith != levelViewLastInteractedWith) {
            // update the last level view
            levelViewLastInteractedWith = ScreenManager.elementLastInteractedWith as GUILevelView
            // the last level interacted with will be the level of the last view interacted with
            levelLastInteractedWith = levelViewLastInteractedWith.level
        }
    }

    fun onInteractWithLevelObjectUnderMouse() {
        levelObjectLastInteractedWith = levelObjectUnderMouse!!
        InputManager.currentLevelHandlers.clear()
        if (levelObjectLastInteractedWith is ControlPressHandler) {
            InputManager.currentLevelHandlers.add(levelObjectLastInteractedWith as ControlPressHandler)
        }
    }

    fun updateMouseLevelPosition() {
        if (levelViewUnderMouse != null) {
            val zoom = levelViewUnderMouse!!.zoomMultiplier
            val viewRectangle = levelViewUnderMouse!!.viewRectangle
            val pXPixel = mouseLevelXPixel
            val pYPixel = mouseLevelYPixel
            mouseLevelXPixel = ((Mouse.xPixel - levelViewUnderMouse!!.xPixel) / zoom).toInt() + viewRectangle.x
            mouseLevelYPixel = ((Mouse.yPixel - levelViewUnderMouse!!.yPixel) / zoom).toInt() + viewRectangle.y
            if (pXPixel != mouseLevelXPixel || pYPixel != mouseLevelYPixel) {
                mouseMovementListeners.forEach { it.onMouseMoveRelativeToLevel(pXPixel, pYPixel) }
            }
        }
    }

    private fun updateChunksBeingRendered() {
        // todo redo this shit
        allViews.forEach {
            if (it.open) {
                val chunksInView = it.level.getChunksFromPixelRectangle(it.viewRectangle.x, it.viewRectangle.y, it.viewRectangle.width, it.viewRectangle.height)
                for (chunk in it.level.data.chunks) {
                    chunk.beingRendered = chunk in chunksInView
                }
            }
        }
    }

    fun update() {
        if (!Game.IS_SERVER) {
            updateLevelAndViewInformation()
            updateChunksBeingRendered()
        }
        loadedLevels.forEach { it.update() }
    }

    override fun onDirectoryChange(dir: Path) {
        if (dir == FileManager.fileSystem.getPath(GameDirectoryIdentifier.SAVES)) {
        }
    }

    fun onCrafterRecipeChange(block: CrafterBlock, newRecipe: Recipe?) {

    }

    fun saveLevels() {
        for (level in loadedLevels) {
            saveLevelInfo(level.id, level.info)
            saveLevelData(level.id, level.data)
        }
    }
}