package level.entity

class DefaultEntity(type: EntityType<DefaultEntity>, xPixel: Int, yPixel: Int) : Entity(type, xPixel, yPixel) {
    private constructor() : this(EntityType.ERROR, 0, 0)
}