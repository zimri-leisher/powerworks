package serialization

import audio.AudioManager
import audio.Sound
import behavior.Behavior
import behavior.BehaviorTree
import behavior.VariableData
import behavior.leaves.EntityPath
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.esotericsoftware.kryonet.FrameworkMessage
import crafting.Recipe
import crafting.RecipeCategory
import data.*
import fluid.FluidTank
import fluid.FluidType
import fluid.MoltenOreFluidType
import graphics.*
import item.*
import item.tool.ToolItemType
import item.weapon.*
import level.*
import level.block.*
import level.entity.*
import level.entity.robot.BrainRobot
import level.entity.robot.Robot
import level.entity.robot.RobotType
import level.generator.EmptyLevelGenerator
import level.generator.LevelGenerator
import level.generator.LevelType
import level.generator.OpenSimplexNoise
import level.moving.MovingObject
import level.moving.MovingObjectType
import level.particle.ParticleType
import level.pipe.FluidPipeBlock
import level.pipe.ItemPipeBlock
import level.pipe.PipeBlock
import level.pipe.PipeState
import level.tile.OreTile
import level.tile.OreTileType
import level.tile.Tile
import level.tile.TileType
import level.update.*
import main.DebugCode
import main.Version
import main.VersionSerializer
import main.isKotlinClass
import misc.Coord
import misc.TileCoord
import network.*
import network.packet.*
import player.*
import player.team.Team
import resource.*
import routing.*
import routing.script.*
import screen.Camera
import setting.BooleanSetting
import setting.IntSetting
import setting.UnknownSetting
import java.awt.Polygon
import java.awt.Rectangle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.system.measureTimeMillis

data class SerializerTemplate(
    val type: Class<out Serializer<out Any>>,
    val settings: List<SerializerSetting<*>> = listOf(),
    val args: Array<out Any> = arrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializerTemplate

        if (type != other.type) return false
        if (settings != other.settings) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + settings.hashCode()
        result = 31 * result + args.contentHashCode()
        return result
    }
}

object Registration {

    private fun getSerializerCtor(
        type: Class<out Serializer<out Any>>,
        args: Array<out Any>
    ): Constructor<out Serializer<out Any>> {
        val shouldBe = arrayOf(
            Class::class.java,
            List::class.java,
            *args.map { arg -> arg::class.java }.toTypedArray()
        )
        return type.constructors.firstOrNull { ctor ->
            println("comparing ${ctor.parameters.joinToString { it.type.toString() }} with ${shouldBe.joinToString { it.toString() }}")
            if (ctor.parameters.size != shouldBe.size) {
                println("diff size")
                false
            } else {
                var matches = true
                for (i in ctor.parameters.indices) {
                    if (!ctor.parameters[i].type.isAssignableFrom(shouldBe[i])) {
                        println("doesn't match")
                        matches = false
                        break
                    }
                }
                matches
            }
        } as Constructor<out Serializer<out Any>>?
            ?: throw Exception("Could not instantiate serializer ${type.simpleName} with (type, settings, *args) ctor")
    }

    private fun setDefaultSerializer(
        type: KClass<out Serializer<out Any>>,
        settings: List<SerializerSetting<*>> = listOf(),
        vararg args: Any
    ) = setDefaultSerializer(type.java, settings, *args)

    private fun setDefaultSerializer(
        type: Class<out Serializer<out Any>>,
        settings: List<SerializerSetting<*>> = listOf(),
        vararg args: Any
    ) {
        defaultSerializerTemplate = SerializerTemplate(type, settings, args)
    }

    @JvmInline
    value class RegistryEntry(val id: Int) {

        fun setSerializer(
            serializerClass: KClass<out Serializer<*>>,
            settings: List<SerializerSetting<*>> = listOf(),
            vararg args: Any
        ) = setSerializer(serializerClass.java, settings, *args)

        fun setSerializer(
            serializerClass: Class<out Serializer<*>>,
            settings: List<SerializerSetting<*>> = listOf(),
            vararg args: Any
        ) {
            val type = ids[id]!!
            defaultSerializerClasses[type] = SerializerTemplate(serializerClass, settings, args)
        }
    }

