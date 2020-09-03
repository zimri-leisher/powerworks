package level.entity

class DefaultEntity(type: EntityType<DefaultEntity>, x: Int, y: Int) : Entity(type, x, y) {
    private constructor() : this(EntityType.ERROR, 0, 0)
}