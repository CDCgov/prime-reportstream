package gov.cdc.prime.tsGeneratePlugin

import me.ntrrgc.tsGenerator.ClassTransformer
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

enum class JsonAnnotations(val fullName: String) {
    JSONIGNORE("com.fasterxml.jackson.annotation.JsonIgnore"),
    JSONIGNORETYPE("com.fasterxml.jackson.annotation.JsonIgnoreType")
}

class TypescriptClassTransformer(): ClassTransformer {
    override fun transformPropertyList(properties: List<KProperty<*>>, klass: KClass<*>): List<KProperty<*>> {
        val filteredProperties = properties.filter { property -> isPropertyOverride(property) || !isPropertyJsonIgnored(property) }
        return super.transformPropertyList(filteredProperties, klass)
    }

    /**
     * If property is a JsonIgnored override, set type to Void.
     * Default type mappings will convert this to undefined.
     */
    override fun transformPropertyType(type: KType, property: KProperty<*>, klass: KClass<*>): KType {
        if(isPropertyOverride(property) && isPropertyJsonIgnored(property)){
            return super.transformPropertyType(Nothing::class.createType(), property, klass)
        }
        return super.transformPropertyType(type, property, klass)
    }

    private fun isPropertyJsonIgnored(property: KProperty<*>): Boolean {
        return isAnnotationPresent(property.javaField?.annotations, JsonAnnotations.JSONIGNORE.fullName) ||
            isAnnotationPresent(property.javaField?.type?.annotations, JsonAnnotations.JSONIGNORETYPE.fullName) ||
            isAnnotationPresent(property.getter.javaMethod?.annotations, JsonAnnotations.JSONIGNORE.fullName)
    }

    private fun isPropertyOverride(property: KProperty<*>): Boolean {
        val superclass = property.javaField?.declaringClass?.superclass
        val interfaces = property.javaField?.declaringClass?.interfaces

        return superclass?.kotlin?.declaredMembers?.any { it.name == property.name } ?: false ||
            interfaces?.any { it.kotlin.declaredMembers.any { member -> member.name == property.name }} ?: false
    }

    /**
     * Search through annotations by full qualified name
     */
    private fun isAnnotationPresent(annotations: Array<Annotation>?, annotationName: String): Boolean {
        return annotations?.any { it.annotationClass.qualifiedName == annotationName } ?: false
    }
}