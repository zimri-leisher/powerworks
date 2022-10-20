package serialization

import java.lang.reflect.Field
import java.net.URI

sealed class SerializerSetting<T>(val value: T) {

    companion object {
        val ALL = mutableListOf<SerializerSetting<*>>()

        fun getSettings(field: Field): List<SerializerSetting<*>> {
            val settings = mutableListOf<SerializerSetting<*>>()
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

class SparseSetting(value: Sparse) : SerializerSetting<Sparse>(value)
class IdSetting(value: Id) : SerializerSetting<Id>(value)
class WriteStrategySetting(value: UseWriteStrategy) : SerializerSetting<UseWriteStrategy>(value)
class ReadStrategySetting(value: UseReadStrategy) : SerializerSetting<UseReadStrategy>(value)
class CreateStrategySetting(value: UseCreateStrategy) : SerializerSetting<UseCreateStrategy>(value)
class ReferenceSetting(value: AsReference) : SerializerSetting<AsReference>(value)
class RecursiveReferenceSetting(value: AsReferenceRecursive) : SerializerSetting<AsReferenceRecursive>(value)
class InternalRecurseSetting(value: AsReferenceRecursive) : SerializerSetting<AsReferenceRecursive>(value)