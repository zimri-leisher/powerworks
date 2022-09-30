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

object Registration {

    private fun getSerializerCtor(
        type: Class<out Serializer<out Any>>,
        args: Array<out Any>
    ): Constructor<out Serializer<out Any>> {
        return type.constructors.firstOrNull { ctor ->
            ctor.parameters.contentEquals(
                arrayOf(
                    Class::class.java,
                    List::class.java,
                    *args.map { arg -> arg::class.java }.toTypedArray()
                )
            )
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
        defaultSerializerFactory = {
            getSerializerCtor(type, args).newInstance(it, settings, *args)
        }
    }

    inline class RegistryEntry(val id: Int) {

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
            val ctor = getSerializerCtor(serializerClass, *args)
            defaultSerializers[type] = ctor.newInstance(type, settings, *args) as Serializer<Any>
        }
    }

    private var nextId = Primitive.values().maxByOrNull { it.id }!!.id + 1

    val REFERENCE_ID = nextId++

    private val ids = mutableMapOf<Int, Class<*>>()

    private val defaultSerializers = mutableMapOf<Class<*>, Serializer<out Any>>()
    private var defaultSerializerFactory: (type: Class<*>) -> Serializer<out Any> = { Serializer(it, listOf()) }

    init {
        ids.put(Primitive.NULL.id, Nothing::class.java)
        ids.put(Primitive.BYTE.id, java.lang.Byte::class.java)
        ids.put(Primitive.SHORT.id, java.lang.Short::class.java)
        ids.put(Primitive.INT.id, java.lang.Integer::class.java)
        ids.put(Primitive.LONG.id, java.lang.Long::class.java)
        ids.put(Primitive.FLOAT.id, java.lang.Float::class.java)
        ids.put(Primitive.DOUBLE.id, java.lang.Double::class.java)
        ids.put(Primitive.CHAR.id, java.lang.Character::class.java)
        ids.put(Primitive.BOOLEAN.id, java.lang.Boolean::class.java)
        ids.put(Primitive.STRING.id, java.lang.String::class.java)
    }

    private fun register(type: KClass<*>, id: Int) = register(type.java, id)

    private fun register(type: Class<*>, id: Int): RegistryEntry {
        registerClass(type, id, defaultSerializerFactory(type))
        return RegistryEntry(id)
    }