    private var nextId = Primitive.values().maxBy { it.id }.id + 1

    val REFERENCE_ID = nextId++

    private val ids = mutableMapOf<Int, Class<*>>()

    private val defaultSerializerClasses =
        mutableMapOf<Class<*>, SerializerTemplate>()
    private val defaultSerializers = mutableMapOf<Class<*>, Serializer<out Any>>()
    private var defaultSerializerTemplate: SerializerTemplate = SerializerTemplate(Serializer::class.java)

    private fun register(type: KClass<*>, id: Int) = register(type.java, id)

    private fun register(type: Class<*>, id: Int): RegistryEntry {
        registerClass(type, id, defaultSerializerTemplate)
        return RegistryEntry(id)
    }

    fun registerAll() {

        // max 221
        /* PRIMITIVES */
        setDefaultSerializer(Serializer::class)
        register(Nothing::class, 0)
        register(java.lang.Byte::class, 1)
        register(java.lang.Short::class, 2)
        register(java.lang.Integer::class, 3)
        register(java.lang.Long::class, 4)
        register(java.lang.Float::class, 5)
        register(java.lang.Double::class, 6)
        register(java.lang.Character::class, 7)
        register(java.lang.Boolean::class, 8)
        register(java.lang.String::class, 9)

        /* COLLECTIONS */
        val singletonList: List<Any> = listOf(1)
        register(singletonList::class, 221)
            .setSerializer(CollectionSerializer::class, listOf(), { it: MutableList<Any> -> it.toList() })
        register(emptyList<Nothing>()::class, 211)
            .setSerializer(EmptyListSerializer::class)
        val immutableListClass = Class.forName("java.util.Arrays${'$'}ArrayList") as Class<Collection<Any?>>
        register(immutableListClass, 212).setSerializer(
            CollectionSerializer::class,
            listOf(),
            { it: MutableCollection<Any> -> it.toList() })
        register(ArrayList::class, 213)
            .setSerializer(MutableCollectionSerializer::class)
        register(LinkedHashSet::class, 214)
            .setSerializer(MutableCollectionSerializer::class)
        register(Array<out Any?>::class, 215)
            .setSerializer(ArraySerializer::class)
        register(kotlin.Pair::class, 216)
            .setSerializer(PairSerializer::class)
        register(LinkedHashMap::class, 217)
            .setSerializer(MutableMapSerializer::class)
        register(Set::class, 218)
//            .setSerializer(MutableCollectionSerializer::class)
        register(Rectangle::class, 220)
            .setSerializer(AllFieldsSerializer::class)
        register(Polygon::class, 219)
            .setSerializer(AllFieldsSerializer::class)
        register(emptyMap<Nothing, Nothing>()::class, 12)
            .setSerializer(EmptyMapSerializer::class)
        register(mapOf(1 to 1)::class, 13)
            .setSerializer(MapSerializer::class, listOf(), { it: MutableMap<Any, Any> -> it.toMap() })
        register(setOf(1)::class, 14)
            .setSerializer(CollectionSerializer::class, listOf(), { it: MutableCollection<Any> -> it.toSet() })
        register(emptySet<Int>()::class, 15)
            .setSerializer(CollectionSerializer::class, listOf(), { it: MutableCollection<Any> -> it.toSet() })


        setDefaultSerializer(AllFieldsSerializer::class.java)
        register(FrameworkMessage.RegisterTCP::class, 16)
        register(FrameworkMessage.RegisterUDP::class, 17)
        register(FrameworkMessage.KeepAlive::class, 18)
        register(FrameworkMessage.DiscoverHost::class, 19)
        register(FrameworkMessage.Ping::class, 20)

        setDefaultSerializer(TaggedSerializer::class)

        register(Serialization.WarmupObject::class, 21)

        /* AUDIO */
        register(AudioManager.SoundSource::class, 22)
        register(Sound::class, 23)
            .setSerializer(EnumSerializer::class)

        /* BEHAVIOR */
        Behavior
        register(BehaviorTree::class, 24)
            .setSerializer(AutoIDSerializer::class)
        register(VariableData::class, 25)
        register(EntityPath::class, 26)

        /* CRAFTING */
        register(Recipe::class, 27)
            .setSerializer(AutoIDSerializer::class)
        register(RecipeCategory::class, 28)
            .setSerializer(EnumSerializer::class)

        /* DATA */
        register(ConcurrentlyModifiableMutableList::class, 29)
        register(ConcurrentlyModifiableMutableMap::class, 30)
        register(ConcurrentlyModifiableWeakMutableList::class, 31)
        register(ConcurrentlyModifiableWeakMutableMap::class, 32)
        register(WeakMutableList::class, 33)
        register(WeakMutableMap::class, 34)

        /* FLUID */
        register(FluidTank::class, 35)
        register(FluidType::class, 36)
            .setSerializer(AutoIDSerializer::class)
        register(MoltenOreFluidType::class, 37)
            .setSerializer(AutoIDSerializer::class)
        /* GRAPHICS */
        register(ImageCollection::class, 38)
        register(Renderable::class, 39)
        register(Texture::class, 40)
            .setSerializer(TextureSerializer::class)
        register(Animation::class, 41)
            .setSerializer(AnimationSerializer::class)
        register(AnimationCollection::class, 42)
            .setSerializer(AnimationCollectionSerializer::class)

        /* IO */

        /* ITEM */
        register(ToolItemType::class, 43)
            .setSerializer(AutoIDSerializer::class)
        register(Projectile::class, 44)
        register(ProjectileType::class, 45)
            .setSerializer(AutoIDSerializer::class)
        register(Weapon::class, 47)
        register(WeaponItemType::class, 48)
            .setSerializer(AutoIDSerializer::class)
        register(Inventory::class, 49)
        register(RobotItemType::class, 51)
            .setSerializer(AutoIDSerializer::class)
        register(EntityItemType::class, 52)
            .setSerializer(AutoIDSerializer::class)
        register(IngotItemType::class, 53)
            .setSerializer(AutoIDSerializer::class)
        register(OreItemType::class, 54)
            .setSerializer(AutoIDSerializer::class)
        register(BlockItemType::class, 55)
            .setSerializer(AutoIDSerializer::class)
        register(ItemType::class, 56)
            .setSerializer(AutoIDSerializer::class)

        /* LEVEL */
        register(ActualLevel::class, 57)
        register(UnknownLevel::class, 58)
        register(Chunk::class, 59)
        register(Hitbox::class, 60)
        register(LevelData::class, 61)
        register(LevelInfo::class, 62)
        register(PhysicalLevelObjectTextures::class, 63)
        register(LevelObjectType::class, 64)
            .setSerializer(AutoIDSerializer::class)
        register(RemoteLevel::class, 65)
        register(ChunkData::class, 66)
        register(GhostLevelObject::class, 67)
        register(LevelObjectAdd::class, 68)
        register(LevelObjectRemove::class, 69)
        register(DefaultLevelUpdate::class, 70)
        register(LevelUpdateType::class, 71)
            .setSerializer(EnumSerializer::class)
        register(CrafterBlockSelectRecipe::class, 72)
        register(MachineBlockFinishWork::class, 74)
        register(EntitySetPath::class, 75)
        register(EntityPathUpdate::class, 76)
        register(EntityAddToGroup::class, 77)
        register(EntitySetFormation::class, 78)
        register(EntitySetTarget::class, 79)
        register(EntityFireWeapon::class, 80)
        register(FarseekerBlockSetAvailableLevels::class, 81)
        register(FarseekerBlockSetDestinationLevel::class, 82)
        register(LevelObjectSwitchLevelsTo::class, 84)
        register(LevelPosition::class, 85)

        /* /BLOCK */
        register(BlockType::class, 87)
            .setSerializer(AutoIDSerializer::class)
        register(MachineBlockType::class, 88)
            .setSerializer(AutoIDSerializer::class)
        register(CrafterBlockType::class, 89)
            .setSerializer(AutoIDSerializer::class)
        register(FluidTankBlockType::class, 90)
            .setSerializer(AutoIDSerializer::class)
        register(ChestBlockType::class, 91)
            .setSerializer(AutoIDSerializer::class)
        register(PipeBlockType::class, 92)
            .setSerializer(AutoIDSerializer::class)

        setDefaultSerializer(TaggedSerializer::class)

        register(Block::class, 93)
        register(ChestBlock::class, 94)
        register(CrafterBlock::class, 95)
        register(DefaultBlock::class, 96)
        register(FluidTankBlock::class, 97)
        register(FurnaceBlock::class, 98)
        register(MachineBlock::class, 99)
        register(MinerBlock::class, 100)
        register(SolidifierBlock::class, 101)
        register(PipeBlock::class, 102)
        register(RobotFactoryBlock::class, 103)
        register(ArmoryBlock::class, 104)
        register(FarseekerBlock::class, 105)
        register(SmelterBlock::class, 106)

        /* /ENTITY */
        register(Entity::class, 107)
        register(EntityType::class, 108)
            .setSerializer(AutoIDSerializer::class)
        register(EntityBehavior::class, 109)
            .setSerializer(TaggedSerializer::class)
        register(EntityGroup::class, 110)
            .setSerializer(TaggedSerializer::class)
        register(Formation::class, 111)
            .setSerializer(TaggedSerializer::class)
        register(DefaultEntity::class, 112)

        /* //ROBOT */
        register(BrainRobot::class, 113)
        register(Robot::class, 114)
        register(RobotType::class, 115)
            .setSerializer(AutoIDSerializer::class)

        setDefaultSerializer(TaggedSerializer::class)

        /* /GENERATOR */
        register(EmptyLevelGenerator::class, 116)
        register(LevelGenerator::class, 117)
        register(LevelType::class, 118)
            .setSerializer(AutoIDSerializer::class)
        register(OpenSimplexNoise::class, 119)

        /* /MOVING */
        register(MovingObject::class, 120)
        register(MovingObjectType::class, 121)
            .setSerializer(AutoIDSerializer::class)

        /* /PARTICLE */
        register(ParticleType::class, 122)
            .setSerializer(AutoIDSerializer::class)

        /* /PIPE */
        register(FluidPipeBlock::class, 123)
        register(ItemPipeBlock::class, 124)
        register(PipeState::class, 125)
            .setSerializer(EnumSerializer::class)

        /* /TILE */
        register(OreTile::class, 126)
        register(OreTileType::class, 127)
            .setSerializer(AutoIDSerializer::class)
        register(Tile::class, 128)
        register(TileType::class, 129)
            .setSerializer(AutoIDSerializer::class)

        /* MAIN */
        register(DebugCode::class, 130)
            .setSerializer(EnumSerializer::class)
        register(Version::class, 131)
            .setSerializer(VersionSerializer::class)

        /* MISC */
        register(Coord::class, 132)
        register(TileCoord::class, 133)

        /* NETWORK */
        register(User::class, 134)

        /* /PACKET */
        register(ChunkDataPacket::class, 135)
        register(ClientHandshakePacket::class, 136)
        register(GenericPacket::class, 137)
        register(LevelDataPacket::class, 138)
        register(LevelInfoPacket::class, 139)
        register(Packet::class, 140)
        register(PacketType::class, 141)
            .setSerializer(EnumSerializer::class)
        register(PlayerDataPacket::class, 142)
        register(RequestLevelDataPacket::class, 143)
        register(RequestLevelInfoPacket::class, 144)
        register(RequestPlayerDataPacket::class, 145)
        register(ServerHandshakePacket::class, 146)
        register(LoadGamePacket::class, 147)
        register(RequestLoadGamePacket::class, 148)
        register(PlayerActionPacket::class, 149)
        register(AcknowledgePlayerActionPacket::class, 150)
        register(BlockReference::class, 151)
        register(MovingObjectReference::class, 152)
        register(ResourceNodeReference::class, 153)
        register(BrainRobotReference::class, 154)
        register(LevelUpdatePacket::class, 155)
        register(LevelLoadedSuccessPacket::class, 156)

        /* PLAYER */
        register(Player::class, 157)
            .setSerializer(PlayerSerializer::class)
        register(ActionLevelObjectPlace::class, 158)
        register(ActionLevelObjectRemove::class, 159)
        register(ActionSelectCrafterRecipe::class, 160)
        register(ActionControlEntity::class, 161)
        register(ActionEntityCreateGroup::class, 163)
        register(ActionFarseekerBlockSetLevel::class, 165)
        register(Team::class, 166)

        /* RESOURCE */
        register(ResourceCategory::class, 167)
            .setSerializer(EnumSerializer::class)
        register(ResourceContainer::class, 168)
        register(ResourceList::class, 169)
        register(MutableResourceList::class, 170)
        register(ResourceNode::class, 171)
        register(ResourceType::class, 174)
            .setSerializer(AutoIDSerializer::class)
        register(PipeNetworkVertex::class, 176)

        /* ROUTING */
        register(RoutingLanguageStatement::class, 178)
            .setSerializer(RoutingLanguageStatementSerializer::class)

        register(TokenType::class.java, 179)
            .setSerializer(EnumSerializer::class)
        register(Token::class.java, 180)

        setDefaultSerializer(NodeSerializer::class)

        register(BooleanLiteral::class, 181)
        register(IntLiteral::class, 182)
        register(DoubleLiteral::class, 183)
        register(ResourceTypeLiteral::class, 184)
        register(TotalQuantity::class, 185)
        register(TotalNetworkQuantity::class, 186)
        register(Not::class, 187)
        register(QuantityOf::class, 188)
        register(NetworkQuantityOf::class, 189)
        register(Implies::class, 190)
        register(IfAndOnlyIf::class, 191)
        register(ExclusiveOr::class, 192)
        register(Or::class, 193)
        register(And::class, 194)
        register(Plus::class, 195)
        register(Minus::class, 196)
        register(Multiply::class, 197)
        register(Divide::class, 198)
        register(GreaterThan::class, 199)
        register(GreaterThanOrEqual::class, 200)
        register(LessThan::class, 201)
        register(LessThanOrEqual::class, 202)
        register(Equal::class, 203)

        setDefaultSerializer(TaggedSerializer::class)

        /* SCREEN */
        register(Camera::class, 204)

        /* SETTING */
        register(UnknownSetting::class, 205)
        register(IntSetting::class, 206)
        register(BooleanSetting::class, 207)
        val clazz =
            java.util.Collections::class.java.declaredClasses.first { it.simpleName == "SynchronizedRandomAccessList" }
        //register(clazz, 103).setInstantiator { Collections.synchronizedList<Any?>(mutableListOf()) }
        register(TextureRegion::class, 208)
        register(UUID::class, 209)
            .setSerializer(UUIDSerializer::class)

        //register(Comparator::class, FieldSerializer<Comparator<*>>(this, Comparator::class), 132)

        createSerializers()
    }

