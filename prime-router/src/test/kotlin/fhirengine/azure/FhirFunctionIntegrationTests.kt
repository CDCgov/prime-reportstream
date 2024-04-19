package fhirengine.azure

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.each
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.matchesPredicate
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables.ACTION_LOG
import gov.cdc.prime.router.azure.db.enums.ActionLogType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.ActionLog
import gov.cdc.prime.router.azure.db.tables.Task
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.ReportLineage
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.azure.FHIRFunctions
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import gov.cdc.prime.router.fhirengine.engine.FHIRTranslator
import gov.cdc.prime.router.fhirengine.engine.QueueMessage
import gov.cdc.prime.router.fhirengine.engine.elrRoutingQueueName
import gov.cdc.prime.router.fhirengine.engine.elrTranslationQueueName
import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.nio.charset.Charset
import java.time.OffsetDateTime
import java.util.UUID

private const val MULTIPLE_TARGETS_FHIR_PATH = "src/test/resources/fhirengine/engine/valid_data_multiple_targets.fhir"

private const val VALID_FHIR_PATH = "src/test/resources/fhirengine/engine/valid_data.fhir"

@Suppress("ktlint:standard:max-line-length")
private const val fhirRecord =
    """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""

@Suppress("ktlint:standard:max-line-length")
private const val codelessFhirRecord =
    """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""

@Suppress("ktlint:standard:max-line-length")
private const val bulkFhirRecord =
    """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}
    {"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c09876","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dbau8cd"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}
    {}
    {"resourceType":"Bund}"""

@Suppress("ktlint:standard:max-line-length")
private const val validFHIRRecord1 =
    """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""

@Suppress("ktlint:standard:max-line-length")
private const val validFHIRRecord2 =
    """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c09876","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dbau8cd"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""

private const val invalidEmptyFHIRRecord = "{}"

private const val invalidMalformedFHIRRecord = """{"resourceType":"Bund}"""

@Suppress("ktlint:standard:max-line-length")
private const val cleanHL7Record =
    """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.99^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|0cba76f5-35e0-4a28-803a-2f31308aae9b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|0cba76f5-35e0-4a28-803a-2f31308aae9b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600"""

@Suppress("ktlint:standard:max-line-length")
private const val cleanHL7RecordConverted =
    """{"resourceType":"Bundle","id":"1713206640202929000.2b9ac916-d794-442b-ab7a-cb8871203d35","meta":{"lastUpdated":"2024-04-15T14:44:00.214-04:00"},"identifier":{"system":"https://reportstream.cdc.gov/prime-router","value":"371784"},"type":"message","timestamp":"2021-02-10T17:07:37.000-05:00","entry":[{"fullUrl":"MessageHeader/4aeed951-99a9-3152-8885-6b0acc6dd35e","resource":{"resourceType":"MessageHeader","id":"4aeed951-99a9-3152-8885-6b0acc6dd35e","meta":{"tag":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0103","code":"P"}]},"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/msh-message-header","extension":[{"url":"MSH.7","valueString":"20210210170737"},{"url":"MSH.15","valueString":"NE"},{"url":"MSH.16","valueString":"NE"},{"url":"MSH.21","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"ELR_Receiver"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.9.99"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"PHLabReportNoAck"}}]}],"eventCoding":{"system":"http://terminology.hl7.org/CodeSystem/v2-0003","code":"R01","display":"ORU^R01^ORU_R01"},"destination":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.5"}],"name":"PRIME_DOH","_endpoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"receiver":{"reference":"Organization/1713206640321683000.ab77e6f3-fd1b-4c04-abb1-dff718bd9c5b"}}],"sender":{"reference":"Organization/1713206640290082000.5ca229fb-8f14-4e1a-bb54-eb353f8695e6"},"source":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CDC PRIME - Atlanta, Georgia (Dekalb)"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.237821"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.3"}],"software":"PRIME ReportStream","version":"0.1-SNAPSHOT","endpoint":"urn:oid:2.16.840.1.114222.4.1.237821"}}},{"fullUrl":"Organization/1713206640290082000.5ca229fb-8f14-4e1a-bb54-eb353f8695e6","resource":{"resourceType":"Organization","id":"1713206640290082000.5ca229fb-8f14-4e1a-bb54-eb353f8695e6","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"10D0876999"}],"address":[{"country":"USA"}]}},{"fullUrl":"Organization/1713206640321683000.ab77e6f3-fd1b-4c04-abb1-dff718bd9c5b","resource":{"resourceType":"Organization","id":"1713206640321683000.ab77e6f3-fd1b-4c04-abb1-dff718bd9c5b","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.6"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Prime ReportStream"}]}},{"fullUrl":"Provenance/1713206641036617000.f3bba4eb-777d-4a11-b0a0-17aac5bdfdb1","resource":{"resourceType":"Provenance","id":"1713206641036617000.f3bba4eb-777d-4a11-b0a0-17aac5bdfdb1","target":[{"reference":"MessageHeader/4aeed951-99a9-3152-8885-6b0acc6dd35e"},{"reference":"DiagnosticReport/1713206641338106000.553110e9-ea2b-493e-9ad4-10bb1c4a7ee1"}],"recorded":"2021-02-10T17:07:37Z","activity":{"coding":[{"display":"ORU^R01^ORU_R01"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"author"}]},"who":{"reference":"Organization/1713206641035511000.ce6ec747-634f-4728-a642-ef07b910e02c"}}],"entity":[{"role":"source","what":{"reference":"Device/1713206641041451000.6bc49932-c8ce-4955-a45e-3efb06b9a208"}}]}},{"fullUrl":"Organization/1713206641035511000.ce6ec747-634f-4728-a642-ef07b910e02c","resource":{"resourceType":"Organization","id":"1713206641035511000.ce6ec747-634f-4728-a642-ef07b910e02c","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"10D0876999"}]}},{"fullUrl":"Organization/1713206641041004000.38fcc890-f16e-4462-b989-d8c7576195d6","resource":{"resourceType":"Organization","id":"1713206641041004000.38fcc890-f16e-4462-b989-d8c7576195d6","name":"Centers for Disease Control and Prevention"}},{"fullUrl":"Device/1713206641041451000.6bc49932-c8ce-4955-a45e-3efb06b9a208","resource":{"resourceType":"Device","id":"1713206641041451000.6bc49932-c8ce-4955-a45e-3efb06b9a208","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org","valueReference":{"reference":"Organization/1713206641041004000.38fcc890-f16e-4462-b989-d8c7576195d6"}}],"manufacturer":"Centers for Disease Control and Prevention","deviceName":[{"name":"PRIME ReportStream","type":"manufacturer-name"}],"modelNumber":"0.1-SNAPSHOT","version":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-install-date","valueDateTime":"2021-02-10","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20210210"}]}}],"value":"0.1-SNAPSHOT"}]}},{"fullUrl":"Provenance/1713206641053143000.bedc4b41-1036-4464-804f-0c25c7ddb497","resource":{"resourceType":"Provenance","id":"1713206641053143000.bedc4b41-1036-4464-804f-0c25c7ddb497","recorded":"2024-04-15T14:44:01Z","policy":["http://hl7.org/fhir/uv/v2mappings/message-oru-r01-to-bundle"],"activity":{"coding":[{"code":"v2-FHIR transformation"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"assembler"}]},"who":{"reference":"Organization/1713206641052572000.4f130a6c-8504-45fc-9676-dbfa373a404f"}}]}},{"fullUrl":"Organization/1713206641052572000.4f130a6c-8504-45fc-9676-dbfa373a404f","resource":{"resourceType":"Organization","id":"1713206641052572000.4f130a6c-8504-45fc-9676-dbfa373a404f","identifier":[{"value":"CDC PRIME - Atlanta"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.237821"}]}},{"fullUrl":"Patient/1713206641092472000.9e168adf-be2b-49ed-968b-52ecff49e827","resource":{"resourceType":"Patient","id":"1713206641092472000.9e168adf-be2b-49ed-968b-52ecff49e827","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/pid-patient","extension":[{"url":"PID.8"},{"url":"PID.30","valueString":"N"}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cx-identifier","extension":[{"url":"CX.5","valueString":"PI"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"PID.3"}],"type":{"coding":[{"code":"PI"}]},"value":"2a14112c-ece1-4f82-915c-7b3a8d152eda","assigner":{"reference":"Organization/1713206641060230000.3f94d759-1f38-4965-98b5-235cbc384a40"}}],"name":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xpn-human-name","extension":[{"url":"XPN.2","valueString":"Kareem"},{"url":"XPN.3","valueString":"Millie"},{"url":"XPN.7","valueString":"L"}]}],"use":"official","family":"Buckridge","given":["Kareem","Millie"]}],"telecom":[{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"211"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"2240784"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.1","valueString":"7275555555:1:"},{"url":"XTN.2","valueString":"PRN"},{"url":"XTN.4","valueString":"roscoe.wilkinson@email.com"},{"url":"XTN.7","valueString":"2240784"}]}],"system":"email","use":"home"}],"gender":"female","birthDate":"1958-08-10","_birthDate":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"19580810"}]},"deceasedBoolean":false,"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"688 Leighann Inlet"}]}]}],"line":["688 Leighann Inlet"],"city":"South Rodneychester","district":"48077","state":"TX","postalCode":"67071"}]}},{"fullUrl":"Organization/1713206641060230000.3f94d759-1f38-4965-98b5-235cbc384a40","resource":{"resourceType":"Organization","id":"1713206641060230000.3f94d759-1f38-4965-98b5-235cbc384a40","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"}]}},{"fullUrl":"Provenance/1713206641096184000.19d932de-61b3-4b20-bf54-735fac7c3929","resource":{"resourceType":"Provenance","id":"1713206641096184000.19d932de-61b3-4b20-bf54-735fac7c3929","target":[{"reference":"Patient/1713206641092472000.9e168adf-be2b-49ed-968b-52ecff49e827"}],"recorded":"2024-04-15T14:44:01Z","activity":{"coding":[{"system":"https://terminology.hl7.org/CodeSystem/v3-DataOperation","code":"UPDATE"}]}}},{"fullUrl":"Observation/1713206641100610000.b700af08-de9f-4373-93f0-3270f5552bd7","resource":{"resourceType":"Observation","id":"1713206641100610000.b700af08-de9f-4373-93f0-3270f5552bd7","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/analysis-date-time","valueDateTime":"2021-02-09T00:00:00-06:00","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"},{"url":"OBX.17"}]}],"status":"final","subject":{"reference":"Patient/1713206641092472000.9e168adf-be2b-49ed-968b-52ecff49e827"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"performer":[{"reference":"Organization/1713206641102025000.bde730ff-b9c8-4aa2-ac4b-1397f0df4223"}],"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","code":"260415000","display":"Not detected"}]}}},{"fullUrl":"Organization/1713206641102025000.bde730ff-b9c8-4aa2-ac4b-1397f0df4223","resource":{"resourceType":"Organization","id":"1713206641102025000.bde730ff-b9c8-4aa2-ac4b-1397f0df4223","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"10D0876999"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.25"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CLIA"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.4.7"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"10D0876999"}],"name":"Avante at Ormond Beach","address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":["170 North King Road"],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}]}},{"fullUrl":"Observation/1713206641106291000.1d0cdff9-199d-44e4-8305-cebcca1da91c","resource":{"resourceType":"Observation","id":"1713206641106291000.1d0cdff9-199d-44e4-8305-cebcca1da91c","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","subject":{"reference":"Patient/1713206641092472000.9e168adf-be2b-49ed-968b-52ecff49e827"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"Y","display":"Yes"}]}}},{"fullUrl":"Observation/1713206641109549000.e3729214-40cb-4bb7-803a-3e9ff7560818","resource":{"resourceType":"Observation","id":"1713206641109549000.e3729214-40cb-4bb7-803a-3e9ff7560818","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","subject":{"reference":"Patient/1713206641092472000.9e168adf-be2b-49ed-968b-52ecff49e827"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"Y","display":"Yes"}]}}},{"fullUrl":"Observation/1713206641112754000.8e0bc613-0ea1-49d3-bfa0-00d1cda28f26","resource":{"resourceType":"Observation","id":"1713206641112754000.8e0bc613-0ea1-49d3-bfa0-00d1cda28f26","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","subject":{"reference":"Patient/1713206641092472000.9e168adf-be2b-49ed-968b-52ecff49e827"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"N","display":"No"}]}}},{"fullUrl":"Observation/1713206641115555000.7b8c6a2b-e833-49e9-a887-18b7d84a9508","resource":{"resourceType":"Observation","id":"1713206641115555000.7b8c6a2b-e833-49e9-a887-18b7d84a9508","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","subject":{"reference":"Patient/1713206641092472000.9e168adf-be2b-49ed-968b-52ecff49e827"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"N","display":"No"}]}}},{"fullUrl":"Specimen/1713206641315380000.d4d7e28b-6c35-4426-9dab-073761eb20ed","resource":{"resourceType":"Specimen","id":"1713206641315380000.d4d7e28b-6c35-4426-9dab-073761eb20ed","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"OBR"}]}},{"fullUrl":"Specimen/1713206641318070000.9ff57cd3-a79f-4f21-8120-9ffb6e4217ec","resource":{"resourceType":"Specimen","id":"1713206641318070000.9ff57cd3-a79f-4f21-8120-9ffb6e4217ec","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"SPM"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Component","valueString":"SPM.2.1"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PGN"}]},"value":"0cba76f5-35e0-4a28-803a-2f31308aae9b"}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","code":"258500001","display":"Nasopharyngeal swab"}]},"receivedTime":"2021-02-09T00:00:00-06:00","_receivedTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"collection":{"collectedDateTime":"2021-02-09T00:00:00-06:00","_collectedDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"bodySite":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"2020-09-01","code":"71836000","display":"Nasopharyngeal structure (body structure)"}]}}}},{"fullUrl":"ServiceRequest/1713206641332131000.5f636905-91f7-4605-93aa-a914687e2462","resource":{"resourceType":"ServiceRequest","id":"1713206641332131000.5f636905-91f7-4605-93aa-a914687e2462","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/business-event","valueCode":"RE"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/orc-common-order","extension":[{"url":"orc-21-ordering-facility-name","valueReference":{"reference":"Organization/1713206641326316000.09bfed54-b804-406d-b81e-99070fc009e5"}},{"url":"orc-22-ordering-facility-address","valueAddress":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":["170 North King Road"],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}},{"url":"orc-24-ordering-provider-address","valueAddress":{"postalCode":"32174"}},{"url":"orc-12-ordering-provider","valueReference":{"reference":"Practitioner/1713206641328586000.8e2d16a2-c07e-45d2-952b-91d5fc20a530"}},{"url":"ORC.15","valueString":"20210209"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request","extension":[{"url":"OBR.2","valueIdentifier":{"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}},{"url":"OBR.3","valueIdentifier":{"value":"0cba76f5-35e0-4a28-803a-2f31308aae9b"}},{"url":"OBR.22","valueString":"202102090000-0600"},{"url":"OBR.16","valueReference":{"reference":"Practitioner/1713206641330598000.c2dc264e-0746-4170-94e1-b960571b8d44"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"386"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"6825220"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.7","valueString":"6825220"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBR.17"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"use":"work"}}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}],"status":"unknown","subject":{"reference":"Patient/1713206641092472000.9e168adf-be2b-49ed-968b-52ecff49e827"},"requester":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"386"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"6825220"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.7","valueString":"6825220"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.14"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"use":"work"}}],"reference":"PractitionerRole/1713206641319383000.da71a4c2-27c8-4ce7-a80c-77c3e84bd709"}}},{"fullUrl":"Practitioner/1713206641321199000.2324ab69-16a8-4c53-a70d-c278579bb1be","resource":{"resourceType":"Practitioner","id":"1713206641321199000.2324ab69-16a8-4c53-a70d-c278579bb1be","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.12"}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":["Husam"]}],"address":[{"postalCode":"32174"}]}},{"fullUrl":"Organization/1713206641323433000.9fe18dfd-e9b8-4cf6-8a05-c1431a346f38","resource":{"resourceType":"Organization","id":"1713206641323433000.9fe18dfd-e9b8-4cf6-8a05-c1431a346f38","name":"Avante at Ormond Beach","telecom":[{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"407"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"7397506"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.4","valueString":"jbrush@avantecenters.com"},{"url":"XTN.7","valueString":"7397506"}]}],"system":"email","use":"work"}],"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":["170 North King Road"],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}]}},{"fullUrl":"PractitionerRole/1713206641319383000.da71a4c2-27c8-4ce7-a80c-77c3e84bd709","resource":{"resourceType":"PractitionerRole","id":"1713206641319383000.da71a4c2-27c8-4ce7-a80c-77c3e84bd709","practitioner":{"reference":"Practitioner/1713206641321199000.2324ab69-16a8-4c53-a70d-c278579bb1be"},"organization":{"reference":"Organization/1713206641323433000.9fe18dfd-e9b8-4cf6-8a05-c1431a346f38"}}},{"fullUrl":"Organization/1713206641326316000.09bfed54-b804-406d-b81e-99070fc009e5","resource":{"resourceType":"Organization","id":"1713206641326316000.09bfed54-b804-406d-b81e-99070fc009e5","name":"Avante at Ormond Beach"}},{"fullUrl":"Practitioner/1713206641328586000.8e2d16a2-c07e-45d2-952b-91d5fc20a530","resource":{"resourceType":"Practitioner","id":"1713206641328586000.8e2d16a2-c07e-45d2-952b-91d5fc20a530","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":["Husam"]}]}},{"fullUrl":"Practitioner/1713206641330598000.c2dc264e-0746-4170-94e1-b960571b8d44","resource":{"resourceType":"Practitioner","id":"1713206641330598000.c2dc264e-0746-4170-94e1-b960571b8d44","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":["Husam"]}]}},{"fullUrl":"DiagnosticReport/1713206641338106000.553110e9-ea2b-493e-9ad4-10bb1c4a7ee1","resource":{"resourceType":"DiagnosticReport","id":"1713206641338106000.553110e9-ea2b-493e-9ad4-10bb1c4a7ee1","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}],"basedOn":[{"reference":"ServiceRequest/1713206641332131000.5f636905-91f7-4605-93aa-a914687e2462"}],"status":"final","subject":{"reference":"Patient/1713206641092472000.9e168adf-be2b-49ed-968b-52ecff49e827"},"effectivePeriod":{"start":"2021-02-09T00:00:00-06:00","_start":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"end":"2021-02-09T00:00:00-06:00","_end":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]}},"issued":"2021-02-09T00:00:00-06:00","_issued":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"specimen":[{"reference":"Specimen/1713206641318070000.9ff57cd3-a79f-4f21-8120-9ffb6e4217ec"},{"reference":"Specimen/1713206641315380000.d4d7e28b-6c35-4426-9dab-073761eb20ed"}],"result":[{"reference":"Observation/1713206641100610000.b700af08-de9f-4373-93f0-3270f5552bd7"},{"reference":"Observation/1713206641106291000.1d0cdff9-199d-44e4-8305-cebcca1da91c"},{"reference":"Observation/1713206641109549000.e3729214-40cb-4bb7-803a-3e9ff7560818"},{"reference":"Observation/1713206641112754000.8e0bc613-0ea1-49d3-bfa0-00d1cda28f26"},{"reference":"Observation/1713206641115555000.7b8c6a2b-e833-49e9-a887-18b7d84a9508"}]}}]}"""

