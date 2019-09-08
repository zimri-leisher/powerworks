package level.entity

import behavior.BehaviorTree
import behavior.DefaultVariable
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import item.weapon.WeaponItemType
import level.moving.MovingObject

abstract class Entity(type: EntityType<out Entity>, xPixel: Int, yPixel: Int, rotation: Int = 0) : MovingObject(type, xPixel, yPixel, rotation) {
    override val type = type

    // tags start here because of superclass tags

    @Tag(22)
    var health = type.maxHealth
    @Tag(23)
    var weapon: WeaponItemType? = null

    @Tag(24)
    val behaviors = mutableMapOf<BehaviorTree, Int>()
    @Tag(25)
    private val _toAdd = mutableMapOf<BehaviorTree, Int>()
    @Tag(26)
    private val _toRemove = mutableListOf<BehaviorTree>()
    @Tag(27)
    private var traversing = false

    override fun update() {
        traversing = true
        behaviors.forEach { it.key.update(this) }
        traversing = false
        behaviors.putAll(_toAdd)
        _toAdd.clear()
        _toRemove.forEach { behaviors.remove(it) }
        _toRemove.clear()
        super.update()
    }

    fun setPriority(behavior: BehaviorTree, priority: Int) {
        behaviors[behavior] = priority
    }

    fun getPriority(behavior: BehaviorTree) = behaviors.filterKeys { it == behavior }.values.firstOrNull() ?: -1

    fun finishBehavior(behavior: BehaviorTree) {
        if (traversing) {
            _toRemove.add(behavior)
        } else {
            behaviors.remove(behavior)
        }
    }

    fun runBehavior(behavior: BehaviorTree, priority: Int = 0, argument: Any? = null) {
        if (behavior.hasBeenInitialized(this)) {
            behavior.reset(this)
        }
        if (traversing) {
            _toAdd.put(behavior, priority)
        } else {
            behaviors.put(behavior, priority)
        }
        if (argument != null) {
            behavior.data.set(name = DefaultVariable.ARGUMENT.name, value = argument)
        }
        behavior.init(this)
    }
}