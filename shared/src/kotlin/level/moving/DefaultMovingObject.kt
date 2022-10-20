package level.moving

class DefaultMovingObject(type: MovingObjectType<DefaultMovingObject>, x: Int, y: Int) : MovingObject(type, x, y) {
    private constructor() : this(MovingObjectType.ERROR, 0, 0)
}