// This message will be parsed and successfully passed through the convert step
// despite having a nonexistent NNN segement and an SFT.2 that is not an ST
@Suppress("ktlint:standard:max-line-length")
private const val invalidHL7Record =
    """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.99^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT^4^NH|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|0cba76f5-35e0-4a28-803a-2f31308aae9b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
NNN|5|CWE|95419-8^Has symptoms related to condition of interest^LN||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|0cba76f5-35e0-4a28-803a-2f31308aae9b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600"""

@Suppress("ktlint:standard:max-line-length")
private const val invalidHL7RecordConverted =
    """{"resourceType":"Bundle","id":"1713206642441578000.44cbb63b-af4e-4235-b9bc-380597115f29","meta":{"lastUpdated":"2024-04-15T14:44:02.441-04:00"},"identifier":{"system":"https://reportstream.cdc.gov/prime-router","value":"371784"},"type":"message","timestamp":"2021-02-10T17:07:37.000-05:00","entry":[{"fullUrl":"MessageHeader/4aeed951-99a9-3152-8885-6b0acc6dd35e","resource":{"resourceType":"MessageHeader","id":"4aeed951-99a9-3152-8885-6b0acc6dd35e","meta":{"tag":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0103","code":"P"}]},"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/msh-message-header","extension":[{"url":"MSH.7","valueString":"20210210170737"},{"url":"MSH.15","valueString":"NE"},{"url":"MSH.16","valueString":"NE"},{"url":"MSH.21","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"ELR_Receiver"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.9.99"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"PHLabReportNoAck"}}]}],"eventCoding":{"system":"http://terminology.hl7.org/CodeSystem/v2-0003","code":"R01","display":"ORU^R01^ORU_R01"},"destination":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.5"}],"name":"PRIME_DOH","_endpoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"receiver":{"reference":"Organization/1713206642443348000.c849b492-ed87-4608-b26c-8099a1d0fbb1"}}],"sender":{"reference":"Organization/1713206642442721000.f62a2956-0018-4408-b495-0090a0c8d3c0"},"source":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CDC PRIME - Atlanta, Georgia (Dekalb)"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.237821"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.3"}],"software":"PRIME ReportStream","version":"0.1-SNAPSHOT","endpoint":"urn:oid:2.16.840.1.114222.4.1.237821"}}},{"fullUrl":"Organization/1713206642442721000.f62a2956-0018-4408-b495-0090a0c8d3c0","resource":{"resourceType":"Organization","id":"1713206642442721000.f62a2956-0018-4408-b495-0090a0c8d3c0","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"10D0876999"}],"address":[{"country":"USA"}]}},{"fullUrl":"Organization/1713206642443348000.c849b492-ed87-4608-b26c-8099a1d0fbb1","resource":{"resourceType":"Organization","id":"1713206642443348000.c849b492-ed87-4608-b26c-8099a1d0fbb1","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.6"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Prime ReportStream"}]}},{"fullUrl":"Provenance/1713206642446623000.9dcdf1c8-8294-493f-aeee-1d27f286d092","resource":{"resourceType":"Provenance","id":"1713206642446623000.9dcdf1c8-8294-493f-aeee-1d27f286d092","target":[{"reference":"MessageHeader/4aeed951-99a9-3152-8885-6b0acc6dd35e"},{"reference":"DiagnosticReport/1713206642520815000.14576954-5178-46e4-bf2d-2948dad65d7a"}],"recorded":"2021-02-10T17:07:37Z","activity":{"coding":[{"display":"ORU^R01^ORU_R01"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"author"}]},"who":{"reference":"Organization/1713206642445970000.8d049c91-8447-4dda-b9d0-0088a25dcd9f"}}],"entity":[{"role":"source","what":{"reference":"Device/1713206642447512000.6eca54ae-ec9f-41f1-b8f6-008992c288cf"}}]}},{"fullUrl":"Organization/1713206642445970000.8d049c91-8447-4dda-b9d0-0088a25dcd9f","resource":{"resourceType":"Organization","id":"1713206642445970000.8d049c91-8447-4dda-b9d0-0088a25dcd9f","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"10D0876999"}]}},{"fullUrl":"Organization/1713206642447276000.6ee8df86-0e44-4ec0-998f-3e994b4136e1","resource":{"resourceType":"Organization","id":"1713206642447276000.6ee8df86-0e44-4ec0-998f-3e994b4136e1","name":"Centers for Disease Control and Prevention"}},{"fullUrl":"Device/1713206642447512000.6eca54ae-ec9f-41f1-b8f6-008992c288cf","resource":{"resourceType":"Device","id":"1713206642447512000.6eca54ae-ec9f-41f1-b8f6-008992c288cf","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org","valueReference":{"reference":"Organization/1713206642447276000.6ee8df86-0e44-4ec0-998f-3e994b4136e1"}}],"manufacturer":"Centers for Disease Control and Prevention","deviceName":[{"name":"PRIME ReportStream","type":"manufacturer-name"}],"modelNumber":"0.1-SNAPSHOT","version":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-install-date","valueDateTime":"2021-02-10","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20210210"}]}}],"value":"0.1-SNAPSHOT"}]}},{"fullUrl":"Provenance/1713206642455479000.ec0c3c5f-b40c-45a0-8da0-f3133cf77f2e","resource":{"resourceType":"Provenance","id":"1713206642455479000.ec0c3c5f-b40c-45a0-8da0-f3133cf77f2e","recorded":"2024-04-15T14:44:02Z","policy":["http://hl7.org/fhir/uv/v2mappings/message-oru-r01-to-bundle"],"activity":{"coding":[{"code":"v2-FHIR transformation"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"assembler"}]},"who":{"reference":"Organization/1713206642455180000.abaf0ca5-0ce4-4e66-ab54-8ed4d39af235"}}]}},{"fullUrl":"Organization/1713206642455180000.abaf0ca5-0ce4-4e66-ab54-8ed4d39af235","resource":{"resourceType":"Organization","id":"1713206642455180000.abaf0ca5-0ce4-4e66-ab54-8ed4d39af235","identifier":[{"value":"CDC PRIME - Atlanta"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.237821"}]}},{"fullUrl":"Patient/1713206642465120000.c7ab3544-2017-42b1-966f-1ea4071aa078","resource":{"resourceType":"Patient","id":"1713206642465120000.c7ab3544-2017-42b1-966f-1ea4071aa078","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/pid-patient","extension":[{"url":"PID.8"},{"url":"PID.30","valueString":"N"}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cx-identifier","extension":[{"url":"CX.5","valueString":"PI"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"PID.3"}],"type":{"coding":[{"code":"PI"}]},"value":"2a14112c-ece1-4f82-915c-7b3a8d152eda","assigner":{"reference":"Organization/1713206642458709000.b07d119a-c5fc-456d-a111-a99d13dfb14f"}}],"name":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xpn-human-name","extension":[{"url":"XPN.2","valueString":"Kareem"},{"url":"XPN.3","valueString":"Millie"},{"url":"XPN.7","valueString":"L"}]}],"use":"official","family":"Buckridge","given":["Kareem","Millie"]}],"telecom":[{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"211"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"2240784"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.1","valueString":"7275555555:1:"},{"url":"XTN.2","valueString":"PRN"},{"url":"XTN.4","valueString":"roscoe.wilkinson@email.com"},{"url":"XTN.7","valueString":"2240784"}]}],"system":"email","use":"home"}],"gender":"female","birthDate":"1958-08-10","_birthDate":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"19580810"}]},"deceasedBoolean":false,"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"688 Leighann Inlet"}]}]}],"line":["688 Leighann Inlet"],"city":"South Rodneychester","district":"48077","state":"TX","postalCode":"67071"}]}},{"fullUrl":"Organization/1713206642458709000.b07d119a-c5fc-456d-a111-a99d13dfb14f","resource":{"resourceType":"Organization","id":"1713206642458709000.b07d119a-c5fc-456d-a111-a99d13dfb14f","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"}]}},{"fullUrl":"Provenance/1713206642466182000.58628ca5-5a97-493e-8b51-6808ec7c2466","resource":{"resourceType":"Provenance","id":"1713206642466182000.58628ca5-5a97-493e-8b51-6808ec7c2466","target":[{"reference":"Patient/1713206642465120000.c7ab3544-2017-42b1-966f-1ea4071aa078"}],"recorded":"2024-04-15T14:44:02Z","activity":{"coding":[{"system":"https://terminology.hl7.org/CodeSystem/v3-DataOperation","code":"UPDATE"}]}}},{"fullUrl":"Observation/1713206642469783000.f4c5db7c-9be0-4e72-80ca-0557f1c9fa78","resource":{"resourceType":"Observation","id":"1713206642469783000.f4c5db7c-9be0-4e72-80ca-0557f1c9fa78","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/analysis-date-time","valueDateTime":"2021-02-09T00:00:00-06:00","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"},{"url":"OBX.17"}]}],"status":"final","subject":{"reference":"Patient/1713206642465120000.c7ab3544-2017-42b1-966f-1ea4071aa078"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"performer":[{"reference":"Organization/1713206642470907000.ae612fe4-9207-4401-9caf-0723abf5f3d9"}],"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","code":"260415000","display":"Not detected"}]}}},{"fullUrl":"Organization/1713206642470907000.ae612fe4-9207-4401-9caf-0723abf5f3d9","resource":{"resourceType":"Organization","id":"1713206642470907000.ae612fe4-9207-4401-9caf-0723abf5f3d9","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"10D0876999"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.25"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CLIA"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.4.7"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"10D0876999"}],"name":"Avante at Ormond Beach","address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":["170 North King Road"],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}]}},{"fullUrl":"Observation/1713206642473605000.b6c83892-0b50-4b01-9706-1c2fa7baa384","resource":{"resourceType":"Observation","id":"1713206642473605000.b6c83892-0b50-4b01-9706-1c2fa7baa384","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","subject":{"reference":"Patient/1713206642465120000.c7ab3544-2017-42b1-966f-1ea4071aa078"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"Y","display":"Yes"}]}}},{"fullUrl":"Observation/1713206642476191000.bb58316f-0dbd-4999-8c76-d8c9e40dd9d3","resource":{"resourceType":"Observation","id":"1713206642476191000.bb58316f-0dbd-4999-8c76-d8c9e40dd9d3","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","subject":{"reference":"Patient/1713206642465120000.c7ab3544-2017-42b1-966f-1ea4071aa078"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"Y","display":"Yes"}]}}},{"fullUrl":"Observation/1713206642478444000.e024445b-c695-44f9-a252-46aa9f3ce08e","resource":{"resourceType":"Observation","id":"1713206642478444000.e024445b-c695-44f9-a252-46aa9f3ce08e","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","subject":{"reference":"Patient/1713206642465120000.c7ab3544-2017-42b1-966f-1ea4071aa078"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"N","display":"No"}]}}},{"fullUrl":"Specimen/1713206642489040000.be76ea4a-6b89-478b-affe-a58c816c6715","resource":{"resourceType":"Specimen","id":"1713206642489040000.be76ea4a-6b89-478b-affe-a58c816c6715","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"OBR"}]}},{"fullUrl":"Specimen/1713206642496414000.358934b8-3f76-46dd-9ea1-f41a36ed71e6","resource":{"resourceType":"Specimen","id":"1713206642496414000.358934b8-3f76-46dd-9ea1-f41a36ed71e6","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"SPM"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Component","valueString":"SPM.2.1"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PGN"}]},"value":"0cba76f5-35e0-4a28-803a-2f31308aae9b"}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","code":"258500001","display":"Nasopharyngeal swab"}]},"receivedTime":"2021-02-09T00:00:00-06:00","_receivedTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"collection":{"collectedDateTime":"2021-02-09T00:00:00-06:00","_collectedDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"bodySite":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"2020-09-01","code":"71836000","display":"Nasopharyngeal structure (body structure)"}]}}}},{"fullUrl":"ServiceRequest/1713206642516988000.2282cd4e-a1c5-463a-af99-195e2cf5c875","resource":{"resourceType":"ServiceRequest","id":"1713206642516988000.2282cd4e-a1c5-463a-af99-195e2cf5c875","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/business-event","valueCode":"RE"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/orc-common-order","extension":[{"url":"orc-21-ordering-facility-name","valueReference":{"reference":"Organization/1713206642510123000.a07cf02d-fb00-48a6-b6e4-a9599d723129"}},{"url":"orc-22-ordering-facility-address","valueAddress":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":["170 North King Road"],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}},{"url":"orc-24-ordering-provider-address","valueAddress":{"postalCode":"32174"}},{"url":"orc-12-ordering-provider","valueReference":{"reference":"Practitioner/1713206642513581000.840c770c-43bc-421e-bf84-e7a191bbdd85"}},{"url":"ORC.15","valueString":"20210209"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request","extension":[{"url":"OBR.2","valueIdentifier":{"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}},{"url":"OBR.3","valueIdentifier":{"value":"0cba76f5-35e0-4a28-803a-2f31308aae9b"}},{"url":"OBR.22","valueString":"202102090000-0600"},{"url":"OBR.16","valueReference":{"reference":"Practitioner/1713206642515144000.715ef1ff-16c7-4b43-b7ae-3380ad45221d"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"386"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"6825220"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.7","valueString":"6825220"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBR.17"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"use":"work"}}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}],"status":"unknown","subject":{"reference":"Patient/1713206642465120000.c7ab3544-2017-42b1-966f-1ea4071aa078"},"requester":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"386"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"6825220"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.7","valueString":"6825220"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.14"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"use":"work"}}],"reference":"PractitionerRole/1713206642498726000.fcbf7fa2-2cf6-4480-b191-4bb402058b6d"}}},{"fullUrl":"Practitioner/1713206642502969000.09c82158-6341-4bc6-a582-e7e90056eb21","resource":{"resourceType":"Practitioner","id":"1713206642502969000.09c82158-6341-4bc6-a582-e7e90056eb21","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.12"}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":["Husam"]}],"address":[{"postalCode":"32174"}]}},{"fullUrl":"Organization/1713206642504676000.c0527884-64f2-4e96-a961-13a58fee9be4","resource":{"resourceType":"Organization","id":"1713206642504676000.c0527884-64f2-4e96-a961-13a58fee9be4","name":"Avante at Ormond Beach","telecom":[{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"407"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"7397506"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.4","valueString":"jbrush@avantecenters.com"},{"url":"XTN.7","valueString":"7397506"}]}],"system":"email","use":"work"}],"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":["170 North King Road"],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}]}},{"fullUrl":"PractitionerRole/1713206642498726000.fcbf7fa2-2cf6-4480-b191-4bb402058b6d","resource":{"resourceType":"PractitionerRole","id":"1713206642498726000.fcbf7fa2-2cf6-4480-b191-4bb402058b6d","practitioner":{"reference":"Practitioner/1713206642502969000.09c82158-6341-4bc6-a582-e7e90056eb21"},"organization":{"reference":"Organization/1713206642504676000.c0527884-64f2-4e96-a961-13a58fee9be4"}}},{"fullUrl":"Organization/1713206642510123000.a07cf02d-fb00-48a6-b6e4-a9599d723129","resource":{"resourceType":"Organization","id":"1713206642510123000.a07cf02d-fb00-48a6-b6e4-a9599d723129","name":"Avante at Ormond Beach"}},{"fullUrl":"Practitioner/1713206642513581000.840c770c-43bc-421e-bf84-e7a191bbdd85","resource":{"resourceType":"Practitioner","id":"1713206642513581000.840c770c-43bc-421e-bf84-e7a191bbdd85","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":["Husam"]}]}},{"fullUrl":"Practitioner/1713206642515144000.715ef1ff-16c7-4b43-b7ae-3380ad45221d","resource":{"resourceType":"Practitioner","id":"1713206642515144000.715ef1ff-16c7-4b43-b7ae-3380ad45221d","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":["Husam"]}]}},{"fullUrl":"DiagnosticReport/1713206642520815000.14576954-5178-46e4-bf2d-2948dad65d7a","resource":{"resourceType":"DiagnosticReport","id":"1713206642520815000.14576954-5178-46e4-bf2d-2948dad65d7a","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}],"basedOn":[{"reference":"ServiceRequest/1713206642516988000.2282cd4e-a1c5-463a-af99-195e2cf5c875"}],"status":"final","subject":{"reference":"Patient/1713206642465120000.c7ab3544-2017-42b1-966f-1ea4071aa078"},"effectivePeriod":{"start":"2021-02-09T00:00:00-06:00","_start":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"end":"2021-02-09T00:00:00-06:00","_end":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]}},"issued":"2021-02-09T00:00:00-06:00","_issued":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"specimen":[{"reference":"Specimen/1713206642496414000.358934b8-3f76-46dd-9ea1-f41a36ed71e6"},{"reference":"Specimen/1713206642489040000.be76ea4a-6b89-478b-affe-a58c816c6715"}],"result":[{"reference":"Observation/1713206642469783000.f4c5db7c-9be0-4e72-80ca-0557f1c9fa78"},{"reference":"Observation/1713206642473605000.b6c83892-0b50-4b01-9706-1c2fa7baa384"},{"reference":"Observation/1713206642476191000.bb58316f-0dbd-4999-8c76-d8c9e40dd9d3"},{"reference":"Observation/1713206642478444000.e024445b-c695-44f9-a252-46aa9f3ce08e"}]}}]}"""

