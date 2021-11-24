package gov.cdc.prime.router.common

import com.github.javafaker.Faker

/**
 * Utility functions for National Provider Identifier or NPI. NPI are numbers assigned by the CMS
 * to health providers, individuals and organizations.
 */
class NPIUtilities {
    companion object {
        // NPIs are always 10 digits and starting with a 1 or 2
        private const val MAIN_REGEX = """(1|2)\d{8}"""
        private const val MAIN_PLUS_CHECK_REGEX = """^(1|2)\d{9}$"""
        private val npiRegex = Regex(MAIN_PLUS_CHECK_REGEX)

        /**
         * Check that the [id] string is formatted like an NPI and that it's check digit
         * is valid
         */
        fun isValidNPI(id: String): Boolean {
            val trimmedId = id.trim()
            if (!npiRegex.containsMatchIn(trimmedId)) return false
            val main = trimmedId.substring(0..8)
            val expectedCheck = calcCheckDigit(main)
            val actualCheck = trimmedId.substring(9)
            return expectedCheck == actualCheck
        }

        /**
         * Generate a random NPI that passes the [isValidNPI] test.
         * Optionally, use the passed in [faker] to avoid creating another instance of this heavy-weight object.
         */
        fun generateRandomNPI(faker: Faker = Faker()): String {
            val main = faker.regexify(MAIN_REGEX)
            return main + calcCheckDigit(main)
        }

        /**
         * Calculate a check digit for an NPI per the CMS spec.
         * https://www.cms.gov/Regulations-and-Guidance/Administrative-Simplification/NationalProvIdentStand/Downloads/NPIcheckdigit.pdf
         */
        internal fun calcCheckDigit(id: String): String {
            val inputDigits = id.map { it.digitToInt() }
            val evenDigitsDoubled = inputDigits.flatMapIndexed { index: Int, digit: Int ->
                if (index % 2 == 0) {
                    // double the even digits and then split the result back into digits
                    (digit * 2).toString().map { it.digitToInt() }
                } else {
                    listOf(digit)
                }
            }
            // Add a constant of 24 because... read the paper.
            val sum = (listOf(24) + evenDigitsDoubled).sum()
            return (10 - (sum % 10)).toString()
        }
    }
}