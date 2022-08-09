package gov.cdc.prime.router.tokens

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.FullELRSender
import gov.cdc.prime.router.Sender
import kotlin.test.Test

class ScopeTests {

    @Test
    fun `test isPrimeAdmin`() {
        assertThat(Scope.isPrimeAdmin("*.*.primeadmin")).isTrue()
        assertThat(Scope.isPrimeAdmin(null)).isFalse()
        assertThat(Scope.isPrimeAdmin("")).isFalse()
        assertThat(Scope.isPrimeAdmin("primeadmin")).isFalse()
        assertThat(Scope.isPrimeAdmin("*.*.*")).isFalse()
        assertThat(Scope.isPrimeAdmin("*.*.admin")).isFalse()
    }

    @Test
    fun `test toDetailedScope`() {
        assertThat(Scope.Companion.DetailedScope.toDetailedScope("")).isNull()
        assertThat(Scope.Companion.DetailedScope.toDetailedScope(" ")).isNull()
        assertThat(Scope.Companion.DetailedScope.toDetailedScope("x")).isNull()
        assertThat(Scope.Companion.DetailedScope.toDetailedScope("*")).isNull()
        assertThat(Scope.Companion.DetailedScope.toDetailedScope("report"))
            .isEqualTo(Scope.Companion.DetailedScope.Report)
        assertThat(Scope.Companion.DetailedScope.toDetailedScope("primeadmin"))
            .isEqualTo(Scope.Companion.DetailedScope.PrimeAdmin)
        assertThat(Scope.Companion.DetailedScope.toDetailedScope("PrimeAdmin")).isNull()
    }

    @Test
    fun `test isWellFormedScope`() {
        assertThat(Scope.isWellFormedScope("")).isFalse()
        assertThat(Scope.isWellFormedScope("a")).isFalse()
        assertThat(Scope.isWellFormedScope("abc")).isFalse()
        assertThat(Scope.isWellFormedScope("a b c")).isFalse()
        assertThat(Scope.isWellFormedScope("a.b")).isFalse()
        assertThat(Scope.isWellFormedScope("..")).isFalse()
        assertThat(Scope.isWellFormedScope("a.b.")).isFalse()
        assertThat(Scope.isWellFormedScope(".a.b")).isFalse()
        assertThat(Scope.isWellFormedScope("a..c")).isFalse()
        assertThat(Scope.isWellFormedScope("a.b")).isFalse()
        assertThat(Scope.isWellFormedScope("a.b.c a.b.c")).isFalse()
        assertThat(Scope.isWellFormedScope("a.b b.c")).isFalse()
        assertThat(Scope.isWellFormedScope("a\ta.b.c")).isFalse()
        assertThat(
            Scope.isWellFormedScope(
                """a.b.c
             c"""
            )
        ).isFalse()
        assertThat(Scope.isWellFormedScope("a.b.c")).isTrue()
        assertThat(Scope.isWellFormedScope("*.*.*")).isTrue()
    }

    @Test
    fun `test isValidScope`() {
        assertThat(Scope.isValidScope("")).isFalse()
        assertThat(Scope.isValidScope("a.b.c a.b.c")).isFalse()
        assertThat(Scope.isValidScope("*.*.foo")).isFalse()
        assertThat(Scope.isValidScope("*.*.*")).isFalse()
        assertThat(Scope.isValidScope("a.b.c")).isFalse()
        assertThat(Scope.isValidScope("*.*.foo")).isFalse()
        assertThat(Scope.isValidScope("md-phd.default.foo")).isFalse()
        assertThat(Scope.isValidScope("*.*.report")).isTrue()
        assertThat(Scope.isValidScope("*.*.primeadmin")).isTrue()
        assertThat(Scope.isValidScope("md-phd.default.admin")).isTrue()
    }