// The encoding ^~\&#! make this message not parseable
@Suppress("ktlint:standard:max-line-length")
private const val badEncodingHL7Record =
    """MSH|^~\&#!|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.99^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|0cba76f5-35e0-4a28-803a-2f31308aae9b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|0cba76f5-35e0-4a28-803a-2f31308aae9b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600"""

// The missing | MSH^~\& make this message not parseable
@Suppress("ktlint:standard:max-line-length")
private const val unparseableHL7Record =
    """MSH^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.99^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85^6^7^8&F^9|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|0cba76f5-35e0-4a28-803a-2f31308aae9b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|0cba76f5-35e0-4a28-803a-2f31308aae9b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600"""

// One key difference that makes this unparseable by the 2.5.1 HAPI HL7 structures is that OBX.3 is a CWE not a CE
@Suppress("ktlint:standard:max-line-length")
private const val nistELRHL7Record =
    """MSH|^~\&#|STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|CDC Atlanta^11D0668319^CLIA|MEDSS-ELR ^2.16.840.1.114222.4.3.3.6.2.1^ISO|MNDOH^2.16.840.1.114222.4.1.3661^ISO|20230501102531-0400||ORU^R01^ORU_R01|3003786103_4988249_33033|T|2.5.1|||NE|NE|USA||||PHLabReport-NoAck^PHIN^2.16.840.1.113883.9.11^ISO
SFT|CDC^^^^^CDC&2.16.840.1.114222.4&ISO^XX^^^CDC CLIA|ELIMS V11|STARLIMS|Binary ID unknown
PID|1||PID03953346^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^PI~10171284^^^SPHL-000034&2.16.840.1.114222.4.1.3661&ISO^PI||^^^^^^U||0000||||^^^^^USA^H
NTE|1|L|SPHL Submitter: MN PHL Division, Minnesota Department of Health, Submitter ID: SPHL-000034, Address: 601 Robert St. N.  St. Paul, Minnesota 55164-0899 United States, Email: Health.idlabreports@state.mn.us, Submitter Patient ID: 10171284, Submitter Alt Patient ID: , Submitter Specimen ID: 230011927, Submitter Alt Specimen ID:|RE^Remark^HL70364^^^^2.5.1^^^^^^^2.16.840.1.113883.12.364
ORC|RE|230011927^SPHL-000034^2.16.840.1.114222.4.1.3661^ISO|40_3003786103_4988249_1087^STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|||||||||SPHL-000034^MN PHL Division, Minnesota Department of Health^^^^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^^^^XX||^NET^Internet^Health.idlabreports@state.mn.us|||||||MN PHL Division, Minnesota Department of Health^D^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^XX^^^SPHL-000034|601 Robert St. N.^^St. Paul^MN^55164-0899^USA^M|^WPN^Internet^Health.idlabreports@state.mn.us|601 Robert St. N.^^St. Paul^MN^55164-0899^USA^M
OBR|1|230011927^SPHL-000034^2.16.840.1.114222.4.1.3661^ISO|40_3003786103_4988249_1087^STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|PLT1228^Mold and Yeast XXX MS.MALDI-TOF^PLT^1087^MALDI-TOF-CLIA^L^2.69^v unknown^^CDC-10179^Fungal Identification^L^^2.16.840.1.113883.6.1|||20230322|||||||||SPHL-000034^MN PHL Division, Minnesota Department of Health^^^^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^^^^XX|^NET^Internet^Health.idlabreports@state.mn.us|||||202304271044-0400|||F
OBX|1|CWE|PLT1228^Mold and Yeast XXX MS.MALDI-TOF^PLT^3562^MALDI-TOF-CLIA^L^2.69^v_unknown^MALDI-TOF-CLIA|N8KHKA9H-1|712760003^Candida metapsilosis (organism)^SCT^^^^09012018^^Candida metapsilosis||||||F|||20230322|11D0668319^Centers for Disease Control and Prevention^CLIA^40^Fungus Reference Laboratory^L|HVR0@cdc.gov^Gade^Lalitha|||20230427092900||||Centers for Disease Control and Prevention^L^^^^CLIA&2.16.840.1.113883.4.7&ISO^XX^^^11D0668319|1600 Clifton Rd^^Atlanta^GA^30329^USA^B
SPM|1|230011927&SPHL-000034&2.16.840.1.114222.4.1.3661&ISO^3003786103&STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO||119365002^Specimen from wound^SCT^WND^Wound^L^0912017^Adobe_Code^Wound||||56459004^Foot^SCT^FOT^Foot^L^09012017^Adobe_Code^Foot||||||Isolate,|||20230322|20230421124150"""

