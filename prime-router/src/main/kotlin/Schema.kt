package gov.cdc.prime.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FilenameFilter

// A PRIME schema contains a collection of data elements, mappings, validations and standard algorithms
// needed to translate data to the form that a public health authority needs
//
data class Schema(
    val name: String, // Name should include version
    val topic: String,
    val elements: List<Element> = emptyList(),
    val trackingElement: String? = null, // the element to use for tracking this test
    val description: String? = null,
) {
    // A mapping maps from one schema to another
    data class Mapping(
        val toSchema: Schema,
        val fromSchema: Schema,
        val useDirectly: Map<String, String>,
        val useValueSet: Map<String, String>,
        val useTranslator: Map<String, Translator>,
        val useDefault: Set<String>,
        val missing: Set<String>
    )

    fun findElement(name: String): Element? {
        return elements.find {
            (it.name == name) ||
                    // a CODE element can have 3 different names <name> or <name>#text or <name>#system depending
                    (it.isCode && (it.nameAsCode == name || it.nameAsCodeText == name || it.nameAsCodeSystem == name))
        }
    }

    fun buildMapping(toSchema: Schema): Mapping {
        if (toSchema.topic != this.topic) error("Trying to match schema with different topics")

        val useDirectly = mutableMapOf<String, String>()
        val useValueSet: MutableMap<String, String> = mutableMapOf<String, String>()
        val useTranslator = mutableMapOf<String, Translator>()
        val useDefault = mutableSetOf<String>()
        val missing = mutableSetOf<String>()

        toSchema.elements.forEach { toElement ->
            findMatchingElement(toElement)?.let {
                useDirectly[toElement.name] = it
                return@forEach
            }
            findMatchingValueSet(toElement)?.let {
                useValueSet[toElement.name] = it
            }
            findMatchingTranslator(toElement)?.let {
                useTranslator[toElement.name] = it
                return@forEach
            }
            if (toElement.required == true) {
                missing.add(toElement.name)
            } else {
                useDefault.add(toElement.name)
            }
        }
        return Mapping(toSchema, this, useDirectly, useValueSet, useTranslator, useDefault, missing)
    }

    private fun findMatchingElement(matchElement: Element): String? {
        return findElement(matchElement.name)?.name
    }

    private fun findMatchingTranslator(matchElement: Element): Translator? {
        return Metadata.findTranslator { translator ->
            translator.topic.equals(topic, ignoreCase = true)
                    && translator.toElement.equals(matchElement.name, ignoreCase = true)
                    && translator.fromElements.find { findElement(it) == null } == null
        }
    }

    private fun findMatchingValueSet(matchElement: Element): String? {
        return when (matchElement.type) {
            Element.Type.CODE -> {
                findElement(matchElement.name)?.valueSet
            }
            else -> null
        }
    }
}