package serialization

import java.lang.reflect.Field

enum class SerializerSettingTarget {
    STRATEGY, FIELD
}

sealed class SerializerSetting<T>(
    val value: T,
    vararg val targets: SerializerSettingTarget = arrayOf(
        SerializerSettingTarget.STRATEGY,
        SerializerSettingTarget.FIELD
    )
) {

    companion object {
        val ALL = mutableListOf<SerializerSetting<*>>()

        fun getSettings(field: Field): Set<SerializerSetting<*>> {
            val settings = mutableSetOf<SerializerSetting<*>>()
            for (annotation in field.annotations) {
                val setting = when (annotation.annotationClass.java) {
                    Sparse::class.java -> {
                        SparseSetting(annotation as Sparse)
                    }

                    Id::class.java -> {
                        IdSetting(annotation as Id)
                    }

                    UseWriteStrategy::class.java -> {
                        WriteStrategySetting(annotation as UseWriteStrategy)
                    }

                    UseReadStrategy::class.java -> {
                        ReadStrategySetting(annotation as UseReadStrategy)
                    }

                    UseCreateStrategy::class.java -> {
                        CreateStrategySetting(annotation as UseCreateStrategy)
                    }

                    AsReference::class.java -> {
                        ReferenceSetting(annotation as AsReference)
                    }

                    else -> {
                        null
                    }
                }
                if (setting != null) {
                    settings.add(setting)
                }
            }
            return settings
        }
    }

    override fun toString(): String {
        return value.toString()
    }
}

class SparseSetting(value: Sparse) : SerializerSetting<Sparse>(value, SerializerSettingTarget.STRATEGY)
class IdSetting(value: Id) : SerializerSetting<Id>(value, SerializerSettingTarget.FIELD)
class WriteStrategySetting(value: UseWriteStrategy) : SerializerSetting<UseWriteStrategy>(value, SerializerSettingTarget.STRATEGY)
class ReadStrategySetting(value: UseReadStrategy) : SerializerSetting<UseReadStrategy>(value, SerializerSettingTarget.STRATEGY)
class CreateStrategySetting(value: UseCreateStrategy) : SerializerSetting<UseCreateStrategy>(value, SerializerSettingTarget.STRATEGY)
class ReferenceSetting(value: AsReference) : SerializerSetting<AsReference>(value, SerializerSettingTarget.STRATEGY)
class RecursiveReferenceSetting(value: AsReferenceRecursive) : SerializerSetting<AsReferenceRecursive>(value, SerializerSettingTarget.STRATEGY)
class InternalRecurseSetting(value: AsReferenceRecursive) : SerializerSetting<AsReferenceRecursive>(value, SerializerSettingTarget.STRATEGY)