    private fun registerClass(type: Class<*>, id: Int = nextId++, serializerTemplate: SerializerTemplate) {
        if (id < 0) throw RegistrationException("Cannot register an id less than 0 (tried to register $id)")
        if (Function::class.java.isAssignableFrom(type)) throw RegistrationException("Cannot register lambdas")
        if (defaultSerializerClasses.putIfAbsent(type, serializerTemplate) != null) {
            throw RegistrationException("$type already has default serializer set")
        } else {
            if (ids.putIfAbsent(id, type) != null) {
                throw RegistrationException("$id is already registered under ${ids[id]}, tried to reregister with $type")
            }
        }
        // this line here will initialize the object if it is an object
        try {
            if (type.isKotlinClass()) {
                with(type.kotlin) {
                    objectInstance
                }
            }
        } catch (e: IllegalAccessException) {
            if (!e.message!!.contains("access a member of class")) {
                throw e
            }
        }

        SerializerDebugger.writeln("Registered class $type with id $id")
    }

    private fun createSerializers() {
        for ((type, template) in defaultSerializerClasses) {
            val ctor = getSerializerCtor(template.type, template.args)
            println("trying to initialize ${ctor.parameters.joinToString { it.type.toString() }} with ${type::class.java} ${template.settings::class.java} ${template.args.joinToString { it::class.java.toString() }}")
            try {
                defaultSerializers[type] = ctor.newInstance(type, template.settings, *template.args) as Serializer<Any>
            } catch (e: java.lang.Exception) {
                System.err.println("Exception while registering $type with serializer template $template:")
                throw e
            }
        }
    }