@Suppress("ktlint:standard:max-line-length")
private const val nistELRHL7RecordConverted =
    """{"resourceType":"Bundle","id":"1713554735823738000.874be7b8-2943-4029-9259-7412fa72411b","meta":{"lastUpdated":"2024-04-19T15:25:35.837-04:00"},"identifier":{"system":"https://reportstream.cdc.gov/prime-router","value":"3003786103_4988249_33033"},"type":"message","timestamp":"2023-05-01T10:25:31.000-04:00","entry":[{"fullUrl":"MessageHeader/0993dd0b-6ce5-3caf-a177-0b81cc780c18","resource":{"resourceType":"MessageHeader","id":"0993dd0b-6ce5-3caf-a177-0b81cc780c18","meta":{"tag":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0103","code":"T"}]},"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/encoding-characters","valueString":"^~\\&#"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/msh-message-header","extension":[{"url":"MSH.7","valueString":"20230501102531-0400"},{"url":"MSH.15","valueString":"NE"},{"url":"MSH.16","valueString":"NE"},{"url":"MSH.21","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"PHIN"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.9.11"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"PHLabReport-NoAck"}}]}],"eventCoding":{"system":"http://terminology.hl7.org/CodeSystem/v2-0003","code":"R01","display":"ORU^R01^ORU_R01"},"destination":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.6.2.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.5"}],"name":"MEDSS-ELR ","endpoint":"urn:oid:2.16.840.1.114222.4.3.3.6.2.1","receiver":{"reference":"Organization/1713554736948760000.78c33e72-e951-4c56-9a3e-2d5bd593b857"}}],"sender":{"reference":"Organization/1713554736183318000.39a5e318-7ce1-4009-afd9-2aeaf438b595"},"source":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.3"}],"software":"STARLIMS","version":"ELIMS V11","endpoint":"urn:oid:2.16.840.1.114222.4.3.3.2.1.2"}}},{"fullUrl":"Organization/1713554736183318000.39a5e318-7ce1-4009-afd9-2aeaf438b595","resource":{"resourceType":"Organization","id":"1713554736183318000.39a5e318-7ce1-4009-afd9-2aeaf438b595","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"CDC Atlanta"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"11D0668319"}],"address":[{"country":"USA"}]}},{"fullUrl":"Organization/1713554736948760000.78c33e72-e951-4c56-9a3e-2d5bd593b857","resource":{"resourceType":"Organization","id":"1713554736948760000.78c33e72-e951-4c56-9a3e-2d5bd593b857","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.6"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"MNDOH"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"ISO"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.3661"}]}},{"fullUrl":"Provenance/1713554739412832000.6cd6f701-1ca6-4578-a0f8-4e3e786f6482","resource":{"resourceType":"Provenance","id":"1713554739412832000.6cd6f701-1ca6-4578-a0f8-4e3e786f6482","target":[{"reference":"MessageHeader/0993dd0b-6ce5-3caf-a177-0b81cc780c18"},{"reference":"DiagnosticReport/1713554773362573000.691c9921-45c2-420d-b9df-bbd21a819a66"}],"recorded":"2023-05-01T10:25:31-04:00","activity":{"coding":[{"display":"ORU^R01^ORU_R01"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"author"}]},"who":{"reference":"Organization/1713554739086190000.578de01a-fdc9-4638-a54b-1d8e64ce72b6"}}],"entity":[{"role":"source","what":{"reference":"Device/1713554740318757000.7acdfcaf-c3cf-4219-8d5d-d16b51046c23"}}]}},{"fullUrl":"Organization/1713554739086190000.578de01a-fdc9-4638-a54b-1d8e64ce72b6","resource":{"resourceType":"Organization","id":"1713554739086190000.578de01a-fdc9-4638-a54b-1d8e64ce72b6","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"CDC Atlanta"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"11D0668319"}]}},{"fullUrl":"Organization/1713554740241427000.9714ebda-d613-4eb0-a32f-bc38e8f87964","resource":{"resourceType":"Organization","id":"1713554740241427000.9714ebda-d613-4eb0-a32f-bc38e8f87964","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"CDC CLIA"}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CDC"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/code-index-name","valueString":"identifier"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"XX"}]},"value":"CDC CLIA"}],"name":"CDC"}},{"fullUrl":"Device/1713554740318757000.7acdfcaf-c3cf-4219-8d5d-d16b51046c23","resource":{"resourceType":"Device","id":"1713554740318757000.7acdfcaf-c3cf-4219-8d5d-d16b51046c23","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org","valueReference":{"reference":"Organization/1713554740241427000.9714ebda-d613-4eb0-a32f-bc38e8f87964"}}],"manufacturer":"CDC","deviceName":[{"name":"STARLIMS","type":"manufacturer-name"}],"modelNumber":"Binary ID unknown","version":[{"value":"ELIMS V11"}]}},{"fullUrl":"Provenance/1713554740871747000.c7fff45d-dd99-416a-afe5-b7f49f760379","resource":{"resourceType":"Provenance","id":"1713554740871747000.c7fff45d-dd99-416a-afe5-b7f49f760379","recorded":"2024-04-19T15:25:40Z","policy":["http://hl7.org/fhir/uv/v2mappings/message-oru-r01-to-bundle"],"activity":{"coding":[{"code":"v2-FHIR transformation"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"assembler"}]},"who":{"reference":"Organization/1713554740646274000.0111be0a-af19-4804-98e2-9bfafafe27f9"}}]}},{"fullUrl":"Organization/1713554740646274000.0111be0a-af19-4804-98e2-9bfafafe27f9","resource":{"resourceType":"Organization","id":"1713554740646274000.0111be0a-af19-4804-98e2-9bfafafe27f9","identifier":[{"value":"CDC PRIME - Atlanta"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.237821"}]}},{"fullUrl":"Patient/1713554745369124000.520cc5e5-55c1-4b20-9b56-b3bc1feaf5c9","resource":{"resourceType":"Patient","id":"1713554745369124000.520cc5e5-55c1-4b20-9b56-b3bc1feaf5c9","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/patient-notes","valueAnnotation":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/note-type","valueCodeableConcept":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/coding-system-oid","valueOid":"urn:oid:2.16.840.1.113883.12.364"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70364"}],"version":"2.5.1","code":"RE","display":"Remark"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/note-comment","valueId":"SPHL Submitter: MN PHL Division, Minnesota Department of Health, Submitter ID: SPHL-000034, Address: 601 Robert St. N.  St. Paul, Minnesota 55164-0899 United States, Email: Health.idlabreports@state.mn.us, Submitter Patient ID: 10171284, Submitter Alt Patient ID: , Submitter Specimen ID: 230011927, Submitter Alt Specimen ID:"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/note-source","valueId":"L"}],"text":"SPHL Submitter: MN PHL Division, Minnesota Department of Health, Submitter ID: SPHL-000034, Address: 601 Robert St. N.  St. Paul, Minnesota 55164-0899 United States, Email: Health.idlabreports@state.mn.us, Submitter Patient ID: 10171284, Submitter Alt Patient ID: , Submitter Specimen ID: 230011927, Submitter Alt Specimen ID:"}}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cx-identifier","extension":[{"url":"CX.5","valueString":"PI"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"PID.3"}],"type":{"coding":[{"code":"PI"}]},"system":"STARLIMS.CDC.Stag","_system":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"}]},"value":"PID03953346"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cx-identifier","extension":[{"url":"CX.5","valueString":"PI"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"PID.3"}],"type":{"coding":[{"code":"PI"}]},"system":"SPHL-000034","_system":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"}]},"value":"10171284"}],"name":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xpn-human-name","extension":[{"url":"XPN.7","valueString":"U"}]}]}],"_birthDate":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"0000"}]},"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"XAD.7","valueCode":"H"}]}],"use":"home","country":"USA"}]}},{"fullUrl":"Provenance/1713554745499237000.4be46ca6-29ee-4a95-974e-61aa25ed6e6e","resource":{"resourceType":"Provenance","id":"1713554745499237000.4be46ca6-29ee-4a95-974e-61aa25ed6e6e","target":[{"reference":"Patient/1713554745369124000.520cc5e5-55c1-4b20-9b56-b3bc1feaf5c9"}],"recorded":"2024-04-19T15:25:45Z","activity":{"coding":[{"system":"https://terminology.hl7.org/CodeSystem/v3-DataOperation","code":"UPDATE"}]}}},{"fullUrl":"Observation/1713554746923060000.dd391c0c-b602-448e-b4e8-bf4b052cf04a","resource":{"resourceType":"Observation","id":"1713554746923060000.dd391c0c-b602-448e-b4e8-bf4b052cf04a","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sub-id","valueString":"N8KHKA9H-1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/analysis-date-time","valueDateTime":"2023-04-27T09:29:00Z","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20230427092900"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"PLT"}],"version":"2.69","code":"PLT1228","display":"Mold and Yeast XXX MS.MALDI-TOF"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","version":"v_unknown","code":"3562","display":"MALDI-TOF-CLIA"}],"text":"MALDI-TOF-CLIA"},"subject":{"reference":"Patient/1713554745369124000.520cc5e5-55c1-4b20-9b56-b3bc1feaf5c9"},"effectiveDateTime":"2023-03-22","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20230322"}]},"performer":[{"reference":"Organization/1713554747683598000.2f7b821d-4f6e-40f5-afef-e61acc043980"},{"reference":"PractitionerRole/1713554747807549000.a04acf73-a2f2-4cca-9667-7bf48d54a2c0"},{"reference":"Organization/1713554750654608000.8da98795-f35c-496b-aa07-86b7d3169b65"}],"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"09012018","code":"712760003","display":"Candida metapsilosis (organism)"}],"text":"Candida metapsilosis"}}},{"fullUrl":"Organization/1713554747683598000.2f7b821d-4f6e-40f5-afef-e61acc043980","resource":{"resourceType":"Organization","id":"1713554747683598000.2f7b821d-4f6e-40f5-afef-e61acc043980","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-organization","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"CLIA"}],"code":"11D0668319","display":"Centers for Disease Control and Prevention"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","code":"40","display":"Fungus Reference Laboratory"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.15"}],"identifier":[{"system":"CLIA","value":"11D0668319"}],"name":"Centers for Disease Control and Prevention"}},{"fullUrl":"Practitioner/1713554748621794000.410c842a-c835-4cb0-b7d7-e66fd2267a2d","resource":{"resourceType":"Practitioner","id":"1713554748621794000.410c842a-c835-4cb0-b7d7-e66fd2267a2d","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Lalitha"}]}],"identifier":[{"value":"HVR0@cdc.gov"}],"name":[{"family":"Gade","given":["Lalitha"]}]}},{"fullUrl":"PractitionerRole/1713554747807549000.a04acf73-a2f2-4cca-9667-7bf48d54a2c0","resource":{"resourceType":"PractitionerRole","id":"1713554747807549000.a04acf73-a2f2-4cca-9667-7bf48d54a2c0","practitioner":{"reference":"Practitioner/1713554748621794000.410c842a-c835-4cb0-b7d7-e66fd2267a2d"},"code":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/practitioner-role","code":"responsibleObserver"}]}]}},{"fullUrl":"Organization/1713554750654608000.8da98795-f35c-496b-aa07-86b7d3169b65","resource":{"resourceType":"Organization","id":"1713554750654608000.8da98795-f35c-496b-aa07-86b7d3169b65","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/organization-name-type","valueCoding":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueCodeableConcept":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"XON.2"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"L"}]}}],"code":"L"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"11D0668319"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.25"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CLIA"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.4.7"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/code-index-name","valueString":"identifier"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"XX"}]},"value":"11D0668319"}],"name":"Centers for Disease Control and Prevention","address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"1600 Clifton Rd"}]},{"url":"XAD.7","valueCode":"B"}]}],"use":"work","line":["1600 Clifton Rd"],"city":"Atlanta","state":"GA","postalCode":"30329","country":"USA"}]}},{"fullUrl":"Specimen/1713554752517070000.a0d3044d-ab7e-4ed5-b137-41a1faf4b4bb","resource":{"resourceType":"Specimen","id":"1713554752517070000.a0d3044d-ab7e-4ed5-b137-41a1faf4b4bb","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"OBR"}]}},{"fullUrl":"Specimen/1713554756103464000.439e4cca-a262-4ece-bdc0-4caa347f6288","resource":{"resourceType":"Specimen","id":"1713554756103464000.439e4cca-a262-4ece-bdc0-4caa347f6288","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"SPM"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/filler-assigned-identifier","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/entity-identifier","valueString":"3003786103"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Component","valueString":"SPM.2.1"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PGN"}]},"value":"230011927"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/placer-assigned-identifier","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/entity-identifier","valueString":"230011927"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Component","valueString":"SPM.2.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FGN"}]},"value":"3003786103"}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"0912017","code":"119365002","display":"Specimen from wound"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","version":"Adobe_Code","code":"WND","display":"Wound"}],"text":"Wound"},"receivedTime":"2023-04-21T12:41:50Z","_receivedTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20230421124150"}]},"collection":{"collectedDateTime":"2023-03-22","_collectedDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20230322"}]},"bodySite":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"09012017","code":"56459004","display":"Foot"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","version":"Adobe_Code","code":"FOT","display":"Foot"}],"text":"Foot"}},"note":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"SPM.14"}],"text":"Isolate,"}]}},{"fullUrl":"ServiceRequest/1713554771399858000.25601862-92f6-4784-ad81-45bfbc35bbd5","resource":{"resourceType":"ServiceRequest","id":"1713554771399858000.25601862-92f6-4784-ad81-45bfbc35bbd5","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/business-event","valueCode":"RE"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/orc-common-order","extension":[{"url":"orc-21-ordering-facility-name","valueReference":{"reference":"Organization/1713554764570047000.a4c3e953-b395-44eb-b427-e37d722dd173"}},{"url":"orc-22-ordering-facility-address","valueAddress":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"601 Robert St. N."}]},{"url":"XAD.7","valueCode":"M"}]}],"type":"postal","line":["601 Robert St. N."],"city":"St. Paul","state":"MN","postalCode":"55164-0899","country":"USA"}},{"url":"orc-24-ordering-provider-address","valueAddress":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"601 Robert St. N."}]},{"url":"XAD.7","valueCode":"M"}]}],"type":"postal","line":["601 Robert St. N."],"city":"St. Paul","state":"MN","postalCode":"55164-0899","country":"USA"}},{"url":"orc-12-ordering-provider","valueReference":{"reference":"Practitioner/1713554767444182000.90670a3d-d8aa-41fb-b3c8-2720e9c24e57"}}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request","extension":[{"url":"OBR.2","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"230011927"}},{"url":"OBR.3","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"40_3003786103_4988249_1087"}},{"url":"OBR.22","valueString":"202304271044-0400"},{"url":"OBR.16","valueReference":{"reference":"Practitioner/1713554769798930000.86e6f6a2-e97b-4c5e-bf71-1bd8a55234ce"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"NET"},{"url":"XTN.3","valueString":"Internet"},{"url":"XTN.4","valueString":"Health.idlabreports@state.mn.us"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBR.17"}],"system":"email","value":"Health.idlabreports@state.mn.us"}}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"230011927"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.3"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"40_3003786103_4988249_1087"}],"status":"unknown","code":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/coding-system-oid","valueOid":"urn:oid:2.16.840.1.113883.6.1"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"PLT"}],"version":"2.69","code":"PLT1228","display":"Mold and Yeast XXX MS.MALDI-TOF"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"secondary-alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","code":"CDC-10179","display":"Fungal Identification"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","version":"v unknown","code":"1087","display":"MALDI-TOF-CLIA"}]},"subject":{"reference":"Patient/1713554745369124000.520cc5e5-55c1-4b20-9b56-b3bc1feaf5c9"},"requester":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"NET"},{"url":"XTN.3","valueString":"Internet"},{"url":"XTN.4","valueString":"Health.idlabreports@state.mn.us"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.14"}],"system":"email","value":"Health.idlabreports@state.mn.us"}}],"reference":"PractitionerRole/1713554756183827000.dd11099b-8a7b-4596-a453-d2608356e841"}}},{"fullUrl":"Practitioner/1713554757979898000.ad0b92d4-60be-4181-9327-cc08495aaa85","resource":{"resourceType":"Practitioner","id":"1713554757979898000.ad0b92d4-60be-4181-9327-cc08495aaa85","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.12"}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"XX"}]},"system":"STARLIMS.CDC.Stag","value":"SPHL-000034"}],"name":[{"family":"MN PHL Division, Minnesota Department of Health"}],"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"601 Robert St. N."}]},{"url":"XAD.7","valueCode":"M"}]}],"type":"postal","line":["601 Robert St. N."],"city":"St. Paul","state":"MN","postalCode":"55164-0899","country":"USA"}]}},{"fullUrl":"Organization/1713554760801781000.c0323db1-a426-4fe4-b322-3e83e8060206","resource":{"resourceType":"Organization","id":"1713554760801781000.c0323db1-a426-4fe4-b322-3e83e8060206","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/organization-name-type","valueCoding":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueCodeableConcept":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"XON.2"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"D"}]}}],"code":"D"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"SPHL-000034"}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/code-index-name","valueString":"identifier"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"XX"}]},"value":"SPHL-000034"}],"name":"MN PHL Division, Minnesota Department of Health","telecom":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.3","valueString":"Internet"},{"url":"XTN.4","valueString":"Health.idlabreports@state.mn.us"}]}],"system":"email","value":"Health.idlabreports@state.mn.us","use":"work"}],"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"601 Robert St. N."}]},{"url":"XAD.7","valueCode":"M"}]}],"type":"postal","line":["601 Robert St. N."],"city":"St. Paul","state":"MN","postalCode":"55164-0899","country":"USA"}]}},{"fullUrl":"PractitionerRole/1713554756183827000.dd11099b-8a7b-4596-a453-d2608356e841","resource":{"resourceType":"PractitionerRole","id":"1713554756183827000.dd11099b-8a7b-4596-a453-d2608356e841","practitioner":{"reference":"Practitioner/1713554757979898000.ad0b92d4-60be-4181-9327-cc08495aaa85"},"organization":{"reference":"Organization/1713554760801781000.c0323db1-a426-4fe4-b322-3e83e8060206"}}},{"fullUrl":"Organization/1713554764570047000.a4c3e953-b395-44eb-b427-e37d722dd173","resource":{"resourceType":"Organization","id":"1713554764570047000.a4c3e953-b395-44eb-b427-e37d722dd173","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/organization-name-type","valueCoding":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueCodeableConcept":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"XON.2"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"D"}]}}],"code":"D"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"SPHL-000034"}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/code-index-name","valueString":"identifier"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"XX"}]},"value":"SPHL-000034"}],"name":"MN PHL Division, Minnesota Department of Health"}},{"fullUrl":"Practitioner/1713554767444182000.90670a3d-d8aa-41fb-b3c8-2720e9c24e57","resource":{"resourceType":"Practitioner","id":"1713554767444182000.90670a3d-d8aa-41fb-b3c8-2720e9c24e57","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"XX"}]},"system":"STARLIMS.CDC.Stag","value":"SPHL-000034"}],"name":[{"family":"MN PHL Division, Minnesota Department of Health"}]}},{"fullUrl":"Practitioner/1713554769798930000.86e6f6a2-e97b-4c5e-bf71-1bd8a55234ce","resource":{"resourceType":"Practitioner","id":"1713554769798930000.86e6f6a2-e97b-4c5e-bf71-1bd8a55234ce","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"XX"}]},"system":"STARLIMS.CDC.Stag","value":"SPHL-000034"}],"name":[{"family":"MN PHL Division, Minnesota Department of Health"}]}},{"fullUrl":"DiagnosticReport/1713554773362573000.691c9921-45c2-420d-b9df-bbd21a819a66","resource":{"resourceType":"DiagnosticReport","id":"1713554773362573000.691c9921-45c2-420d-b9df-bbd21a819a66","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"230011927"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"40_3003786103_4988249_1087"}],"basedOn":[{"reference":"ServiceRequest/1713554771399858000.25601862-92f6-4784-ad81-45bfbc35bbd5"}],"status":"final","code":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/coding-system-oid","valueOid":"urn:oid:2.16.840.1.113883.6.1"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"PLT"}],"version":"2.69","code":"PLT1228","display":"Mold and Yeast XXX MS.MALDI-TOF"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"secondary-alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","code":"CDC-10179","display":"Fungal Identification"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","version":"v unknown","code":"1087","display":"MALDI-TOF-CLIA"}]},"subject":{"reference":"Patient/1713554745369124000.520cc5e5-55c1-4b20-9b56-b3bc1feaf5c9"},"effectiveDateTime":"2023-03-22","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20230322"}]},"issued":"2023-04-27T10:44:00-04:00","_issued":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202304271044-0400"}]},"specimen":[{"reference":"Specimen/1713554756103464000.439e4cca-a262-4ece-bdc0-4caa347f6288"},{"reference":"Specimen/1713554752517070000.a0d3044d-ab7e-4ed5-b137-41a1faf4b4bb"}],"result":[{"reference":"Observation/1713554746923060000.dd391c0c-b602-448e-b4e8-bf4b052cf04a"}]}}]}"""

