package serialization

import kotlin.reflect.KClass

/**
 * An annotation that tells [TaggedSerializer] what id to give this field when reading and writing it. There are no
 * constraints on [id]
 */
@Target(AnnotationTarget.FIELD)
annotation class Id(val id: Int)

@Target(AnnotationTarget.FIELD)
annotation class AsReference(val recursive: Boolean = false)

@Target(AnnotationTarget.FIELD)
annotation class Sparse

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
annotation class ObjectIdentifier

@Target(AnnotationTarget.FIELD)
annotation class ObjectList

@Target(AnnotationTarget.FIELD)
annotation class UseWriteStrategy(val writeStrategyClass: KClass<WriteStrategy<*>>)

@Target(AnnotationTarget.FIELD)
annotation class UseReadStrategy(val readStrategyClass: KClass<ReadStrategy<*>>)

@Target(AnnotationTarget.FIELD)
annotation class UseCreateStrategy(val createStrategyClass: KClass<CreateStrategy<*>>)

@Target(AnnotationTarget.CLASS)
annotation class TryToResolveReferences