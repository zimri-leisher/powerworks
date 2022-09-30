package serialization

import java.lang.reflect.Field
import java.lang.reflect.Modifier

open class FieldSerializer<T : Any>(type: Class<T>, settings: List<SerializerSetting<*>>) :
    Serializer<T>(type, settings) {
    // field serializer should go thru and cache fields with their annotations/annotation values

    data class CachedField(val field: Field, val serializer: Serializer<*>)

    var fields: Array<CachedField>
        protected set

    init {
        fun getFields(type: Class<*>): Array<Field> {
            var fields = type.declaredFields.filter { !Modifier.isStatic(it.modifiers) }.toTypedArray()
            if (type.superclass != null) {
                fields += getFields(type.superclass)
            }
            return fields
        }

        fields = getFields(type).map { CachedField(it, Registration.getSerializer(it)) }.toTypedArray()

        if (fields.isNotEmpty()) {
            SerializerDebugger.writeln("Found fields:")
            for (field in fields) {
                SerializerDebugger.writeln("    ${field.field.name}: ${field.field.type.simpleName} ")
            }
        } else {
            SerializerDebugger.writeln("Found no fields")
        }
    }
}