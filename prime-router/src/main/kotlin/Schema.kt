package gov.cdc.prime.router

import gov.cdc.prime.router.metadata.LIVDLookupMapper

/**
 * A PRIME schema contains a collection of data elements, mappings, validations and standard algorithms
 * needed to translate data to the form that a public health authority needs.
 *
 * Schemas can be either be `basedOn` another schema or `extend` another schema. If
 * a schema is `basedOn` another schema, the elements of the schema that match
 * the elements of the base schema inherit their properties. The `basedOn` feature is
 * useful when defining a state CSV schema because it allows the state schema to only specify the names
 * of the elements it wants and the unique properties of that element. For example:
 *
 * <code>
 * - name: state_schema
 *   basedOn: covid-19 # means all elements inherit properties from this schema
 *   elements:
 *   - name: patient_first_name
 *     csvFields: [{name: PatientFirstName }] # adds a property that is not in the base schema
 * </code>
 *
 * Schemas can also have a `extends` relationship with another schema. Like a `basedOn` relationship,
 * the elements of a schema inherit from the `extends` schema. In addition, all the elements of the `extends`
 * schema are appended to the schema. The `extends` relationship is useful in building HL7 schemas, because
 * these schema mostly have a common element set.
 *
 * @param name is the unique name of the schema
 * @param topic defines the set of schemas that can translate with each other
 * @param elements are the data elements of the schema
 * @param trackingElement is the element that is unique per item and is used for tracking
 * @param description is the text description of the schema
 * @param referenceUrl is a link to a standard document used to create the schema
 * @param extends is a name of a schema that this schema extends.
 * @param basedOn is a name of a schema that this schema is based on
 */
data class Schema(
    val name: String,
    val topic: Topic,
    val elements: List<Element> = emptyList(),
    val trackingElement: String? = null, // the element to use for tracking this test
    val description: String? = null,
    val referenceUrl: String? = null,
    val extends: String? = null,
    val extendsRef: Schema? = null,
    val basedOn: String? = null,
    val basedOnRef: Schema? = null,
) {
    constructor(
        vararg varElements: Element,
        name: String,
        topic: Topic,
        trackingElement: String? = null, // the element to use for tracking this test
        description: String? = null,
        referenceUrl: String? = null,
        extends: String? = null,
        basedOn: String? = null
    ) : this(
        name = name,
        topic = topic,
        elements = varElements.toList(),
        trackingElement = trackingElement,
        description = description,
        referenceUrl = referenceUrl,
        extends = extends,
        extendsRef = null,
        basedOn = basedOn,
        basedOnRef = null,
    )

    val baseName: String get() = formBaseName(name)
    val csvFields: List<Element.CsvField> get() = elements.flatMap { it.csvFields ?: emptyList() }

    private val elementIndex: Map<String, Int> = elements.mapIndexed { index, element -> element.name to index }.toMap()

    fun findElement(name: String): Element? {
        return elementIndex[name]?.let { elements[it] }
    }

    fun findElementColumn(name: String): Int? {
        return elementIndex[name]
    }

    fun findElementByCsvName(name: String): Element? {
        return elements.firstOrNull { e ->
            e.csvFields?.map { c -> c.name.lowercase() }?.contains(name.lowercase()) ?: false
        }
    }

    fun containsElement(name: String): Boolean {
        return elementIndex[name] != null
    }

    fun filterCsvFields(block: (Element) -> Boolean): List<Element.CsvField> {
        return elements.filter(block).flatMap { it.csvFields ?: emptyList() }
    }

    /**
     * Order the elements in this schema so elements that are dependencies to another elements mapper
     * are first.
     * @return the ordered element list
     */
    internal fun orderElementsByMapperDependencies(): List<Element> {
        // Start with all elements that DO NOT have a mapper as completed.
        val orderedElements = elements.filter { it.mapperRef == null }.toMutableList()
        val unorderedElements = elements.filter { it.mapperRef != null }.toMutableList()

        while (unorderedElements.isNotEmpty()) {
            // This is to keep track of how many elements we ordered in one run
            val unorderedElementsLeft = unorderedElements.size

            // Iterate through the list, so we can remove items without having a concurrent modification exception
            with(unorderedElements.iterator()) {
                forEach { element ->
                    val args = element.mapperArgs ?: emptyList()
                    val valueNames = element.mapperRef?.valueNames(element, args)
                    var hasAllDependencies = true
                    valueNames?.forEach { valueName ->
                        // Look through all values names that are elements in the schema and not itself.
                        if (valueName != element.name) findElement(valueName)?.let {
                            if (!orderedElements.contains(it)) hasAllDependencies = false
                        }
                    }
                    if (hasAllDependencies) {
                        orderedElements.add(element)
                        remove()
                    }
                }
            }

            // If the number of unordered elements did not change then we can no longer order the rest of them
            if (unorderedElementsLeft == unorderedElements.size) {
                break
            }
        }

        // Try to order any last mappers by the order the LIVD lookup mapper looks at elements
        val livdElementNames = LIVDLookupMapper().valueNames(Element("dummy"), emptyList())
        livdElementNames.forEach { livdElementName ->
            unorderedElements.firstOrNull { it.name == livdElementName }?.also {
                orderedElements.add(it)
                unorderedElements.remove(it)
            }
        }

        // Last, add any mappers we were not able to order at the end.
        if (unorderedElements.isNotEmpty()) {
            orderedElements.addAll(unorderedElements)
        }

        // Paranoia test.
        check(orderedElements.size == elements.size)
        return orderedElements
    }

    /**
     * Determine the values for the elements on this schema.  This function checks if a
     * mapper needs to be run or if a default needs to be applied.
     * @param allElementValues the values for all other elements which are updated as needed
     * @param errors errors will be added to this list
     * @param warnings warnings will be added to this list
     * @param defaultOverrides element name and value pairs of defaults that override schema defaults
     * @param itemIndex the index of the item from a report being processed
     * @param sender Sender who submitted the data.  Can be null if called at a point in code where its not known
     */
    fun processValues(
        allElementValues: MutableMap<String, String>,
        errors: MutableList<ActionLogDetail>,
        warnings: MutableList<ActionLogDetail>,
        defaultOverrides: Map<String, String> = emptyMap(),
        itemIndex: Int,
        sender: Sender? = null,
        specialFailureValue: String? = null
    ) {
        orderElementsByMapperDependencies().forEach { element ->
            // Do not process any field that had an error
            if (specialFailureValue == null || allElementValues[element.name] != specialFailureValue) {
                val mappedResult =
                    element.processValue(allElementValues, this, defaultOverrides, itemIndex, sender)
                allElementValues[element.name] = mappedResult.value ?: ""
                errors.addAll(mappedResult.errors)
                warnings.addAll(mappedResult.warnings)
            } else {
                allElementValues[element.name] = ""
            }
        }
    }

    /**
     * Validate all the elements in the schema.
     * @return a list of error messages, or an empty list if no errors
     */
    fun validate(): MutableList<String> {
        val validationErrors = mutableListOf<String>()
        elements.forEach { element ->
            validationErrors.addAll(element.validate().map { "Schema $name: $it" })
        }
        return validationErrors
    }

    companion object {
        fun formBaseName(name: String): String {
            if (!name.contains("/")) return name
            return name.split("/").last()
        }
    }
}