package gov.cdc.prime.router.azure

import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.db.enums.SettingType
import gov.cdc.prime.router.azure.db.tables.pojos.Setting
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.spyk
import org.jooq.JSON
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingFacadeTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))

    val testOrg = Setting(
        1,
        SettingType.ORGANIZATION,
        "test",
        null,
        JSON.valueOf(
            """{"name":"test","description":"Arizona PHD","jurisdiction":""" +
                """"STATE","stateCode":"CA","countyName":null,"meta":null}")"""
        ),
        false,
        true,
        1,
        "todo",
        OffsetDateTime.now()
    )
    val defaultSender = Setting(
        2,
        SettingType.SENDER,
        "test",
        1,
        JSON.valueOf(
            """{"name":"default","organizationName":"test","format":"CSV","topic":"covid-19"""" +
                ""","schemaName":"primedatainput/pdi-covid-19","meta":null}"""
        ),
        false,
        true,
        4,
        "todo",
        OffsetDateTime.now()
    )
    val elrReceiver = Setting(
        3,
        SettingType.RECEIVER,
        "elr-test",
        1,
        JSON.valueOf(
            """{"name":"elr-test","organizationName":"test","topic":"covid-19","translation":""" +
                """{"schemaName":"az/az-covid-19","format":"CSV","defaults":{},"type":"CUSTOM"},""" +
                """"jurisdictionalFilter":["matches(ordering_facility_state,AZ)",""" +
                """"doesNotMatch(ordering_facility_name,Tucson Mountains,Tucson Foothills,Sierra Vista Canyons)"]""" +
                ""","deidentify":false,"timing":{"operation":"MERGE","numberPerDay":1440,""" +
                """"initialTime":"00:00","timeZone":"ARIZONA","maxReportCount":100},"description":"","transport":""" +
                """{"host":"sftp","port":"22","filePath":"./upload","type":"SFTP"},"meta":null}"""
        ),
        false,
        true,
        5,
        "todo",
        OffsetDateTime.now()
    )

    fun setupOrgDatabaseAccess() {
        every {
            accessSpy.fetchSetting(SettingType.ORGANIZATION, "test", null, any())
        }.returns(testOrg)
        every {
            accessSpy.fetchSetting(SettingType.ORGANIZATION, not("test"), parentId = null, any())
        }.returns(null)
        every {
            accessSpy.fetchSettings(SettingType.ORGANIZATION, txn = any())
        }.returns(listOf(testOrg))

        every {
            accessSpy.fetchSettings(SettingType.SENDER, txn = any())
        }.returns(listOf(defaultSender))
    }

    fun setupSenderDatabaseAccess() {
        setupOrgDatabaseAccess()
        every {
            accessSpy.fetchSetting(SettingType.SENDER, "default", "test", any())
        }.returns(defaultSender)
        every {
            accessSpy.fetchSetting(SettingType.SENDER, "default", 1, any())
        }.returns(defaultSender)
        every {
            accessSpy.fetchSetting(SettingType.SENDER, not("default"), parentId = any(), any())
        }.returns(null)
        every {
            accessSpy.fetchSetting(SettingType.SENDER, not("default"), organizationName = any(), any())
        }.returns(null)
    }

    fun setupReceiverDatabaseAccess() {
        setupOrgDatabaseAccess()
        every {
            accessSpy.fetchSettings(SettingType.RECEIVER, txn = any())
        }.returns(listOf(elrReceiver))
        every {
            accessSpy.fetchSetting(SettingType.RECEIVER, "elr-test", "test", any())
        }.returns(elrReceiver)
        every {
            accessSpy.fetchSetting(SettingType.RECEIVER, "elr-test", 1, any())
        }.returns(elrReceiver)
        every {
            accessSpy.fetchSetting(SettingType.RECEIVER, not("elr-test"), parentId = any(), any())
        }.returns(null)
        every {
            accessSpy.fetchSetting(SettingType.RECEIVER, not("elr-test"), organizationName = any(), any())
        }.returns(null)
    }

    fun testMetadata(): Metadata {
        return Metadata(
            schema = Schema("primedatainput/pdi-covid-19", "covid-19")
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `get orgs test`() {
        setupOrgDatabaseAccess()
        val list = SettingsFacade(testMetadata(), accessSpy).organizations
        assertEquals("test", list.first().name)
        assertEquals(Organization.Jurisdiction.STATE, list.first().jurisdiction)
    }

    @Test
    fun `get org test`() {
        setupOrgDatabaseAccess()
        val org = SettingsFacade(testMetadata(), accessSpy).findOrganization("test")
        assertEquals("test", org?.name)
        assertEquals(Organization.Jurisdiction.STATE, org?.jurisdiction)
    }

    @Test
    fun `get org test failure`() {
        setupOrgDatabaseAccess()
        val org = SettingsFacade(testMetadata(), accessSpy).findOrganization("foo")
        assertNull(org)
    }

    @Test
    fun `get senders test`() {
        setupSenderDatabaseAccess()
        val list = SettingsFacade(testMetadata(), accessSpy).senders
        assertEquals("default", list.first().name)
        assertEquals(Sender.Format.CSV, list.first().format)
    }

    @Test
    fun `get sender test`() {
        setupSenderDatabaseAccess()
        val sender = SettingsFacade(testMetadata(), accessSpy).findSender("test.default")
        assertEquals("default", sender?.name)
        assertEquals(Sender.Format.CSV, sender?.format)
    }

    @Test
    fun `get sender test failure`() {
        setupSenderDatabaseAccess()
        val sender = SettingsFacade(testMetadata(), accessSpy).findSender("test.foo")
        assertNull(sender)
    }

    @Test
    fun `get sender test failure2`() {
        setupSenderDatabaseAccess()
        val sender = SettingsFacade(testMetadata(), accessSpy).findSender("foo.foo")
        assertNull(sender)
    }

    @Test
    fun `get receivers test`() {
        setupReceiverDatabaseAccess()
        val list = SettingsFacade(testMetadata(), accessSpy).receivers
        assertEquals("elr-test", list.first().name)
        assertEquals(Report.Format.CSV, list.first().format)
    }

    @Test
    fun `get receiver test`() {
        setupReceiverDatabaseAccess()
        val receiver = SettingsFacade(testMetadata(), accessSpy).findReceiver("test.elr-test")
        assertEquals("elr-test", receiver?.name)
        assertEquals(Report.Format.CSV, receiver?.format)
    }
}