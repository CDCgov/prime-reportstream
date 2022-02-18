package gov.cdc.prime.router.common

/**
 * String Utilities function.
 */
class StringUtilities {
    companion object {

        // Takes a nullable String and trims it down to null if the string is empty
        fun String?.trimToNull(): String? {
            if (this?.isEmpty() == true) return null
            return this
        }
    }
}