    fun getSerializer(field: Field): Serializer<*> {
        val defaultSerializer = getSerializer(field.type)
        val options = SerializerSetting.getSettings(field)
        var newCreateStrategy: CreateStrategy<Any> = defaultSerializer.createStrategy
        var newReadStrategy: ReadStrategy<Any> = defaultSerializer.readStrategy as ReadStrategy<Any>
        var newWriteStrategy: WriteStrategy<Any> = defaultSerializer.writeStrategy as WriteStrategy<Any>
        for (option in options) {
            when (option) {
                ReferenceSetting -> {
                    if (!Referencable::class.java.isAssignableFrom(field.type)) {
                        // if its not itself a referencable, check if recursive is true
                        throw Exception("Field ${field.name} in class ${field.declaringClass} was specified to be saved as a Reference but ${field.type} does not implement Referencable")
                    }
                    newReadStrategy = ReadStrategy.None
                    newWriteStrategy =
                        ReferencableWriteStrategy(field.type as Class<Referencable<Any>>) as WriteStrategy<Any>
                    newCreateStrategy =
                        ReferencableCreateStrategy(field.type as Class<Referencable<Any>>)
                }

                WriteStrategySetting -> {
                    val writeStrategyClass = WriteStrategySetting.getFrom(field).writeStrategyClass
                    val ctor = writeStrategyClass.primaryConstructor
                    newWriteStrategy = ctor!!.call(field.type) as WriteStrategy<Any>
                }

                ReadStrategySetting -> {
                    val readStrategyClass = ReadStrategySetting.getFrom(field).readStrategyClass
                    val ctor = readStrategyClass.primaryConstructor
                    newReadStrategy = ctor!!.call(field.type) as ReadStrategy<Any>
                }

                CreateStrategySetting -> {
                    val createStrategyClass = CreateStrategySetting.getFrom(field).createStrategyClass
                    val ctor = createStrategyClass.primaryConstructor
                    newCreateStrategy = ctor!!.call(field.type)
                }

                else -> {
                    // it is a setting meant for individual strategies
                }
            }
        }
        if (newReadStrategy != defaultSerializer.readStrategy
            || newWriteStrategy != defaultSerializer.writeStrategy
            || newCreateStrategy != defaultSerializer.createStrategy
        ) {
            return Serializer(field.type, options, newCreateStrategy, newWriteStrategy, newReadStrategy)
        }
        return defaultSerializer
    }

