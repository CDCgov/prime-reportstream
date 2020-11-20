package gov.cdc.prime.router

/**
 * A *Mapper* is defined as a property of a schema element. For example
 *
 *  `mapper: middleInitial(standard.patient_middle_name)`
 *
 * A mapper object is stateless. It has a name which corresponds to
 * the function name in the property. It has a set of arguments, which
 * corresponding to the arguments of the function. Before applying the
 * mapper, the elementName list is generated from the arguments. All element values
 * are then fetched and provided to the apply function.
 */
interface Mapper {
    val name: String
    fun elementNames(args: List<String>): List<String>
    fun apply(args: List<String>, values: Map<String, String>): String?
}

class MiddleInitialMapper : Mapper {
    override val name = "middleInitial"

    override fun elementNames(args: List<String>): List<String> {
        if (args.size != 1) error("Invalid number of arguments")
        return args
    }

    override fun apply(args: List<String>, values: Map<String, String>): String? {
        if (values.size != 1) error("Didn't find the right number of values")
        return values.values.first().substring(0..0).toUpperCase()
    }
}

class UseMapper : Mapper {
    override val name = "use"

    override fun elementNames(args: List<String>) = args

    override fun apply(args: List<String>, values: Map<String, String>): String? {
        return if (values.isEmpty()) {
            null
        } else {
            values.values.first()
        }
    }
}

class IfPresentMapper : Mapper {
    override val name = "ifPresent"

    override fun elementNames(args: List<String>): List<String> {
        if (args.size != 2) error("Expect dependency and value parameters")
        return args.subList(0, 1)
    }

    override fun apply(args: List<String>, values: Map<String, String>): String? {
        return if (values.containsKey(args[0])) {
            return args[1]
        } else {
            null
        }
    }
}

object Mappers {
    fun parseMapperField(field: String): Pair<String, List<String>> {
        val match = Regex("([a-zA-Z0-9]+)\\x28([a-z, \\x2E_\\x2DA-Z0-9]*)\\x29").find(field)
            ?: error("Mapper field $field does not parse")
        return match.groupValues[1] to match.groupValues[2].split(',').map { it.trim() }
    }
}