    fun registerAll() {

        // max 279
        /* COLLECTIONS */
        val singletonList: List<Any> = listOf(1)
        register(singletonList::class, 1)
            .setSerializer(CollectionSerializer::class, listOf(), { it: MutableList<Any> -> it.toList() })
        register(emptyList<Nothing>()::class, 2)
            .setSerializer(EmptyListSerializer::class)
        val immutableListClass = Class.forName("java.util.Arrays${'$'}ArrayList") as Class<Collection<Any?>>
        register(immutableListClass, 3).setSerializer(
            CollectionSerializer::class,
            listOf(),
            { it: MutableCollection<Any> -> it.toList() })
        register(ArrayList::class, 4)
            .setSerializer(MutableCollectionSerializer::class)
        register(LinkedHashSet::class, 5)
            .setSerializer(MutableCollectionSerializer::class)
        register(Array<out Any?>::class, 6)
            .setSerializer(ArraySerializer::class)
        register(kotlin.Pair::class, 7)
            .setSerializer(PairSerializer::class)
        register(LinkedHashMap::class, 8)
            .setSerializer(MutableMapSerializer::class)
        register(Set::class, 9)
            .setSerializer(MutableCollectionSerializer::class)
        register(Polygon::class, 10)
            .setSerializer(AllFieldsSerializer::class)
        register(Rectangle::class, 11)
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
        register(WeaponItem::class, 46)
        register(Weapon::class, 47)
        register(WeaponItemType::class, 48)
            .setSerializer(AutoIDSerializer::class)
        register(Inventory::class, 49)
        register(Item::class, 50)
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
            .setSerializer(LevelSerializer::class)
        register(UnknownLevel::class, 58)
            .setSerializer(LevelSerializer::class)
        register(Chunk::class, 59)
        register(Hitbox::class, 60)
        register(LevelData::class, 61)
        register(LevelInfo::class, 62)
        register(LevelObjectTextures::class, 63)
        register(LevelObjectType::class, 64)
            .setSerializer(AutoIDSerializer::class)
        register(RemoteLevel::class, 65)
            .setSerializer(LevelSerializer::class)
        register(ChunkData::class, 66)
        register(GhostLevelObject::class, 67)
        register(LevelObjectAdd::class, 68)
        register(LevelObjectRemove::class, 69)
        register(DefaultLevelUpdate::class, 70)
        register(LevelUpdateType::class, EnumSerializer<LevelUpdateType>(), 227)
        register(CrafterBlockSelectRecipe::class, 229)
        register(ResourceNodeTransferThrough::class, 232)
        register(MachineBlockFinishWork::class, 233)
        register(EntitySetPath::class, 237)
        register(EntityPathUpdate::class, 238)
        register(EntityAddToGroup::class, 242)
        register(EntitySetFormation::class, 243)
        register(EntitySetTarget::class, 246)
        register(EntityFireWeapon::class, 253)
        register(FarseekerBlockSetAvailableLevels::class, 263)
        register(FarseekerBlockSetDestinationLevel::class, 269)
        register(ResourceNodeBehaviorEdit::class, 265)
        register(LevelObjectSwitchLevelsTo::class, 270)
        register(LevelPosition::class, 271)
        register(LevelObjectResourceContainerModify::class, 273)

        /* /BLOCK */
        register(BlockType::class, AutoIDSerializer({ BlockType.ALL }, { it.id }), 22)
        register(MachineBlockType::class, AutoIDSerializer({ MachineBlockType.ALL }, { it.id }), 119)
        register(CrafterBlockType::class, AutoIDSerializer({ CrafterBlockType.ALL }, { it.id }), 120)
        register(FluidTankBlockType::class, AutoIDSerializer({ FluidTankBlockType.ALL }, { it.id }), 121)
        register(ChestBlockType::class, AutoIDSerializer({ ChestBlockType.ALL }, { it.id }), 122)
        register(PipeBlockType::class, AutoIDSerializer({ PipeBlockType.ALL }, { it.id }), 188)

        setSerializerFactory { LevelObjectSerializer<LevelObject>() }

        register(Block::class, 23)
        register(ChestBlock::class, 24)
        register(CrafterBlock::class, 25)
        register(DefaultBlock::class, 26)
        register(FluidTankBlock::class, 27)
        register(FurnaceBlock::class, 28)
        register(MachineBlock::class, 29)
        register(MinerBlock::class, 30)
        register(SolidifierBlock::class, 31)
        register(PipeBlock::class, 189)
        register(RobotFactoryBlock::class, 217)
        register(ArmoryBlock::class, 247)
        register(FarseekerBlock::class, 260)
        register(SmelterBlock::class, 272)

        /* /ENTITY */
        register(Entity::class, 32)
        register(EntityType::class, AutoIDSerializer({ EntityType.ALL }, { it.id }), 36)
        register(EntityBehavior::class, Serializer.Tagged<EntityBehavior>(), 234)
        register(EntityGroup::class, Serializer.Tagged<EntityGroup>(), 240)
        register(Formation::class, Serializer.Tagged<Formation>(), 244)
        register(DefaultEntity::class, 235)

        /* //ROBOT */
        register(BrainRobot::class, 33)
        register(Robot::class, 34)
        register(RobotType::class, AutoIDSerializer({ RobotType.ALL }, { it.id }), 35)

        /* /GENERATOR */
        setSerializerFactory { Serializer.Tagged<Any>() }
        register(EmptyLevelGenerator::class, 118)
        register(LevelGenerator::class, 37)
        register(LevelType::class, AutoIDSerializer({ LevelType.ALL }, { it.id }), 38)
        register(OpenSimplexNoise::class, 40)

        /* /MOVING */
        register(MovingObject::class, LevelObjectSerializer<MovingObject>(), 41)
        register(MovingObjectType::class, AutoIDSerializer({ MovingObjectType.ALL }, { it.id }), 42)

        /* /PARTICLE */
        register(ParticleType::class, AutoIDSerializer({ ParticleType.ALL }, { it.id }), 43)

        /* /PIPE */
        register(FluidPipeBlock::class, LevelObjectSerializer<FluidPipeBlock>(), 44)
        register(ItemPipeBlock::class, LevelObjectSerializer<ItemPipeBlock>(), 186)
        register(PipeState::class, EnumSerializer<PipeState>(), 46)

        /* /TILE */
        register(OreTile::class, 47)
        register(OreTileType::class, AutoIDSerializer({ OreTileType.ALL }, { it.id }), 108)
        register(Tile::class, 48)
        register(TileType::class, AutoIDSerializer({ TileType.ALL }, { it.id }), 49)

        /* MAIN */
        register(DebugCode::class, EnumSerializer<DebugCode>(), 65)
        register(Version::class, VersionSerializer(), 66)

        /* MISC */
        register(Coord::class, 67)
        register(TileCoord::class, 68)

        /* NETWORK */
        register(User::class, 69)

        /* /PACKET */
        register(ChunkDataPacket::class, 70)
        register(ClientHandshakePacket::class, 71)
        register(GenericPacket::class, 72)
        register(LevelDataPacket::class, 73)
        register(LevelInfoPacket::class, 74)
        register(Packet::class, 75)
        register(PacketType::class, EnumSerializer<PacketType>(), 77)
        register(PlayerDataPacket::class, 79)
        register(RequestLevelDataPacket::class, 81)
        register(RequestLevelInfoPacket::class, 82)
        register(RequestPlayerDataPacket::class, 83)
        register(ServerHandshakePacket::class, 84)
        register(LoadGamePacket::class, 134)
        register(RequestLoadGamePacket::class, 135)
        register(PlayerActionPacket::class, 192)
        register(AcknowledgePlayerActionPacket::class, 196)
        register(BlockReference::class, NetworkReferenceSerializer(), 197)
        register(MovingObjectReference::class, NetworkReferenceSerializer(), 198)
        register(ResourceNodeReference::class, NetworkReferenceSerializer(), 199)
        register(BrainRobotReference::class, NetworkReferenceSerializer(), 256)
        register(LevelUpdatePacket::class, 228)
        register(LevelLoadedSuccessPacket::class, 239)

        /* PLAYER */
        register(Player::class, PlayerSerializer(), 85)
        register(ActionLevelObjectPlace::class, 202)
        register(ActionLevelObjectRemove::class, 203)
        register(ActionSelectCrafterRecipe::class, 206)
        register(ActionControlEntity::class, 207)
        register(ActionEditResourceNodeBehavior::class, 208)
        register(ActionEntityCreateGroup::class, 241)
        register(ActionTransferResourcesBetweenLevelObjects::class, 245)
        register(ActionFarseekerBlockSetLevel::class, 268)
        register(Team::class, 255)

        /* RESOURCE */
        register(ResourceCategory::class, EnumSerializer<ResourceCategory>(), 86)
        register(ResourceContainer::class, 87)
        register(ResourceList::class, 89)
        register(MutableResourceList::class, 276)
        register(ResourceNode::class, 90)
        register(ResourceNodeBehavior::class, 91)
        register(RoutingLanguageIORule::class, 92)
        register(ResourceType::class, AutoIDSerializer({ ResourceType.ALL }, { it.id }), 93)
        register(ResourceNode2::class, 278)
        register(PipeNetworkVertex::class, 279)

        /* ROUTING */
        register(ResourceRoutingNetwork::class, 98)
        register(RoutingLanguageStatement::class, RoutingLanguageStatementSerializer(), 99)

        register(TokenType::class.java, EnumSerializer<TokenType>(), 184)
        register(Token::class.java, 185)

        setSerializerFactory { NodeSerializer() }

        register(BooleanLiteral::class, 161)
        register(IntLiteral::class, 162)
        register(DoubleLiteral::class, 163)
        register(ResourceTypeLiteral::class, 164)
        register(TotalQuantity::class, 165)
        register(TotalNetworkQuantity::class, 166)
        register(Not::class, 167)
        register(QuantityOf::class, 168)
        register(NetworkQuantityOf::class, 169)
        register(Implies::class, 170)
        register(IfAndOnlyIf::class, 171)
        register(ExclusiveOr::class, 172)
        register(Or::class, 173)
        register(And::class, 174)
        register(Plus::class, 175)
        register(Minus::class, 176)
        register(Multiply::class, 177)
        register(Divide::class, 178)
        register(GreaterThan::class, 179)
        register(GreaterThanOrEqual::class, 180)
        register(LessThan::class, 181)
        register(LessThanOrEqual::class, 182)
        register(Equal::class, 183)

        setSerializerFactory { Serializer.Tagged<Any>() }

        /* SCREEN */
        register(Camera::class, LevelObjectSerializer<Camera>(), 131)

        /* SETTING */
        register(UnknownSetting::class, 266)
        register(IntSetting::class, 267)
        register(BooleanSetting::class, 277)
        val clazz =
            java.util.Collections::class.java.declaredClasses.first { it.simpleName == "SynchronizedRandomAccessList" }
        //register(clazz, 103).setInstantiator { Collections.synchronizedList<Any?>(mutableListOf()) }
        register(TextureRegion::class, 104)
        register(UUID::class, Serializer<UUID>(), 109).setSerializer(
            { UUID.fromString(it.readUTF()) },
            { newInstance, _ -> newInstance },
            { obj, output -> output.writeUTF(obj.toString()) })
        //register(Comparator::class, FieldSerializer<Comparator<*>>(this, Comparator::class), 132)
    }


