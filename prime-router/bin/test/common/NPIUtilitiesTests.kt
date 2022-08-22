package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import kotlin.test.fail

class NPIUtilitiesTests {
    @Test
    fun `test with known valid NPIs`() {
        assertThat(NPIUtilities.isValidNPI(NPIUtilities.VALID_NPI)).isTrue()
        // Valid ID with a zero check digit
        assertThat(NPIUtilities.isValidNPI("1368219030")).isTrue()
        // Valid ID taken from NPPES
        assertThat(NPIUtilities.isValidNPI("1841374824")).isTrue()
        assertThat(NPIUtilities.isValidNPI(" 1457368953")).isTrue()
        assertThat(NPIUtilities.isValidNPI("1871554113")).isTrue()
        assertThat(NPIUtilities.isValidNPI("1265573299 ")).isTrue()
    }

    @Test
    fun `test with invalid NPIs`() {
        assertThat(NPIUtilities.isValidNPI("")).isFalse()
        assertThat(NPIUtilities.isValidNPI("-1")).isFalse()
        assertThat(NPIUtilities.isValidNPI("1265573298")).isFalse()
    }

    @Test
    fun `test against random NPI`() {
        // loop 30 times to likely generate cases with all 10 check digits
        for (i in 0..30) {
            val randomNpi = NPIUtilities.generateRandomNPI()
            val isValid = NPIUtilities.isValidNPI(randomNpi)
            if (!isValid) {
                fail("$randomNpi is does not test as a valid NPI. iteration = $i")
            }
        }
    }
}