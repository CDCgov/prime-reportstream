package gov.cdc.prime.router.common

import com.github.javafaker.Faker

/**
 * Utility functions for National Provider Identifier or NPI. NPI are numbers assigned by the CMS
 * to health providers, individuals and organizations.
 */
class NPIUtilities {
    companion object {
        /**
         * A valid test NPI from the NPIcheckdigit.pdf document.
         */
        const val VALID_NPI = "1234567893"

        // NPIs are always 10 digits and start with a 1 or 2
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
            val expectedCheck = calculateCheckDigit(main)
            val actualCheck = trimmedId.substring(9)
            return expectedCheck == actualCheck
        }

        /**
         * Generate a random NPI that passes the [isValidNPI] test.
         * Optionally, use the passed in [faker] to avoid creating another instance of this heavy-weight object.
         */
        fun generateRandomNPI(faker: Faker = Faker()): String {
            val main = faker.numerify("1########")
            return main + calculateCheckDigit(main)
        }

        /**
         * Calculate a check digit for an NPI per the CMS spec.
         * https://www.cms.gov/Regulations-and-Guidance/Administrative-Simplification/NationalProvIdentStand/Downloads/NPIcheckdigit.pdf
         */
        internal fun calculateCheckDigit(id: String): String {
            if (id.length != 9) error("$id is not a valid NPI main part")
            val inputDigits = id.map { it.digitToInt() }
            // double the even digits and then add the result back into odd digits
            val evenDigitsDoubled = inputDigits.flatMapIndexed { index: Int, digit: Int ->
                if (index % 2 == 0) {
                    (digit * 2).toString().map { it.digitToInt() }
                } else {
                    listOf(digit)
                }
            }
            // Add a constant of 24 because... read the paper.
            val step2 = (listOf(24) + evenDigitsDoubled).sum()
            // From the paper...
            // Subtract the total obtained in step 2 from the next higher number ending in zero, this is the check digit.
            // If the number obtained in step 2 ends in zero, the check digit is zero.
            val lastDigit = step2 % 10
            return if (lastDigit == 0) "0" else (10 - lastDigit).toString()
        }
    }
}