    fun getSerializer(type: Class<*>): Serializer<out Any> {
        val actualType = Serialization.makeTypeNice(type)
        return defaultSerializers[actualType]
            ?: throw Exception("Class $actualType has not been assigned a default serializer")
    }

    fun getId(type: Class<*>): Int {
        val actualType = Serialization.makeTypeNice(type)
        val id = ids.filterValues { it == actualType }.keys.firstOrNull()
        if (id == null) {
            throw Exception("Unregistered class ${actualType.typeName}")
        }
        return id
    }

    fun getType(id: Int): Class<*>? {
        return ids.get(id)
    }

}

enum class Primitive {
    NULL,
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    CHAR,
    BOOLEAN,
    STRING;

    val id get() = ordinal
}

fun test() {
    val byteOutput = ByteArrayOutputStream(32)
    val output = Output(byteOutput)
    //val level = ActualLevel(UUID.randomUUID(), LevelInfo(User(UUID.randomUUID(), ""), "", "", LevelType.DEFAULT_SIMPLEX, 10L))
    //val levelObj = newPlayer(User(UUID.randomUUID(), "")).brainRobot
    //val array = Inventory(3, 3)
    println(
        "Now write new thing: ${
            measureTimeMillis {
                val statement = RoutingLanguage.parse("quantity IRON_ORE > 4")
                output.write(statement)
            }
        }"
    )

    output.close()
    val input = Input(ByteArrayInputStream(byteOutput.toByteArray()))
    println(
        "now read new thing: ${
            measureTimeMillis {
                val statement = input.readUnknown()
                println(statement)

            }
        }"
    )
    input.close()
}
