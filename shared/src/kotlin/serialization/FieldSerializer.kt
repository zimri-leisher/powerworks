package serialization

import java.lang.reflect.Field
import java.lang.reflect.Modifier

abstract class FieldSerializer<T : Any>(type: Class<T>, settings: List<SerializerSetting<*>>) :
    Serializer<T>(type, settings, EmptyConstructorCreateStrategy(type, settings)) {

    data class CachedField(val field: Field, val settings: List<SerializerSetting<*>>)

    val fields: Array<CachedField> by lazy {
        fun getFields(type: Class<*>): Array<Field> {
            var fields = type.declaredFields.filter { !Modifier.isStatic(it.modifiers) }.toTypedArray()
            if (type.superclass != null) {
                fields += getFields(type.superclass)
            }
            return fields
        }
        getFields(type).map { CachedField(it, SerializerSetting.getSettings(it)) }.toTypedArray()
    }
}