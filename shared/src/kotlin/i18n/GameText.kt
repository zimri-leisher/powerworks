package i18n

import io.Control
import resource.ResourceType

object GameText {
    fun getControlName(control: Control): String {
        return control.name
    }
    fun getResourceTypeName(type: ResourceType): String {
        return type.name
    }
}