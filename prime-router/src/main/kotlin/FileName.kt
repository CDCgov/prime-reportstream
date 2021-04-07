package gov.cdc.prime.router

interface FileNameElement {
    val name: String

    fun getElementValue(args: List<String> = emptyList(), translatorConfig: TranslatorConfiguration? = null): String
}

class Literal : FileNameElement {
    override val name = "literal"

    override fun getElementValue(args: List<String>, translatorConfig: TranslatorConfiguration?): String {
        return args.getOrElse(0) { "" }
    }
}

class ReceivingOrganization : FileNameElement {
    override val name = "receivingOrganization"

    override fun getElementValue(args: List<String>, translatorConfig: TranslatorConfiguration?): String {
        val hl7Config = translatorConfig as? Hl7Configuration?
        return hl7Config?.receivingOrganization ?: ""
    }
}

open class FileName(val elements: List<String>) {
    fun getFileName(
        translatorConfig: TranslatorConfiguration? = null
    ): String {
        val parsedElements = fixupFileNameElements(elements)
        return parsedElements.joinToString {
            val (elem, args) = it
            elem.getElementValue(
                args,
                translatorConfig
            )
        }
    }

    companion object {
        private val fileNameElements: List<FileNameElement> = listOf(
            Literal(),
            ReceivingOrganization()
        )

        private fun findFileNameElement(elementName: String): FileNameElement {
            return fileNameElements.first {
                it.name == elementName
            }
        }

        fun parseFileNameElement(fileNameElement: String): Pair<String, List<String>> {
            // Using a permissive match in the (arg1, arg2) section, to allow most regexes to be passed as args.
            // Somehow this works with "(?i).*Pima.*", I guess because the \\x29 matches rightmost ')' char
            val match = Regex("([a-zA-Z0-9]+)\\x28(.*)\\x29").find(fileNameElement)
                ?: error("FileNameElement field $fileNameElement does not parse")
            return match.groupValues[1] to match.groupValues[2].split(',').map { it.trim() }
        }

        fun fixupFileNameElements(elements: List<String>): List<Pair<FileNameElement, List<String>>> {
            return elements.map {
                val parsedElement = parseFileNameElement(it)
                Pair(findFileNameElement(parsedElement.first), parsedElement.second)
            }
        }
    }
}