@Suppress("ktlint:standard:max-line-length")
private const val nistELRHL7RecordWithoutMSH21 =
    """MSH|^~\&#|STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|CDC Atlanta^11D0668319^CLIA|MEDSS-ELR ^2.16.840.1.114222.4.3.3.6.2.1^ISO|MNDOH^2.16.840.1.114222.4.1.3661^ISO|20230501102531-0400||ORU^R01^ORU_R01|3003786103_4988249_33033|T|2.5.1|||NE|NE|USA||||PHLabReport-NoAck^PHIN^2.16.840.1.113883.9.11^ISO
SFT|CDC^^^^^CDC&2.16.840.1.114222.4&ISO^XX^^^CDC CLIA|ELIMS V11|STARLIMS|Binary ID unknown
PID|1||PID03953346^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^PI~10171284^^^SPHL-000034&2.16.840.1.114222.4.1.3661&ISO^PI||^^^^^^U||0000||||^^^^^USA^H
NTE|1|L|SPHL Submitter: MN PHL Division, Minnesota Department of Health, Submitter ID: SPHL-000034, Address: 601 Robert St. N.  St. Paul, Minnesota 55164-0899 United States, Email: Health.idlabreports@state.mn.us, Submitter Patient ID: 10171284, Submitter Alt Patient ID: , Submitter Specimen ID: 230011927, Submitter Alt Specimen ID:|RE^Remark^HL70364^^^^2.5.1^^^^^^^2.16.840.1.113883.12.364
ORC|RE|230011927^SPHL-000034^2.16.840.1.114222.4.1.3661^ISO|40_3003786103_4988249_1087^STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|||||||||SPHL-000034^MN PHL Division, Minnesota Department of Health^^^^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^^^^XX||^NET^Internet^Health.idlabreports@state.mn.us|||||||MN PHL Division, Minnesota Department of Health^D^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^XX^^^SPHL-000034|601 Robert St. N.^^St. Paul^MN^55164-0899^USA^M|^WPN^Internet^Health.idlabreports@state.mn.us|601 Robert St. N.^^St. Paul^MN^55164-0899^USA^M
OBR|1|230011927^SPHL-000034^2.16.840.1.114222.4.1.3661^ISO|40_3003786103_4988249_1087^STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|PLT1228^Mold and Yeast XXX MS.MALDI-TOF^PLT^1087^MALDI-TOF-CLIA^L^2.69^v unknown^^CDC-10179^Fungal Identification^L^^2.16.840.1.113883.6.1|||20230322|||||||||SPHL-000034^MN PHL Division, Minnesota Department of Health^^^^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^^^^XX|^NET^Internet^Health.idlabreports@state.mn.us|||||202304271044-0400|||F
OBX|1|CE|PLT1228^Mold and Yeast XXX MS.MALDI-TOF^PLT^3562^MALDI-TOF-CLIA^L^2.69^v_unknown^MALDI-TOF-CLIA|N8KHKA9H-1|712760003^Candida metapsilosis (organism)^SCT^^^^09012018^^Candida metapsilosis||||||F|||20230322|11D0668319^Centers for Disease Control and Prevention^CLIA^40^Fungus Reference Laboratory^L|HVR0@cdc.gov^Gade^Lalitha|||20230427092900||||Centers for Disease Control and Prevention^L^^^^CLIA&2.16.840.1.113883.4.7&ISO^XX^^^11D0668319|1600 Clifton Rd^^Atlanta^GA^30329^USA^B
SPM|1|230011927&SPHL-000034&2.16.840.1.114222.4.1.3661&ISO^3003786103&STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO||119365002^Specimen from wound^SCT^WND^Wound^L^0912017^Adobe_Code^Wound||||56459004^Foot^SCT^FOT^Foot^L^09012017^Adobe_Code^Foot||||||Isolate,|||20230322|20230421124150"""

@Suppress("ktlint:standard:max-line-length")
private const val validRadxMarsHL7Message =
    """MSH|^~\&|MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO|CAREEVOLUTION^00Z0000024^CLIA|AIMS.INTEGRATION.PRD^2.16.840.1.114222.4.3.15.1^ISO|AIMS.PLATFORM^2.16.840.1.114222.4.1.217446^ISO|20240403205305+0000||ORU^R01^ORU_R01|20240403205305_dba7572cc6334f1ea0744c5f235c823e|P|2.5.1|||NE|NE|||||PHLabReport-NoAck^ELR251R1_Rcvr_Prof^2.16.840.1.113883.9.11^ISO
SFT|CAREEVOLUTION|2022|MMTC.PROD|16498||20240402
PID|1||8be6fa3710374dcebe0174e0fd5a1a7c^^^MMTC.PROD&2.16.840.1.113883.3.8589.4.2.106.1&ISO^PI||^^^^^^~^^^^^^||||||^^^^02139^USA||^^^^^111^1111111
ORC|RE||^MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO|||||||||^^||^^^^^^|||||||SA.OTCSelfReport|^^^^02139^^^^|^^^^^^
OBR|1||^MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.71|||20240403120000-0400|||||||||^^|^^^^^^|||||20240403120000-0400|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.71||260373001^Detected^SCT^^^^20200901||||||F||||00Z0000042||BinaxNOW COVID-19 Antigen Self Test_Abbott Diagnostics Scarborough, Inc._EUA^^99ELR^^^^Vunknown||20240403120000-0400||||SA.OTCSelfReport^^^^^&2.16.840.1.113883.3.8589.4.1.152&ISO^XX^^^00Z0000042|
NTE|1|L|Note
OBX|2|NM|35659-2^Age at specimen collection^LN^^^^2.71||24|a^year^UCUM^^^^2.1|||||F||||00Z0000042||||||||SA.OTCSelfReport^^^^^&2.16.840.1.113883.3.8589.4.1.152&ISO^XX^^^00Z0000042||||||QST
SPM|1|^dba7572cc6334f1ea0744c5f235c823e&MMTC.PROD&2.16.840.1.113883.3.8589.4.2.106.1&ISO||697989009^Anterior nares swab^SCT^^^^20200901|||||||||||||20240403120000-0400|20240403120000-0400"""

@Suppress("ktlint:standard:max-line-length")
private const val validRadxMarsHL7MessageConverted =
    """{"resourceType":"Bundle","id":"1713555888862407000.cef383e2-5b9f-4ace-91e6-987b0fbb075f","meta":{"lastUpdated":"2024-04-19T15:44:48.877-04:00"},"identifier":{"system":"https://reportstream.cdc.gov/prime-router","value":"20240403205305_dba7572cc6334f1ea0744c5f235c823e"},"type":"message","timestamp":"2024-04-03T16:53:05.000-04:00","entry":[{"fullUrl":"MessageHeader/df373c48-bfb2-36b0-b63c-5be13bc5d051","resource":{"resourceType":"MessageHeader","id":"df373c48-bfb2-36b0-b63c-5be13bc5d051","meta":{"tag":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0103","code":"P"}]},"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/msh-message-header","extension":[{"url":"MSH.7","valueString":"20240403205305+0000"},{"url":"MSH.15","valueString":"NE"},{"url":"MSH.16","valueString":"NE"},{"url":"MSH.21","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"ELR251R1_Rcvr_Prof"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.9.11"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"PHLabReport-NoAck"}}]}],"eventCoding":{"system":"http://terminology.hl7.org/CodeSystem/v2-0003","code":"R01","display":"ORU^R01^ORU_R01"},"destination":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.15.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.5"}],"name":"AIMS.INTEGRATION.PRD","endpoint":"urn:oid:2.16.840.1.114222.4.3.15.1","receiver":{"reference":"Organization/1713555889700167000.d1e27ebf-775e-4ecb-8198-0f4e48bb5e4a"}}],"sender":{"reference":"Organization/1713555889025647000.bcbf69ca-f8bf-4f54-bfb3-1b003103c5e0"},"source":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.3"}],"software":"MMTC.PROD","version":"2022","endpoint":"urn:oid:2.16.840.1.113883.3.8589.4.2.106.1"}}},{"fullUrl":"Organization/1713555889025647000.bcbf69ca-f8bf-4f54-bfb3-1b003103c5e0","resource":{"resourceType":"Organization","id":"1713555889025647000.bcbf69ca-f8bf-4f54-bfb3-1b003103c5e0","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"CAREEVOLUTION"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"00Z0000024"}]}},{"fullUrl":"Organization/1713555889700167000.d1e27ebf-775e-4ecb-8198-0f4e48bb5e4a","resource":{"resourceType":"Organization","id":"1713555889700167000.d1e27ebf-775e-4ecb-8198-0f4e48bb5e4a","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.6"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"AIMS.PLATFORM"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"ISO"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.217446"}]}},{"fullUrl":"Provenance/1713555891941112000.398cfd69-aed7-4ba2-95da-4c773b55b41a","resource":{"resourceType":"Provenance","id":"1713555891941112000.398cfd69-aed7-4ba2-95da-4c773b55b41a","target":[{"reference":"MessageHeader/df373c48-bfb2-36b0-b63c-5be13bc5d051"},{"reference":"DiagnosticReport/1713555917740429000.b89431d3-7bfe-4cf9-a71f-33f278555eb3"}],"recorded":"2024-04-03T20:53:05Z","activity":{"coding":[{"display":"ORU^R01^ORU_R01"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"author"}]},"who":{"reference":"Organization/1713555891675908000.22f13d01-df78-4396-8828-7d02cfa69a7c"}}],"entity":[{"role":"source","what":{"reference":"Device/1713555892405191000.89cb9024-6a4b-451c-ba0a-bbd9d13ea1db"}}]}},{"fullUrl":"Organization/1713555891675908000.22f13d01-df78-4396-8828-7d02cfa69a7c","resource":{"resourceType":"Organization","id":"1713555891675908000.22f13d01-df78-4396-8828-7d02cfa69a7c","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"CAREEVOLUTION"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"00Z0000024"}]}},{"fullUrl":"Organization/1713555892339950000.66a969bc-51ce-4c7d-830d-f663a587ca76","resource":{"resourceType":"Organization","id":"1713555892339950000.66a969bc-51ce-4c7d-830d-f663a587ca76","name":"CAREEVOLUTION"}},{"fullUrl":"Device/1713555892405191000.89cb9024-6a4b-451c-ba0a-bbd9d13ea1db","resource":{"resourceType":"Device","id":"1713555892405191000.89cb9024-6a4b-451c-ba0a-bbd9d13ea1db","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org","valueReference":{"reference":"Organization/1713555892339950000.66a969bc-51ce-4c7d-830d-f663a587ca76"}}],"manufacturer":"CAREEVOLUTION","deviceName":[{"name":"MMTC.PROD","type":"manufacturer-name"}],"modelNumber":"16498","version":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-install-date","valueDateTime":"2024-04-02","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240402"}]}}],"value":"2022"}]}},{"fullUrl":"Provenance/1713555892959030000.c106f700-edb6-4df2-89e1-f7c751ff394e","resource":{"resourceType":"Provenance","id":"1713555892959030000.c106f700-edb6-4df2-89e1-f7c751ff394e","recorded":"2024-04-19T15:44:52Z","policy":["http://hl7.org/fhir/uv/v2mappings/message-oru-r01-to-bundle"],"activity":{"coding":[{"code":"v2-FHIR transformation"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"assembler"}]},"who":{"reference":"Organization/1713555892762294000.10c1b4e7-5cc5-41aa-bddf-46dc4aae8a6d"}}]}},{"fullUrl":"Organization/1713555892762294000.10c1b4e7-5cc5-41aa-bddf-46dc4aae8a6d","resource":{"resourceType":"Organization","id":"1713555892762294000.10c1b4e7-5cc5-41aa-bddf-46dc4aae8a6d","identifier":[{"value":"CDC PRIME - Atlanta"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.237821"}]}},{"fullUrl":"Patient/1713555896970223000.d6378fbb-99da-4555-8a7a-610c1679d2a1","resource":{"resourceType":"Patient","id":"1713555896970223000.d6378fbb-99da-4555-8a7a-610c1679d2a1","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cx-identifier","extension":[{"url":"CX.5","valueString":"PI"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"PID.3"}],"type":{"coding":[{"code":"PI"}]},"system":"MMTC.PROD","_system":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"}]},"value":"8be6fa3710374dcebe0174e0fd5a1a7c"}],"name":[{},{}],"telecom":[{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"111"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"1111111"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.7","valueString":"1111111"}]}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"use":"home"}],"address":[{"postalCode":"02139","country":"USA"}]}},{"fullUrl":"Provenance/1713555897142384000.f1b6f07f-c4fe-4121-a648-cd48a3fb5cac","resource":{"resourceType":"Provenance","id":"1713555897142384000.f1b6f07f-c4fe-4121-a648-cd48a3fb5cac","target":[{"reference":"Patient/1713555896970223000.d6378fbb-99da-4555-8a7a-610c1679d2a1"}],"recorded":"2024-04-19T15:44:57Z","activity":{"coding":[{"system":"https://terminology.hl7.org/CodeSystem/v3-DataOperation","code":"UPDATE"}]}}},{"fullUrl":"Observation/1713555898715289000.3f1ca893-577a-42a2-8566-456dd558f3c2","resource":{"resourceType":"Observation","id":"1713555898715289000.3f1ca893-577a-42a2-8566-456dd558f3c2","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/analysis-date-time","valueDateTime":"2024-04-03T12:00:00-04:00","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240403120000-0400"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"},{"url":"OBX.17","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"99ELR"}],"version":"Vunknown","code":"BinaxNOW COVID-19 Antigen Self Test_Abbott Diagnostics Scarborough, Inc._EUA"}]}}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","version":"2.71","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1713555896970223000.d6378fbb-99da-4555-8a7a-610c1679d2a1"},"performer":[{"reference":"Organization/1713555899345277000.b5239449-b980-49d2-b1e7-cce259513439"}],"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"20200901","code":"260373001","display":"Detected"}]},"note":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/note-comment","valueId":"Note"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/note-source","valueId":"L"}],"text":"Note"}],"method":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"99ELR"}],"version":"Vunknown","code":"BinaxNOW COVID-19 Antigen Self Test_Abbott Diagnostics Scarborough, Inc._EUA"}]}}},{"fullUrl":"Organization/1713555899345277000.b5239449-b980-49d2-b1e7-cce259513439","resource":{"resourceType":"Organization","id":"1713555899345277000.b5239449-b980-49d2-b1e7-cce259513439","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-organization","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"00Z0000042"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.15"}],"identifier":[{"value":"00Z0000042"}]}},{"fullUrl":"Observation/1713555901979912000.90ce2ce6-48e1-44f2-9daf-6030183606ef","resource":{"resourceType":"Observation","id":"1713555901979912000.90ce2ce6-48e1-44f2-9daf-6030183606ef","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"NM"},{"url":"OBX.6","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"UCUM"}],"system":"http://unitsofmeasure.org","version":"2.1","code":"a","display":"year"}]}},{"url":"OBX.11","valueString":"F"}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","version":"2.71","code":"35659-2","display":"Age at specimen collection"}]},"subject":{"reference":"Patient/1713555896970223000.d6378fbb-99da-4555-8a7a-610c1679d2a1"},"performer":[{"reference":"Organization/1713555902640852000.59f20c98-97bb-448a-896c-54cfec023d87"}],"valueQuantity":{"value":24,"unit":"year","system":"UCUM","code":"a"}}},{"fullUrl":"Organization/1713555902640852000.59f20c98-97bb-448a-896c-54cfec023d87","resource":{"resourceType":"Organization","id":"1713555902640852000.59f20c98-97bb-448a-896c-54cfec023d87","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-organization","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"00Z0000042"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.15"}],"identifier":[{"value":"00Z0000042"}]}},{"fullUrl":"Specimen/1713555904344262000.26db3a9a-dfd8-41eb-be8f-7223d2b4763c","resource":{"resourceType":"Specimen","id":"1713555904344262000.26db3a9a-dfd8-41eb-be8f-7223d2b4763c","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"OBR"}]}},{"fullUrl":"Specimen/1713555906899454000.e71d8eba-00a9-4b27-8af7-a432697c5a26","resource":{"resourceType":"Specimen","id":"1713555906899454000.e71d8eba-00a9-4b27-8af7-a432697c5a26","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"SPM"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Component","valueString":"SPM.2.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FGN"}]},"value":"dba7572cc6334f1ea0744c5f235c823e"}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"20200901","code":"697989009","display":"Anterior nares swab"}]},"receivedTime":"2024-04-03T12:00:00-04:00","_receivedTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240403120000-0400"}]},"collection":{"collectedDateTime":"2024-04-03T12:00:00-04:00","_collectedDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240403120000-0400"}]}}}},{"fullUrl":"ServiceRequest/1713555916247401000.c520233f-96ae-45e6-8abb-c4ffef7fdbe3","resource":{"resourceType":"ServiceRequest","id":"1713555916247401000.c520233f-96ae-45e6-8abb-c4ffef7fdbe3","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/business-event","valueCode":"RE"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/orc-common-order","extension":[{"url":"orc-21-ordering-facility-name","valueReference":{"reference":"Organization/1713555911608986000.7d3d3e2b-d949-4ae4-bfd6-b163c8576810"}},{"url":"orc-22-ordering-facility-address","valueAddress":{"postalCode":"02139"}},{"url":"orc-12-ordering-provider","valueReference":{"reference":"Practitioner/1713555913137584000.29fcaec7-ad91-4a4c-86e1-b7afda8f80a0"}}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request","extension":[{"url":"OBR.3","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}]}},{"url":"OBR.22","valueString":"20240403120000-0400"},{"url":"OBR.16","valueReference":{"reference":"Practitioner/1713555914841387000.74381b5c-0f60-413f-bbd9-54f886a3ab89"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBR.17"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]}}}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.3"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]}}],"status":"unknown","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","version":"2.71","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1713555896970223000.d6378fbb-99da-4555-8a7a-610c1679d2a1"},"requester":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.14"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]}}}],"reference":"PractitionerRole/1713555906967545000.1c1e4b74-94b0-48d9-8f1e-f15dac814736"}}},{"fullUrl":"Practitioner/1713555907819246000.06853b79-69a5-45d9-be08-d5eb7bac23ff","resource":{"resourceType":"Practitioner","id":"1713555907819246000.06853b79-69a5-45d9-be08-d5eb7bac23ff","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.12"}]}},{"fullUrl":"Organization/1713555909313131000.20e93c2d-cda4-4b71-a105-0fecad8d30cb","resource":{"resourceType":"Organization","id":"1713555909313131000.20e93c2d-cda4-4b71-a105-0fecad8d30cb","name":"SA.OTCSelfReport","telecom":[{"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]}}],"address":[{"postalCode":"02139"}]}},{"fullUrl":"PractitionerRole/1713555906967545000.1c1e4b74-94b0-48d9-8f1e-f15dac814736","resource":{"resourceType":"PractitionerRole","id":"1713555906967545000.1c1e4b74-94b0-48d9-8f1e-f15dac814736","practitioner":{"reference":"Practitioner/1713555907819246000.06853b79-69a5-45d9-be08-d5eb7bac23ff"},"organization":{"reference":"Organization/1713555909313131000.20e93c2d-cda4-4b71-a105-0fecad8d30cb"}}},{"fullUrl":"Organization/1713555911608986000.7d3d3e2b-d949-4ae4-bfd6-b163c8576810","resource":{"resourceType":"Organization","id":"1713555911608986000.7d3d3e2b-d949-4ae4-bfd6-b163c8576810","name":"SA.OTCSelfReport"}},{"fullUrl":"Practitioner/1713555913137584000.29fcaec7-ad91-4a4c-86e1-b7afda8f80a0","resource":{"resourceType":"Practitioner","id":"1713555913137584000.29fcaec7-ad91-4a4c-86e1-b7afda8f80a0"}},{"fullUrl":"Practitioner/1713555914841387000.74381b5c-0f60-413f-bbd9-54f886a3ab89","resource":{"resourceType":"Practitioner","id":"1713555914841387000.74381b5c-0f60-413f-bbd9-54f886a3ab89"}},{"fullUrl":"DiagnosticReport/1713555917740429000.b89431d3-7bfe-4cf9-a71f-33f278555eb3","resource":{"resourceType":"DiagnosticReport","id":"1713555917740429000.b89431d3-7bfe-4cf9-a71f-33f278555eb3","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]}}],"basedOn":[{"reference":"ServiceRequest/1713555916247401000.c520233f-96ae-45e6-8abb-c4ffef7fdbe3"}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","version":"2.71","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1713555896970223000.d6378fbb-99da-4555-8a7a-610c1679d2a1"},"effectiveDateTime":"2024-04-03T12:00:00-04:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240403120000-0400"}]},"issued":"2024-04-03T12:00:00-04:00","_issued":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240403120000-0400"}]},"specimen":[{"reference":"Specimen/1713555906899454000.e71d8eba-00a9-4b27-8af7-a432697c5a26"},{"reference":"Specimen/1713555904344262000.26db3a9a-dfd8-41eb-be8f-7223d2b4763c"}],"result":[{"reference":"Observation/1713555898715289000.3f1ca893-577a-42a2-8566-456dd558f3c2"},{"reference":"Observation/1713555901979912000.90ce2ce6-48e1-44f2-9daf-6030183606ef"}]}}]}"""