    fun setSerializerFactory(fac: (type: Class<*>) -> Serializer<*>) {
        defaultSerializerFactory = fac
    }

    fun resetSerializerFactory() {
        defaultSerializerFactory = { Serializer<Any>() }
    }

    fun registerClass(type: Class<*>, id: Int = nextId++, serializer: Serializer<out Any>) {
        if (Serialization.isPrimitive(id)) throw RegistrationException("Cannot reregister a primitive id ($id)")
        if (id < 0) throw RegistrationException("Cannot register an id less than 0 (tried to register $id)")
        if (Function::class.java.isAssignableFrom(type)) throw RegistrationException("Cannot register lambdas")
        if (defaultSerializers.putIfAbsent(type, serializer) != null) {
            throw RegistrationException("$type already has default settings")
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

    fun getSerializer(field: Field): Serializer<*> {
        val defaultSerializer = getSerializer(field.type)
        val options = SerializerSetting.getSettings(field)
        var newCreateStrategy: CreateStrategy<Any> = defaultSerializer.createStrategy
        var newReadStrategy: ReadStrategy<Any> = defaultSerializer.readStrategy
        var newWriteStrategy: WriteStrategy<Any> = defaultSerializer.writeStrategy
        for (option in options) {
            when (option) {
                ReferenceSetting -> {
                    if (!Referencable::class.java.isAssignableFrom(field.type)) {
                        throw Exception("Field ${field.name} in class ${field.declaringClass} was specified to be saved as a SerializationReference but ${field.type} does not implement SerializationReferencable")
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

    fun getSerializer(type: Class<*>): Serializer<Any> {
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
