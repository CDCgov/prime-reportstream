package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class HistoryFunctionsTests {

    @Test
    fun `test isAuthorizedIgnoreDashes`() {
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(emptyList<String>(), "md-phd")).isFalse()
        var oktaOrgs = listOf<String?>("DHPrimeAdmins")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "")).isFalse()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "foo")).isTrue()
        oktaOrgs = listOf("DHPrimeAdmins", "DHmd-phd")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "")).isFalse()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "foo")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd")).isTrue()
        oktaOrgs = listOf(null, "DHmd-phd")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "foo")).isFalse()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd")).isTrue()
        oktaOrgs = listOf("DHmd-phd")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md_phd")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd-")).isFalse()
        oktaOrgs = listOf("DHmd_phd")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md_phd")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd-")).isFalse()
        oktaOrgs = listOf("DHfoobar")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "foobar")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "FOOBAR")).isFalse()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "foo")).isFalse()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "PrimeAdmin")).isFalse()
    }
}