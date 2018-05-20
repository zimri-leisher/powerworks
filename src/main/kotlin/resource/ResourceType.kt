package resource

import graphics.Texture

interface ResourceType {
    val icon: Texture
    val category: ResourceCategory
    val name: String
}