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

    // TODO update one of the HL7 tests to send an NIST ELR message
    // TODO add an HL7 test that executes the RADxMARS validation

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