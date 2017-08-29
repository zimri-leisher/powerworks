package level.tile

import graphics.ImageCollection
import graphics.Texture

class OreTileType(textures: Array<Texture>, val maxAmount: Int, val minAmount: Int = 1) : TileType(textures) {
    constructor(textures: ImageCollection, maxAmount: Int, minAmount: Int = 1): this(textures.textures, maxAmount, minAmount)
    constructor(texture: Texture, maxAmount: Int, minAmount: Int = 1): this(arrayOf(texture), maxAmount, minAmount)
}