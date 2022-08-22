package gov.cdc.prime.router.common

import org.apache.commons.lang3.StringUtils

/**
 * A collection of string utilities
 */
object StringUtilities {
    /**
     * A wrapper around the Apache trimToNull function that allows us to call it
     * as an extension method. This is just more of a convenience feature, but it
     * was suggested as part of the DateUtilities changes.
     *
     * If the string value is null, it returns null. If the string value is blank
     * or empty string, it converts it to null, otherwise it trims trailing whitespace.
     */
    fun String?.trimToNull(): String? {
        if (this == null) return null

        return StringUtils.trimToNull(this)
    }

    /**
     * A wrapper around toIntOrNull
     * utility to improve readability when parsing params
     *
     * usage:
     * request.queryParameters['foo']?.toIntOrDefault(30)
     */
    fun String?.toIntOrDefault(default: Int = 0): Int {
        return this?.toIntOrNull() ?: default
    }
}