@Suppress("ktlint:standard:max-line-length")
private const val invalidRadxMarsHL7Message =
    """MSH|^~\&|MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO|CAREEVOLUTION^00Z0000024^CLIA|AIMS.INTEGRATION.PRD^2.16.840.1.114222.4.3.15.1^ISO|AIMS.PLATFORM^2.16.840.1.114222.4.1.217446^ISO|20240403205305+0000||ORU^R01^ORU_R01|20240403205305_dba7572cc6334f1ea0744c5f235c823e|P|2.5.1|||NE|NE|||||PHLabReport-NoAck^ELR251R1_Rcvr_Prof^2.16.840.1.113883.9.11^ISO
SFT|CAREEVOLUTION|2022|MMTC.PROD|16498||20240402
PID|1||8be6fa3710374dcebe0174e0fd5a1a7c^^^MMTC.PROD&2.16.840.1.113883.3.8589.4.2.106.1&ISO^PI||^^^^^^~^^^^^^||||||^^^^02139^USA||^^^^^111^1111111
ORC|RE||^MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO|||||||||^^||^^^^^^|||||||SA.OTCSelfReport|^^^^02139^^^^|^^^^^^
OBR|1||^MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.71|||20240403120000|||||||||^^|^^^^^^|||||20240403120000|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.71||260373001^Detected^SCT^^^^20200901||||||F||||00Z0000042||BinaxNOW COVID-19 Antigen Self Test_Abbott Diagnostics Scarborough, Inc._EUA^^99ELR^^^^Vunknown||20240403120000||||SA.OTCSelfReport^^^^^&2.16.840.1.113883.3.8589.4.1.152&ISO^XX^^^00Z0000042|
NTE|1|L|Note
OBX|2|NM|35659-2^Age at specimen collection^LN^^^^2.71||24|a^year^UCUM^^^^2.1|||||F||||00Z0000042||||||||SA.OTCSelfReport^^^^^&2.16.840.1.113883.3.8589.4.1.152&ISO^XX^^^00Z0000042||||||QST
SPM|1|^dba7572cc6334f1ea0744c5f235c823e&MMTC.PROD&2.16.840.1.113883.3.8589.4.2.106.1&ISO||697989009^Anterior nares swab^SCT^^^^20200901|||||||||||||20240403120000-0400|20240403120000-0400"""

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FhirFunctionIntegrationTests() {

    @Container
    val azuriteContainer = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_fhirfunctionintegration1",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    val oneOrganization = DeepOrganization(
        "phd", "test", Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "phd",
                Topic.TEST,
                CustomerStatus.INACTIVE,
                "one",
                timing = Receiver.Timing(numberPerDay = 1, maxReportCount = 1, whenEmpty = Receiver.WhenEmpty())
            ),
            Receiver(
                "elr2",
                "phd",
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "classpath:/metadata/hl7_mapping/ORU_R01/ORU_R01-base.yml",
                timing = Receiver.Timing(numberPerDay = 1, maxReportCount = 1, whenEmpty = Receiver.WhenEmpty()),
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                processingModeFilter = listOf("true"),
                format = Report.Format.HL7,
            )
        ),
    )

    private fun makeWorkflowEngine(
        metadata: Metadata,
        settings: SettingsProvider,
        databaseAccess: DatabaseAccess,
    ): WorkflowEngine {
        return spyk(
            WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(databaseAccess)
                .build()
        )
    }

    private fun seedTask(
        fileFormat: Report.Format,
        currentAction: TaskAction,
        nextAction: TaskAction,
        nextEventAction: Event.EventAction,
        topic: Topic,
        taskIndex: Long = 0,
        organization: DeepOrganization,
        childReport: Report? = null,
        bodyURL: String? = null,
    ): Report {
        val report = Report(
            fileFormat,
            listOf(ClientSource(organization = organization.name, client = "Test Sender")),
            1,
            metadata = UnitTestUtils.simpleMetadata,
            nextAction = nextAction,
            topic = topic
        )
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val action = Action().setActionName(currentAction)
            val actionId = ReportStreamTestDatabaseContainer.testDatabaseAccess.insertAction(txn, action)
            report.bodyURL = bodyURL ?: "http://${report.id}.${fileFormat.toString().lowercase()}"
            val reportFile = ReportFile().setSchemaTopic(topic).setReportId(report.id)
                .setActionId(actionId).setSchemaName("").setBodyFormat(fileFormat.toString()).setItemCount(1)
                .setExternalName("test-external-name")
                .setBodyUrl(report.bodyURL)
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                reportFile, txn, action
            )
            if (childReport != null) {
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            taskIndex,
                            actionId,
                            report.id,
                            childReport.id,
                            OffsetDateTime.now()
                        ),
                        txn
                    )
            }

            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertTask(
                report,
                fileFormat.toString().lowercase(),
                report.bodyURL,
                nextAction = ProcessEvent(
                    nextEventAction,
                    report.id,
                    Options.None,
                    emptyMap(),
                    emptyList()
                ),
                txn
            )
        }

        return report
    }

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `test does not update the DB or send messages on an error`() {
        val report = seedTask(
            Report.Format.HL7,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )

        mockkObject(BlobAccess)
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { BlobAccess.downloadBlobAsByteArray(any()) } returns cleanHL7Record.toByteArray()
        every {
            BlobAccess.uploadBody(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws RuntimeException("manual error")
        every { QueueAccess.sendMessage(any(), any()) } returns Unit

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = FHIRConverter(
            UnitTestUtils.simpleMetadata,
            settings,
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${report.id}\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-${report.id}.hl7\",\"digest\"" +
            ":\"${BlobAccess.digestToString(BlobAccess.sha256Digest(cleanHL7Record.toByteArray()))}\"," +
            "\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        assertThrows<RuntimeException> {
            fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)
        }

        val processTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
        assertThat(processTask.processedAt).isNull()
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routeTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.route))
                .fetchOneInto(Task.TASK)
            assertThat(routeTask).isNull()
            val convertReportFile =
                DSL.using(txn).select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                    .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                    .where(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.NEXT_ACTION.eq(TaskAction.route))
                    .fetchOneInto(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
            assertThat(convertReportFile).isNull()
        }
        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }
    }

    @Test
    fun `test successfully processes a convert message for HL7`() {
        val report = seedTask(
            Report.Format.HL7,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )

        mockkObject(BlobAccess)
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { BlobAccess.downloadBlobAsByteArray(any()) } returns cleanHL7Record.toByteArray()
        every {
            BlobAccess.uploadBody(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns BlobAccess.BlobInfo(Report.Format.FHIR, "", "".toByteArray())
        every { QueueAccess.sendMessage(any(), any()) } returns Unit

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = FHIRConverter(
            metadata,
            settings,
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${report.id}\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-${report.id}.hl7\",\"digest\":" +
            "\"${BlobAccess.digestToString(BlobAccess.sha256Digest(cleanHL7Record.toByteArray()))}\"," +
            "\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)

        val processTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
        assertThat(processTask.processedAt).isNotNull()
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routeTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.route))
                .fetchOneInto(Task.TASK)
            assertThat(routeTask).isNotNull()
            val convertReportFile =
                DSL.using(txn).select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                    .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                    .where(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.NEXT_ACTION.eq(TaskAction.route))
                    .fetchOneInto(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
            assertThat(convertReportFile).isNotNull()
        }
        verify(exactly = 1) {
            QueueAccess.sendMessage(elrRoutingQueueName, any())
            BlobAccess.uploadBody(Report.Format.FHIR, any(), any(), any(), any())
        }
    }

    @Test
    fun `test successfully processes a convert message for bulk HL7 message`() {
        val validBatch = cleanHL7Record + "\n" + invalidHL7Record
        val report = seedTask(
            Report.Format.HL7,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )

        mockkObject(BlobAccess)
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { BlobAccess.downloadBlobAsByteArray(any()) } returns validBatch.toByteArray()
        every {
            BlobAccess.uploadBody(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } answers { BlobAccess.BlobInfo(Report.Format.FHIR, UUID.randomUUID().toString(), "".toByteArray()) }
        every { QueueAccess.sendMessage(any(), any()) } returns Unit

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = FHIRConverter(
            metadata,
            settings,
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${report.id}\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-${report.id}.hl7\",\"digest\":" +
            "\"${BlobAccess.digestToString(BlobAccess.sha256Digest(validBatch.toByteArray()))}\"," +
            "\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)

        val processTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
        assertThat(processTask.processedAt).isNotNull()
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routeTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.route))
                .fetchInto(Task.TASK)
            assertThat(routeTask).hasSize(2)
            val convertReportFile =
                DSL.using(txn).select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                    .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                    .where(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.NEXT_ACTION.eq(TaskAction.route))
                    .fetchInto(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
            assertThat(convertReportFile).hasSize(2)
        }
        verify(exactly = 2) {
            QueueAccess.sendMessage(elrRoutingQueueName, any())
        }
        verify(exactly = 1) {
            BlobAccess.uploadBody(
                Report.Format.FHIR,
                match { bytes ->
                    val result = CompareData().compare(
                        bytes.inputStream(),
                        cleanHL7RecordConverted.byteInputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    result.passed
                },
                any(), any(), any()
            )
            BlobAccess.uploadBody(
                Report.Format.FHIR,
                match { bytes ->
                    val result = CompareData().compare(
                        bytes.inputStream(),
                        invalidHL7RecordConverted.byteInputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    result.passed
                },
                any(), any(), any()
            )
        }
    }

    @Test
    fun `test no items routed for HL7 if any in batch are invalid`() {
        val validBatch =
            cleanHL7Record + "\n" + invalidHL7Record + "\n" + badEncodingHL7Record + "\n" + unparseableHL7Record
        val report = seedTask(
            Report.Format.HL7,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )

        mockkObject(BlobAccess)
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { BlobAccess.downloadBlobAsByteArray(any()) } returns validBatch.toByteArray()
        every {
            BlobAccess.uploadBody(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } answers { BlobAccess.BlobInfo(Report.Format.FHIR, UUID.randomUUID().toString(), "".toByteArray()) }
        every { QueueAccess.sendMessage(any(), any()) } returns Unit

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = FHIRConverter(
            metadata,
            settings,
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${report.id}\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-${report.id}.hl7\",\"digest\":" +
            "\"${BlobAccess.digestToString(BlobAccess.sha256Digest(validBatch.toByteArray()))}\"," +
            "\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

        val actionLogger = ActionLogger()
        val fhirFunc = FHIRFunctions(
            workflowEngine,
            actionLogger = actionLogger,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)

        val processTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
        assertThat(processTask.processedAt).isNotNull()
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routeTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.route))
                .fetchInto(Task.TASK)
            assertThat(routeTask).hasSize(2)
            val convertReportFile =
                DSL.using(txn).select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                    .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                    .where(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.NEXT_ACTION.eq(TaskAction.route))
                    .fetchInto(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
            assertThat(convertReportFile).hasSize(2)
            assertThat(actionLogger.errors).hasSize(2)
        }
        verify(exactly = 2) {
            QueueAccess.sendMessage(elrRoutingQueueName, any())
        }

        verify(exactly = 1) {
            BlobAccess.uploadBody(
                Report.Format.FHIR,
                match { bytes ->
                    val result = CompareData().compare(
                        bytes.inputStream(),
                        cleanHL7RecordConverted.byteInputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    result.passed
                },
                any(), any(), any()
            )
            BlobAccess.uploadBody(
                Report.Format.FHIR,
                match { bytes ->
                    val result = CompareData().compare(
                        bytes.inputStream(),
                        invalidHL7RecordConverted.byteInputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    result.passed
                },
                any(), any(), any()
            )
        }
    }

    @Test
    fun `test successfully processes a convert message for a bulk (ndjson) FHIR message`() {
        val report = seedTask(
            Report.Format.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )

        mockkObject(BlobAccess)
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        val bulkFHIRRecord =
            listOf(
                validFHIRRecord1,
                invalidEmptyFHIRRecord,
                validFHIRRecord2,
                invalidMalformedFHIRRecord
            ).joinToString(
                "\n"
            )
        every { BlobAccess.downloadBlobAsByteArray(any()) } returns bulkFHIRRecord.toByteArray()
        every {
            BlobAccess.uploadBody(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            BlobAccess.BlobInfo(Report.Format.FHIR, UUID.randomUUID().toString(), "".toByteArray())
        }
        every { QueueAccess.sendMessage(any(), any()) } returns Unit

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = FHIRConverter(
            metadata,
            settings,
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val actionLogger = ActionLogger()
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${report.id}\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-${report.id}.fhir\",\"digest\":" +
            "\"${BlobAccess.digestToString(BlobAccess.sha256Digest(bulkFHIRRecord.toByteArray()))}\"," +
            "\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess,
            actionLogger = actionLogger
        )

        fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)

        val processTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
        assertThat(processTask.processedAt).isNotNull()
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routeTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.route))
                .fetchInto(Task.TASK)
            assertThat(routeTask).hasSize(2)
            val convertReportFile =
                DSL.using(txn).select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                    .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                    .where(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.NEXT_ACTION.eq(TaskAction.route))
                    .fetchInto(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
            assertThat(convertReportFile).hasSize(2)

            // Expect two errors for the two badly formed bundles
            assertThat(actionLogger.errors).hasSize(2)
        }
        verify(exactly = 2) {
            QueueAccess.sendMessage(elrRoutingQueueName, any())
            BlobAccess.uploadBody(Report.Format.FHIR, any(), any(), any(), any())
        }
        verify(exactly = 1) {
            BlobAccess.uploadBody(
                Report.Format.FHIR,
                validFHIRRecord1.toByteArray(),
                any(),
                any(),
                any()
            )
            BlobAccess.uploadBody(
                Report.Format.FHIR,
                validFHIRRecord2.toByteArray(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `test successfully processes a convert message with invalid HL7 items`() {
        val receiveReport = seedTask(
            Report.Format.HL7,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        // set up and seed azure blobstore
        val blobConnectionString =
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10000
                )
            }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10001
                )
            }/devstoreaccount1;"""
        val blobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            blobConnectionString
        )

        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns blobContainerMetadata
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
        mockkObject(BlobAccess.BlobContainerMetadata)

        val receiveReportBytes = (cleanHL7Record + "\n" + invalidHL7Record + "\n" + unparseableHL7Record).toByteArray()
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "convertBlob.hl7",
            receiveReportBytes,
            blobContainerMetadata
        )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${receiveReport.id}\"," +
            "\"blobURL\":\"" + receiveBlobUrl +
            "\",\"digest\":\"${
                BlobAccess.digestToString(
                    BlobAccess.sha256Digest(
                        receiveReportBytes
                    )
                )
            }\",\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"," +
            "\"receiverFullName\":\"phd.elr2\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        val fhirEngine = spyk(
            FHIRConverter(
                metadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
            )
        )

        fhirFunc.doConvert(queueMessage, 1, fhirEngine)

        verify(exactly = 2) {
            QueueAccess.sendMessage(any(), any())
        }

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            // Verify that there were two created reports from the 2 items that were parseable
            val routedReports = DSL
                .using(txn)
                .select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                .where(
                    gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.REPORT_ID
                        .`in`(
                            DSL
                                .select(
                                    gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE.CHILD_REPORT_ID
                                )
                                .from(gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE)
                                .where(
                                    gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE.PARENT_REPORT_ID
                                        .eq(
                                            receiveReport.id
                                        )
                                )
                        )
                ).fetchInto(ReportFile::class.java)
            assertThat(routedReports).hasSize(2)

            // Verify that the expected FHIR bundles were uploaded
            val fhirBundles =
                routedReports.map { BlobAccess.downloadBlobAsByteArray(it.bodyUrl, blobContainerMetadata) }
            assertThat(fhirBundles).each {
                it.matchesPredicate { bytes ->
                    val invalidHL7Result = CompareData().compare(
                        bytes.inputStream(),
                        invalidHL7RecordConverted.byteInputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    invalidHL7Result.passed

                    val cleanHL7Result = CompareData().compare(
                        bytes.inputStream(),
                        cleanHL7RecordConverted.byteInputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    invalidHL7Result.passed || cleanHL7Result.passed
                }
            }

            // Verify that there was an action log with an error created
            val actionLogs = DSL.using(txn).select(ACTION_LOG.asterisk()).from(ACTION_LOG)
                .where(ACTION_LOG.REPORT_ID.eq(receiveReport.id)).and(ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(
                    DetailedActionLog::class.java
                )

            assertThat(actionLogs).hasSize(1)
            @Suppress("ktlint:standard:max-line-length")
            assertThat(actionLogs.first()).transform { it.detail.message }
                .isEqualTo("Item 3 in the report was not parseable. Reason: exception while parsing HL7: Determine encoding for message. The following is the first 50 chars of the message for reference, although this may not be where the issue is: MSH^~\\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16")
        }
    }

    @Test
    fun `test successfully processes a convert message with invalid FHIR items`() {
        val receiveReport = seedTask(
            Report.Format.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        // set up and seed azure blobstore
        val blobConnectionString =
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10000
                )
            }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10001
                )
            }/devstoreaccount1;"""
        val blobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            blobConnectionString
        )

        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns blobContainerMetadata
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
        mockkObject(BlobAccess.BlobContainerMetadata)
        val bulkFHIRRecord =
            listOf(
                validFHIRRecord1,
                invalidEmptyFHIRRecord,
                validFHIRRecord2,
                invalidMalformedFHIRRecord
            ).joinToString(
                "\n"
            )
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "convertBlob.fhir",
            bulkFHIRRecord.toByteArray(),
            blobContainerMetadata
        )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${receiveReport.id}\"," +
            "\"blobURL\":\"" + receiveBlobUrl +
            "\",\"digest\":\"${
                BlobAccess.digestToString(
                    BlobAccess.sha256Digest(
                        bulkFHIRRecord.toByteArray()
                    )
                )
            }\",\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"," +
            "\"receiverFullName\":\"phd.elr2\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        val fhirEngine = spyk(
            FHIRConverter(
                metadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
            )
        )

        fhirFunc.doConvert(queueMessage, 1, fhirEngine)

        verify(exactly = 2) {
            QueueAccess.sendMessage(any(), any())
        }

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            // Verify that there were two created reports from the 2 items that were parseable
            val routedReports = DSL
                .using(txn)
                .select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                .where(
                    gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.REPORT_ID
                        .`in`(
                            DSL
                                .select(
                                    gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE.CHILD_REPORT_ID
                                )
                                .from(gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE)
                                .where(
                                    gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE.PARENT_REPORT_ID
                                        .eq(
                                            receiveReport.id
                                        )
                                )
                        )
                ).fetchInto(ReportFile::class.java)
            assertThat(routedReports).hasSize(2)

            // Verify that the expected FHIR bundles were uploaded
            val fhirBundles =
                routedReports.map { BlobAccess.downloadBlobAsByteArray(it.bodyUrl, blobContainerMetadata) }
                    .map { it.toString(Charset.defaultCharset()) }
            assertThat(fhirBundles).containsOnly(validFHIRRecord1, validFHIRRecord2)

            // Verify that there was an action log with an error created
            val actionLogs = DSL.using(txn).select(ACTION_LOG.asterisk()).from(ACTION_LOG)
                .where(ACTION_LOG.REPORT_ID.eq(receiveReport.id)).and(ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(
                    DetailedActionLog::class.java
                )

            assertThat(actionLogs).hasSize(2)
            @Suppress("ktlint:standard:max-line-length")
            assertThat(actionLogs).transform {
                it.map { log ->
                    log.detail.message
                }
            }
                .containsOnly(
                    "Item 2 in the report was not parseable. Reason: exception while parsing FHIR: HAPI-1838: Invalid JSON content detected, missing required element: 'resourceType'",
                    "Item 4 in the report was not parseable. Reason: exception while parsing FHIR: HAPI-1861: Failed to parse JSON encoded FHIR content: Unexpected end-of-input: was expecting closing quote for a string value\n" +
                        " at [line: 1, column: 23]"
                )
        }
    }

    @Test
    fun `test successfully converting a NIST ELR HL7 message`() {
        val receiveReport = seedTask(
            Report.Format.HL7,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        // set up and seed azure blobstore
        val blobConnectionString =
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10000
                )
            }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10001
                )
            }/devstoreaccount1;"""
        val blobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            blobConnectionString
        )

        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns blobContainerMetadata
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
        mockkObject(BlobAccess.BlobContainerMetadata)

        val receiveReportBytes = (nistELRHL7Record + "\n" + nistELRHL7RecordWithoutMSH21).toByteArray()
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "convertBlob.hl7",
            receiveReportBytes,
            blobContainerMetadata
        )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${receiveReport.id}\"," +
            "\"blobURL\":\"" + receiveBlobUrl +
            "\",\"digest\":\"${
                BlobAccess.digestToString(
                    BlobAccess.sha256Digest(
                        receiveReportBytes
                    )
                )
            }\",\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"elr-elims\"," +
            "\"receiverFullName\":\"phd.elr2\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        val fhirEngine = spyk(
            FHIRConverter(
                metadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
            )
        )

        fhirFunc.doConvert(queueMessage, 1, fhirEngine)

        verify(exactly = 1) {
            QueueAccess.sendMessage(any(), any())
        }

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            // Verify that there were two created reports from the 2 items that were parseable
            val routedReports = DSL
                .using(txn)
                .select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                .where(
                    gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.REPORT_ID
                        .`in`(
                            DSL
                                .select(
                                    gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE.CHILD_REPORT_ID
                                )
                                .from(gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE)
                                .where(
                                    gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE.PARENT_REPORT_ID
                                        .eq(
                                            receiveReport.id
                                        )
                                )
                        )
                ).fetchInto(ReportFile::class.java)
            assertThat(routedReports).hasSize(1)

            // Verify that the expected FHIR bundles were uploaded
            val fhirBundles =
                routedReports.map { BlobAccess.downloadBlobAsByteArray(it.bodyUrl, blobContainerMetadata) }
            assertThat(fhirBundles).each {
                it.matchesPredicate { bytes ->
                    val nistELRResult = CompareData().compare(
                        bytes.inputStream(),
                        nistELRHL7RecordConverted.byteInputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    nistELRResult.passed
                }
            }

            // Verify that there was an action log with an error created
            val actionLogs = DSL.using(txn).select(ACTION_LOG.asterisk()).from(ACTION_LOG)
                .where(ACTION_LOG.REPORT_ID.eq(receiveReport.id)).and(ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(
                    DetailedActionLog::class.java
                )

            assertThat(actionLogs).hasSize(1)
            @Suppress("ktlint:standard:max-line-length")
            assertThat(actionLogs.first()).transform { it.detail.message }
                .isEqualTo("Item 2 in the report was not parseable. Reason: exception while parsing HL7: 'CE' in record 1 is invalid for version 2.7 at OBX-2(0)")
        }
    }

    @Test
    fun `test successfully processes a valid RADxMARS HL7 message`() {
        val receiveReport = seedTask(
            Report.Format.HL7,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.MARS_OTC_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        // set up and seed azure blobstore
        val blobConnectionString =
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10000
                )
            }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10001
                )
            }/devstoreaccount1;"""
        val blobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            blobConnectionString
        )

        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns blobContainerMetadata
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
        mockkObject(BlobAccess.BlobContainerMetadata)

        val receiveReportBytes = (invalidRadxMarsHL7Message + "\n" + validRadxMarsHL7Message).toByteArray()
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "convertBlob.hl7",
            receiveReportBytes,
            blobContainerMetadata
        )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${receiveReport.id}\"," +
            "\"blobURL\":\"" + receiveBlobUrl +
            "\",\"digest\":\"${
                BlobAccess.digestToString(
                    BlobAccess.sha256Digest(
                        receiveReportBytes
                    )
                )
            }\",\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"mars-otc-elr\"," +
            "\"receiverFullName\":\"phd.elr2\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        val fhirEngine = spyk(
            FHIRConverter(
                metadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
            )
        )

        fhirFunc.doConvert(queueMessage, 1, fhirEngine)

        verify(exactly = 1) {
            QueueAccess.sendMessage(any(), any())
        }

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            // Verify that there were two created reports from the 2 items that were parseable
            val routedReports = DSL
                .using(txn)
                .select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                .where(
                    gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.REPORT_ID
                        .`in`(
                            DSL
                                .select(
                                    gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE.CHILD_REPORT_ID
                                )
                                .from(gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE)
                                .where(
                                    gov.cdc.prime.router.azure.db.tables.ReportLineage.REPORT_LINEAGE.PARENT_REPORT_ID
                                        .eq(
                                            receiveReport.id
                                        )
                                )
                        )
                ).fetchInto(ReportFile::class.java)
            assertThat(routedReports).hasSize(1)

            // Verify that the expected FHIR bundles were uploaded
            val fhirBundles =
                routedReports.map { BlobAccess.downloadBlobAsByteArray(it.bodyUrl, blobContainerMetadata) }
            assertThat(fhirBundles).each {
                it.matchesPredicate { bytes ->
                    val radxMarsResult = CompareData().compare(
                        bytes.inputStream(),
                        validRadxMarsHL7MessageConverted.byteInputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    radxMarsResult.passed
                }
            }

            // Verify that there was an action log with an error created
            val actionLogs = DSL.using(txn).select(ACTION_LOG.asterisk()).from(ACTION_LOG)
                .where(ACTION_LOG.REPORT_ID.eq(receiveReport.id)).and(ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(
                    DetailedActionLog::class.java
                )

            assertThat(actionLogs).hasSize(1)
            @Suppress("ktlint:standard:max-line-length")
            assertThat(actionLogs.first()).transform { it.detail.message }
                .isEqualTo(
                    "Item 1 in the report was not valid. Reason: [Error][6,266] Constraint Failure # OBX-19 - If  OBX-19.1 (Time)  is valued  then  OBX-19.1 (Time) shall match the regular expression '^(\\d{14}\\.\\d{1,4}|\\d{14})((\\x2D|\\x2B)\\d{4})\$'. \n" +
                        "\tOBX.19[1].1[1] SHALL not be present OR ()OBX.19[1].1[1] SHALL match '^(\\d{14}\\.\\d{1,4}|\\d{14})((\\x2D|\\x2B)\\d{4})\$' regular expression\n" +
                        "\tOBX.19[1].1[1] SHALL not be present\n" +
                        "\t\t[6, 266] The inner expression is true\n" +
                        "\t()OBX.19[1].1[1] SHALL match '^(\\d{14}\\.\\d{1,4}|\\d{14})((\\x2D|\\x2B)\\d{4})\$' regular expression\n" +
                        "\t\t[6, 266] '20240403120000' doesn't match '^(\\d{14}\\.\\d{1,4}|\\d{14})((\\x2D|\\x2B)\\d{4})\$'"
                )
        }
    }

    @Test
    fun `test successfully processes a route message`() {
        val reportServiceMock = mockk<ReportService>()
        val report = seedTask(
            Report.Format.HL7,
            TaskAction.receive,
            TaskAction.translate,
            Event.EventAction.TRANSLATE,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )

        mockkObject(BlobAccess)
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        val routeFhirBytes =
            File(VALID_FHIR_PATH).readBytes()
        every {
            BlobAccess.downloadBlobAsByteArray(any())
        } returns routeFhirBytes
        every {
            BlobAccess.uploadBody(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns BlobAccess.BlobInfo(Report.Format.FHIR, "", "".toByteArray())
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
        every { reportServiceMock.getSenderName(any()) } returns "senderOrg.senderOrgClient"

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = FHIRRouter(
            UnitTestUtils.simpleMetadata,
            settings,
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            reportService = reportServiceMock
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        val queueMessage = "{\"type\":\"route\",\"reportId\":\"${report.id}\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-${report.id}.hl7\",\"digest\":" +
            "\"${BlobAccess.digestToString(BlobAccess.sha256Digest(routeFhirBytes))}\",\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,

            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        fhirFunc.doRoute(queueMessage, 1, fhirEngine, actionHistory)

        val convertTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
        assertThat(convertTask.routedAt).isNotNull()
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routeTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.translate))
                .fetchOneInto(Task.TASK)
            assertThat(routeTask).isNotNull()
            val convertReportFile =
                DSL.using(txn).select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                    .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                    .where(
                        gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.NEXT_ACTION
                            .eq(TaskAction.translate)
                    )
                    .fetchOneInto(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
            assertThat(convertReportFile).isNotNull()
        }
        verify(exactly = 1) {
            QueueAccess.sendMessage(elrTranslationQueueName, any())
        }
    }

    /*
    Send a FHIR message to an HL7v2 receiver and ensure the message receiver receives is translated to HL7v2
     */
    @Test
    fun `test successfully processes a translate message when isSendOriginal is false`() {
        // set up and seed azure blobstore
        val blobConnectionString =
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10000
                )
            }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10001
                )
            }/devstoreaccount1;"""
        val blobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            blobConnectionString
        )

        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns blobContainerMetadata

        // upload reports
        val receiveBlobName = "receiveBlobName"
        val translateFhirBytes = File(
            MULTIPLE_TARGETS_FHIR_PATH
        ).readBytes()
        val receiveBlobUrl = BlobAccess.uploadBlob(
            receiveBlobName,
            translateFhirBytes,
            blobContainerMetadata
        )

        // Seed the steps backwards so report lineage can be correctly generated
        val translateReport = seedTask(
            Report.Format.FHIR,
            TaskAction.translate,
            TaskAction.send,
            Event.EventAction.SEND,
            Topic.ELR_ELIMS,
            100,
            oneOrganization
        )
        val routeReport = seedTask(
            Report.Format.FHIR,
            TaskAction.route,
            TaskAction.translate,
            Event.EventAction.TRANSLATE,
            Topic.ELR_ELIMS,
            99,
            oneOrganization,
            translateReport
        )
        val convertReport = seedTask(
            Report.Format.FHIR,
            TaskAction.convert,
            TaskAction.route,
            Event.EventAction.ROUTE,
            Topic.ELR_ELIMS,
            98,
            oneOrganization,
            routeReport
        )
        val receiveReport = seedTask(
            Report.Format.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.ELR_ELIMS,
            97,
            oneOrganization,
            convertReport,
            receiveBlobUrl
        )

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = spyk(
            FHIRTranslator(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
                reportService = ReportService(ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess))
            )
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
        mockkObject(BlobAccess.BlobContainerMetadata)
        every { BlobAccess.BlobContainerMetadata.build("metadata", any()) } returns BlobAccess.BlobContainerMetadata(
            "metadata",
            blobConnectionString
        )

        // The topic param of queueMessage is what should determine how the Translate function runs
        val queueMessage = "{\"type\":\"translate\",\"reportId\":\"${translateReport.id}\"," +
            "\"blobURL\":\"" + receiveBlobUrl +
            "\",\"digest\":\"${
                BlobAccess.digestToString(
                    BlobAccess.sha256Digest(
                        translateFhirBytes
                    )
                )
            }\",\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"," +
            "\"receiverFullName\":\"phd.elr2\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )

        fhirFunc.doTranslate(queueMessage, 1, fhirEngine, actionHistory)

        // verify task and report_file tables were updated correctly in the Translate function (new task and new
        // record file created)
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val queueTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.batch))
                .fetchOneInto(Task.TASK)
            assertThat(queueTask).isNotNull()

            val sendReportFile =
                DSL.using(txn).select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                    .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                    .where(
                        gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.REPORT_ID
                            .eq(queueTask!!.reportId)
                    )
                    .fetchOneInto(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
            assertThat(sendReportFile).isNotNull()

            // verify sendReportFile message does not match the original message from receive step
            assertThat(BlobAccess.downloadBlobAsByteArray(sendReportFile!!.bodyUrl, blobContainerMetadata))
                .isNotEqualTo(BlobAccess.downloadBlobAsByteArray(receiveReport.bodyURL, blobContainerMetadata))
        }

        // verify we did not call the sendOriginal function
        verify(exactly = 0) {
            fhirEngine.sendOriginal(any(), any(), any())
        }

        // verify we called the sendTranslated function
        verify(exactly = 1) {
            fhirEngine.sendTranslated(any(), any(), any())
        }

        // verify sendMessage did not get called because next action should be Batch
        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }
    }

    /*
    Send a FHIR message to an HL7v2 receiver and ensure the message receiver receives is the original FHIR and NOT
    translated to HL7v2
     */
    @Test
    fun `test successfully processes a translate message when isSendOriginal is true`() {
        // set up and seed azure blobstore
        val blobConnectionString =
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10000
                )
            }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10001
                )
            }/devstoreaccount1;"""
        val blobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            blobConnectionString
        )

        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns blobContainerMetadata

        // upload reports
        val receiveBlobName = "receiveBlobName"
        val translateFhirBytes = File(
            MULTIPLE_TARGETS_FHIR_PATH
        ).readBytes()
        val receiveBlobUrl = BlobAccess.uploadBlob(
            receiveBlobName,
            translateFhirBytes,
            blobContainerMetadata
        )

        // Seed the steps backwards so report lineage can be correctly generated
        val translateReport = seedTask(
            Report.Format.FHIR,
            TaskAction.translate,
            TaskAction.send,
            Event.EventAction.SEND,
            Topic.ELR_ELIMS,
            100,
            oneOrganization
        )
        val routeReport = seedTask(
            Report.Format.FHIR,
            TaskAction.route,
            TaskAction.translate,
            Event.EventAction.TRANSLATE,
            Topic.ELR_ELIMS,
            99,
            oneOrganization,
            translateReport
        )
        val convertReport = seedTask(
            Report.Format.FHIR,
            TaskAction.convert,
            TaskAction.route,
            Event.EventAction.ROUTE,
            Topic.ELR_ELIMS,
            98,
            oneOrganization,
            routeReport
        )
        val receiveReport = seedTask(
            Report.Format.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.ELR_ELIMS,
            97,
            oneOrganization,
            convertReport,
            receiveBlobUrl
        )

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = spyk(
            FHIRTranslator(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
                reportService = ReportService(ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess))
            )
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns Unit

        // The topic param of queueMessage is what should determine how the Translate function runs
        val queueMessage = "{\"type\":\"translate\",\"reportId\":\"${translateReport.id}\"," +
            "\"blobURL\":\"" + receiveBlobUrl +
            "\",\"digest\":\"${
                BlobAccess.digestToString(
                    BlobAccess.sha256Digest(
                        translateFhirBytes
                    )
                )
            }\",\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"elr-elims\"," +
            "\"receiverFullName\":\"phd.elr2\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,

            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )

        fhirFunc.doTranslate(queueMessage, 1, fhirEngine, actionHistory)

        // verify task and report_file tables were updated correctly in the Translate function
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val sendTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.send))
                .fetchOneInto(Task.TASK)
            assertThat(sendTask).isNotNull()

            val sendReportFile =
                DSL.using(txn).select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                    .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                    .where(
                        gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.REPORT_ID
                            .eq(sendTask!!.reportId)
                    )
                    .fetchOneInto(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
            assertThat(sendReportFile).isNotNull()

            // verify sendReportFile message matches the original message from receive step
            assertThat(BlobAccess.downloadBlobAsByteArray(sendReportFile!!.bodyUrl, blobContainerMetadata))
                .isEqualTo(BlobAccess.downloadBlobAsByteArray(receiveReport.bodyURL, blobContainerMetadata))
        }

        // verify we called the sendOriginal function
        verify(exactly = 1) {
            fhirEngine.sendOriginal(any(), any(), any())
        }

        // verify we did not call the sendTranslated function
        verify(exactly = 0) {
            fhirEngine.sendTranslated(any(), any(), any())
        }

        // verify sendMessage did get called because next action should be Send since isOriginal skips the batch
        // step
        verify(exactly = 1) {
            QueueAccess.sendMessage(any(), any())
        }
    }

    @Test
    fun `test unmapped observation error messages`() {
        val report = seedTask(
            Report.Format.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)
        val fhirRecordBytes = fhirengine.azure.fhirRecord.toByteArray()

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )

        mockkObject(BlobAccess)
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { BlobAccess.downloadBlobAsByteArray(any()) } returns fhirRecordBytes
        every {
            BlobAccess.uploadBody(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns BlobAccess.BlobInfo(Report.Format.FHIR, "", "".toByteArray())
        every { QueueAccess.sendMessage(any(), any()) } returns Unit

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = FHIRConverter(
            metadata,
            settings,
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine =
            makeWorkflowEngine(
                metadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${report.id}\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-${report.id}.fhir\",\"digest\":" +
            "\"${BlobAccess.digestToString(BlobAccess.sha256Digest(fhirRecordBytes))}\"," +
            "\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)

        val processTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
        assertThat(processTask.processedAt).isNotNull()
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogs = DSL.using(txn)
                .select(ActionLog.ACTION_LOG.asterisk())
                .from(ActionLog.ACTION_LOG)
                .fetchMany()
                .map { it.into(gov.cdc.prime.router.azure.db.tables.pojos.ActionLog::class.java) }
            assertThat(actionLogs.size).isEqualTo(1)
            assertThat(actionLogs[0].size).isEqualTo(2)
            assertThat(actionLogs[0].map { it.detail.message }).isEqualTo(
                listOf(
                    "Missing mapping for code(s): 80382-5",
                    "Missing mapping for code(s): 260373001"
                )
            )
        }
    }

    @Test
    fun `test codeless observation error message`() {
        val report = seedTask(
            Report.Format.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)
        val fhirRecordBytes = fhirengine.azure.codelessFhirRecord.toByteArray()

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )

        mockkObject(BlobAccess)
        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { BlobAccess.downloadBlobAsByteArray(any()) } returns fhirRecordBytes
        every {
            BlobAccess.uploadBody(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns BlobAccess.BlobInfo(Report.Format.FHIR, "", "".toByteArray())
        every { QueueAccess.sendMessage(any(), any()) } returns Unit

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = FHIRConverter(
            metadata,
            settings,
            ReportStreamTestDatabaseContainer.testDatabaseAccess,

            )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine =
            makeWorkflowEngine(
                metadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${report.id}\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-${report.id}.fhir\",\"digest\":" +
            "\"${BlobAccess.digestToString(BlobAccess.sha256Digest(fhirRecordBytes))}\"," +
            "\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,

            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)

        val processTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
        assertThat(processTask.processedAt).isNotNull()
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogs = DSL.using(txn)
                .select(ActionLog.ACTION_LOG.asterisk())
                .from(ActionLog.ACTION_LOG).fetchMany()
                .map { it.into(gov.cdc.prime.router.azure.db.tables.pojos.ActionLog::class.java) }
            assertThat(actionLogs.size).isEqualTo(1)
            assertThat(actionLogs[0].size).isEqualTo(1)
            assertThat(actionLogs[0][0].detail.message).isEqualTo("Observation missing code")
        }
    }
}