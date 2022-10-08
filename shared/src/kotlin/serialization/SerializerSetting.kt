package serialization

import java.lang.reflect.Field

sealed class SerializerSetting<T>(val annotationType: Class<out Annotation>) {

    init {
        ALL.add(this)
    }

    fun getFrom(field: Field): T {
        return (field.getAnnotation(annotationType)
            ?: throw Exception("$annotationType not present in ${field.name} (from ${field.declaringClass}) annotations")) as T
    }

    companion object {
        val ALL = mutableListOf<SerializerSetting<*>>()

        fun getSettings(field: Field): List<SerializerSetting<*>> {
            val settings = mutableListOf<SerializerSetting<*>>()
            for (setting in ALL) {
                if (field.isAnnotationPresent(setting.annotationType)) {
                    settings.add(setting)
                }
            }
            return settings
        }
    }
}

object SparseSetting : SerializerSetting<Sparse>(Sparse::class.java)
object IdSetting : SerializerSetting<Id>(Id::class.java)
val Field.id get() = IdSetting.getFrom(this).id
object WriteStrategySetting : SerializerSetting<UseWriteStrategy>(UseWriteStrategy::class.java)
object ReadStrategySetting : SerializerSetting<UseReadStrategy>(UseReadStrategy::class.java)
object CreateStrategySetting : SerializerSetting<UseCreateStrategy>(UseCreateStrategy::class.java)
object ReferenceSetting : SerializerSetting<AsReference>(AsReference::class.java)
object ObjectIdentifierSetting : SerializerSetting<ObjectIdentifier>(ObjectIdentifier::class.java)
object ObjectListSetting : SerializerSetting<ObjectList>(ObjectList::class.java)