package level

import audio.AudioManager
import com.esotericsoftware.kryo.io.Input
import data.DirectoryChangeWatcher
import data.FileManager
import data.GameDirectoryIdentifier
import data.WeakMutableList
import io.ControlPressHandler
import io.InputManager
import io.MouseMovementListener
import main.Game
import main.State
import network.Client
import network.Server
import network.User
import network.packet.Packet
import network.packet.PacketHandler
import network.packet.PacketType
import resource.ResourceNode
import screen.CameraMovementListener
import screen.ScreenManager
import screen.elements.GUILevelView
import screen.mouse.Mouse
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.streams.toList

object LevelManager : DirectoryChangeWatcher, MouseMovementListener, CameraMovementListener, PacketHandler {
    val allLevels = mutableSetOf<Level>()

    /**
     * The level that the local [Player] has loaded
     */
    lateinit var localLevel: Level
        private set

    /**
     * The [Level] that the mouse is over, or null if the mouse is not over any [Level]
     */
    var levelUnderMouse: Level? = null
    /**
     * The last [Level] that was clicked on. Before any have been clicked on, defaults to the [localLevel]
     */
    lateinit var levelLastInteractedWith: Level
    /**
     * The default [Level] for [LevelObject]s and [ResourceNode]s. They are never added to it and only store it after their
     * instantiation and before they get [added][Level.add] to a [Level]
     */
    val emptyLevel = object : Level(LevelInfo("", "emptyLevel", "-1", LevelGeneratorSettings(0, 0), 0)) {
        override fun genTiles(xChunk: Int, yChunk: Int) = throw UnsupportedOperationException("Empty level cannot generate tiles")

        override fun genBlocks(xChunk: Int, yChunk: Int) = throw UnsupportedOperationException("Empty level cannot generate blocks")
    }

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
     * last. However, this is a pretty rare case,
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
    val levelInfos = mutableListOf<LevelInfo>()

    init {
        InputManager.mouseMovementListeners.add(this)
        if (Game.IS_SERVER) {
            Server.registerClientPacketHandler(this, PacketType.REQUEST_LEVEL_INFO)
            FileManager.fileSystem.registerDirectoryChangeWatcher(this, FileManager.fileSystem.getPath(GameDirectoryIdentifier.SERVER_SAVES))
        } else {
            println("requesting level info")
            Client.registerServerPacketHandler(this, PacketType.LEVEL_INFO)
            //Client.sendToServer(RequestLevelInfoPacket())
            FileManager.fileSystem.registerDirectoryChangeWatcher(this, FileManager.fileSystem.getPath(GameDirectoryIdentifier.SAVES))
        }
    }

    fun setLocalLevel(level: Level) {
        localLevel = level
        levelLastInteractedWith = level
    }

    fun addLevelView(view: GUILevelView) {
        if (allViews.size == 0) {
            levelViewLastInteractedWith = view
        }
        view.moveListeners.add(this)
        allViews.add(view)
    }

    override fun handleServerPacket(packet: Packet) {
//        if (packet is LevelInfoPacket) {
//            levelInfos.add(packet.info)
//            Client.sendToServer(RequestLevelDataPacket())
//        }
    }

    override fun handleClientPacket(packet: Packet) {
//        if (packet is RequestLevelInfoPacket) {
//            val correspondingLevelInfo = getLevelInfoFor(User(packet.userId))
//            Server.sendToClient(LevelInfoPacket(correspondingLevelInfo), packet.clientId)
//            println("sending level info to clients")
//        }
    }

    override fun onMouseMove(pXPixel: Int, pYPixel: Int) {
        if (State.CURRENT_STATE == State.INGAME) {
            updateMouseLevelPosition()
        }
    }

    override fun onCameraMove(view: GUILevelView, pXPixel: Int, pYPixel: Int) {
        updateMouseLevelPosition()
    }

    fun getLevelInfoFor(user: User) = levelInfos.firstOrNull { it.userId == user.id } ?: createNewLevelInfoFor(user)

    private fun createNewLevelInfoFor(user: User, name: String = user.id): LevelInfo {
        val info = LevelInfo(user.id, name, LocalDateTime.now().toString(), LevelGeneratorSettings(), (Math.random() * 4096).toLong())
        levelInfos.add(info)
        return info
    }

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

    fun onInteractWithLevelObject() {
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
                for (chunk in it.level.chunks) {
                    chunk.beingRendered = chunk in chunksInView
                }
            }
        }
    }

    fun update() {
        ResourceNode.update()
        updateLevelAndViewInformation()
        updateChunksBeingRendered()
        allLevels.forEach { it.update() }
    }

    fun exists(levelName: String) = levelInfos.any { it.name == levelName }

    /**
     * @return either a new level if none existed previously, or loads and returns the previous one
     */
    fun get(info: LevelInfo): Level {
        return SimplexLevel(info)
    }

    override fun onDirectoryChange(dir: Path) {
        if (dir == FileManager.fileSystem.getPath(GameDirectoryIdentifier.SAVES) ||
                dir == FileManager.fileSystem.getPath(GameDirectoryIdentifier.SERVER_SAVES)) {
            println("directory changed")
            indexLevels(dir)
        }
    }

    fun indexLevels(id: GameDirectoryIdentifier = GameDirectoryIdentifier.SAVES) = indexLevels(FileManager.fileSystem.getPath(id))

    fun indexLevels(path: Path) {
        val allFiles = Files.walk(path).filter { Files.isRegularFile(it) }.map { it.toFile() }.toList()
        val levelFileInfoFilePairs = mutableMapOf<File, File>()
        for (file in allFiles) {
            if (file.name.endsWith(".level"))
                if (file !in levelFileInfoFilePairs) {
                    levelFileInfoFilePairs.put(file, allFiles.first { it.name.removeSuffix(".info") == file.name.removeSuffix(".level") })
                }
        }
        for ((level, info) in levelFileInfoFilePairs) {
            val stream = Input(info.inputStream())
            val levelInfo = Game.KRYO.readObject(stream, LevelInfo::class.java)
            levelInfos.add(levelInfo)
        }
    }
}