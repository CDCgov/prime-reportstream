package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class NPIUtilitiesTests {
    @Test
    fun `test with known valid NPIs`() {
        assertThat(NPIUtilities.isValidNPI(NPIUtilities.VALID_NPI)).isTrue()
        // From NPPES
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
        val randomNpi = NPIUtilities.generateRandomNPI()
        assertThat(NPIUtilities.isValidNPI(randomNpi)).isTrue()
    }
}