    @Test
    fun `test isValidScope with expectedSender`() {
        val sender1 = FullELRSender(name = "mySender", organizationName = "myOrg", Sender.Format.CSV)
        assertThat(Scope.isValidScope("", sender1)).isFalse()
        assertThat(Scope.isValidScope("myOrg..", sender1)).isFalse()
        assertThat(Scope.isValidScope("NotMyOrg.mySender.admin", sender1)).isFalse()
        assertThat(Scope.isValidScope("myOrg.mySender.NotValid", sender1)).isFalse()
        assertThat(Scope.isValidScope("myOrg..admin", sender1)).isFalse()
        assertThat(Scope.isValidScope(".mySender.admin", sender1)).isFalse()
        assertThat(Scope.isValidScope("myOrg.mySender.admin", sender1)).isTrue()
        assertThat(Scope.isValidScope("myOrg.mySender.user", sender1)).isTrue()
        assertThat(Scope.isValidScope("myOrg.mySender.report", sender1)).isTrue()
        // A match on sender name is not required.
        assertThat(Scope.isValidScope("myOrg.notMySender.admin", sender1)).isTrue()
    }

    @Test
    fun `test parseScope`() {
        assertThat(Scope.parseScope("")).isNull()
        assertThat(Scope.parseScope("a.b.wrong")).isNull()
        assertThat(Scope.parseScope("foobar")).isNull()
        assertThat(Scope.parseScope("a.b.report"))
            .isEqualTo(Triple("a", "b", Scope.Companion.DetailedScope.Report))
        assertThat(Scope.parseScope("*.*.primeadmin"))
            .isEqualTo(Triple("*", "*", Scope.Companion.DetailedScope.PrimeAdmin))
    }

    @Test
    fun `test authorized`() {
        assertThat(Scope.authorized(emptySet(), emptySet())).isFalse()
        val scopes1 = setOf(" ", "\t", "")
        assertThat(Scope.authorized(scopes1, emptySet())).isFalse()
        assertThat(Scope.authorized(emptySet(), scopes1)).isFalse()
        assertThat(Scope.authorized(scopes1, scopes1)).isFalse()
        val scopes2 = setOf("a", "b", " ", "\t", "")
        assertThat(Scope.authorized(scopes1, scopes2)).isFalse()
        assertThat(Scope.authorized(scopes2, emptySet())).isFalse()
        assertThat(Scope.authorized(emptySet(), scopes2)).isFalse()
        assertThat(Scope.authorized(scopes2, scopes2)).isTrue()
        val scopes3 = setOf("a", "b", "c")
        assertThat(Scope.authorized(scopes3, scopes3)).isTrue()
        assertThat(Scope.authorized(scopes2, scopes3)).isTrue()
        assertThat(Scope.authorized(scopes3, scopes2)).isTrue()
        val scopes4 = setOf("d")
        assertThat(Scope.authorized(scopes4, scopes3)).isFalse()
        assertThat(Scope.authorized(scopes3, scopes4)).isFalse()
        val scopes5 = setOf("c")
        assertThat(Scope.authorized(scopes5, scopes3)).isTrue()
        assertThat(Scope.authorized(scopes3, scopes5)).isTrue()
    }

    @Test
    fun `test scopeListContainsScope`() {
        assertThat(Scope.scopeListContainsScope("a", "a")).isTrue()
        assertThat(Scope.scopeListContainsScope("a:b c:d e:f", "a:b")).isTrue()
        assertThat(Scope.scopeListContainsScope("a:b c:d e:f", "a:b ")).isFalse()
        assertThat(Scope.scopeListContainsScope("", "")).isFalse()
        assertThat(Scope.scopeListContainsScope("xx", "x")).isFalse()
        assertThat(Scope.scopeListContainsScope("x   x", "x")).isTrue()
        assertThat(Scope.scopeListContainsScope("x   x", "")).isFalse()
        assertThat(Scope.scopeListContainsScope("x   x", " ")).isFalse()
    }
}