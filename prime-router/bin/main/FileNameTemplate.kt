package gov.cdc.prime.router

import org.apache.commons.lang3.StringUtils
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Random
import java.util.UUID

// a file name element should be a pure function, which does not modify
// its inputs, and has a minimum amount of side effects (and by minimum) we
// mean ideally 0.
//
// ALSO, these elements should not fail. If the value doesn't exist
// or the arguments are empty, or the translation config is null, or any
// of the inputs are bad, it should return an empty string and move on.
// We do not want to fail the delivery of a file at this point for something
// like this.
interface FileNameElement {
    val name: String

    fun getElementValue(args: List<String> = emptyList(), translatorConfig: TranslatorConfiguration? = null): String
}

// a literal call, which just returns the first argument passed in
// this is strictly not necessary, as the FileName logic below will actually
// just pass through any bare strings it is sent, but maybe you want to make
// it abundantly clear that you intend this value to be a literal value
// and not some function call
class Literal : FileNameElement {
    override val name = "literal"

    override fun getElementValue(args: List<String>, translatorConfig: TranslatorConfiguration?): String {
        return args.getOrElse(0) { "" }
    }
}

// some of the receivers give us a value for the receiving organization. this is a standard
// from the APHL file name format, and that comes from the translation config
class ReceivingOrganization : FileNameElement {
    override val name = "receivingOrganization"

    override fun getElementValue(args: List<String>, translatorConfig: TranslatorConfiguration?): String {
        val hl7Config = translatorConfig as? Hl7Configuration?
        return hl7Config?.receivingOrganization ?: ""
    }
}

class FileUuid : FileNameElement {
    override val name: String
        get() = "uuid"

    override fun getElementValue(args: List<String>, translatorConfig: TranslatorConfiguration?): String {
        return if (args.isEmpty()) {
            UUID.randomUUID().toString()
        } else {
            args[0]
        }
    }
}

class RegexReplace : FileNameElement {
    override val name = "regexReplace"

    override fun getElementValue(args: List<String>, translatorConfig: TranslatorConfiguration?): String {
        if (args.count() < 3) return ""
        val lookupValue = args[0]
        val regex = args[1]
        val replacement = args[2]

        val re = Regex(regex)
        return re.replace(lookupValue, replacement)
    }
}

class SchemaBaseName : FileNameElement {
    override val name = "schemaBaseName"

    override fun getElementValue(args: List<String>, translatorConfig: TranslatorConfiguration?): String {
        return Schema.formBaseName(translatorConfig?.schemaName ?: "")
    }
}

class ProcessingModeCode : FileNameElement {
    override val name: String get() = "processingModeCode"

    override fun getElementValue(args: List<String>, translatorConfig: TranslatorConfiguration?): String {
        val hl7Config = translatorConfig as? Hl7Configuration?
        return when (hl7Config?.processingModeCode?.lowercase()) {
            "p" -> "production"
            "d" -> "development"
            else -> "testing"
        }
    }
}

// returns the created date according to the provided format OR the format passed in.
// if the format fails, it just returns an empty string and moves on
class CreatedDate : FileNameElement {
    private val defaultFormat = "yyyyMMddHHmmss"

    override val name = "createdDate"

    override fun getElementValue(args: List<String>, translatorConfig: TranslatorConfiguration?): String {
        return try {
            val pattern = if (args.isEmpty() || args[0].isEmpty()) {
                defaultFormat
            } else {
                args[0]
            }
            val formatter = DateTimeFormatter.ofPattern(pattern)
            val dt = OffsetDateTime.now()
            formatter.format(dt)
        } catch (_: Exception) {
            ""
        }
    }
}

/**
 * Use to add a bit randomness to a file name. Like UUID, helpful to generate unique names.
 */
class Rand6 : FileNameElement {
    override val name = "rand6"

    override fun getElementValue(args: List<String>, translatorConfig: TranslatorConfiguration?): String {
        val value = Random().nextInt(1000000)
        return StringUtils.leftPad(value.toString(), 6, "0")
    }
}

open class FileNameTemplate(
    val elements: List<String>,
    val lowerCase: Boolean? = null,
    val upperCase: Boolean? = null,
    val name: String? = null
) {
    fun getFileName(
        translatorConfig: TranslatorConfiguration? = null,
        reportId: ReportId
    ): String {
        val fileName = StringBuilder()
        val parsedElements = fixupFileNameElements(elements)
        parsedElements.forEach {
            when (it.first) {
                is FileNameElement -> {
                    val e = it.first as FileNameElement
                    // if the file element type is the UUID, combine args
                    val args = if (e is FileUuid) {
                        listOf(reportId.toString())
                    } else {
                        it.second
                    }
                    fileName.append(e.getElementValue(args, translatorConfig))
                }
                is String -> {
                    fileName.append(it.first)
                }
                else -> {
                }
            }
        }
        return when {
            (lowerCase == true) -> fileName.toString().lowercase()
            (upperCase == true) -> fileName.toString().uppercase()
            else -> fileName.toString()
        }
    }

    companion object {
        // this is the collection of file name elements that can be invoked
        private val fileNameElements: List<FileNameElement> = listOf(
            Literal(),
            ReceivingOrganization(),
            CreatedDate(),
            RegexReplace(),
            FileUuid(),
            SchemaBaseName(),
            ProcessingModeCode(),
            Rand6()
        )

        private fun findFileNameElement(elementName: String): FileNameElement? {
            return fileNameElements.firstOrNull {
                it.name == elementName
            }
        }

        fun parseFileNameElement(fileNameElement: String): Pair<String, List<String>> {
            // Using a permissive match in the (arg1, arg2) section, to allow most regexes to be passed as args.
            // Somehow this works with "(?i).*Pima.*", I guess because the \\x29 matches rightmost ')' char
            // that said, this allows for the passing in of bare strings, so if the match fails, then we
            // just pass through a pair of the failing value to an empty list
            val match = Regex("([a-zA-Z0-9]+)\\x28(.*)\\x29").find(fileNameElement)
                ?: return fileNameElement to emptyList()
            return match.groupValues[1] to match.groupValues[2].split(',').map { it.trim() }
        }

        fun fixupFileNameElements(elements: List<String>): List<Pair<Any, List<String>>> {
            // given a list of elements in the file name template, try to cast each
            // to a function, or to just the literal string
            return elements.map {
                val parsedElement = parseFileNameElement(it)
                Pair(findFileNameElement(parsedElement.first) ?: it, parsedElement.second)
            }
        }
    }
}