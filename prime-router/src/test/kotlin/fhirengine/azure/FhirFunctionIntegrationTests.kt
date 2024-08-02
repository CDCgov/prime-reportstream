package fhirengine.azure

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
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
import gov.cdc.prime.router.fhirengine.engine.elrDestinationFilterQueueName
import gov.cdc.prime.router.fhirengine.engine.elrTranslationQueueName
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
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
import java.time.OffsetDateTime
import java.util.UUID
import gov.cdc.prime.router.azure.db.tables.ReportFile as RF

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
    """{"resourceType":"Bundle","id":"1721775776533144000.52e90957-c156-45a9-baa1-da5f2b0a973b","meta":{"lastUpdated":"2024-07-23T16:02:56.537-07:00"},"identifier":{"system":"https://reportstream.cdc.gov/prime-router","value":"371784"},"type":"message","timestamp":"2021-02-10T17:07:37.000-08:00","entry":[{"fullUrl":"MessageHeader/4aeed951-99a9-3152-8885-6b0acc6dd35e","resource":{"resourceType":"MessageHeader","id":"4aeed951-99a9-3152-8885-6b0acc6dd35e","meta":{"tag":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0103","code":"P"}]},"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/msh-message-header","extension":[{"url":"MSH.7","valueString":"20210210170737"},{"url":"MSH.15","valueString":"NE"},{"url":"MSH.16","valueString":"NE"},{"url":"MSH.21","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"ELR_Receiver"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.9.99"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"PHLabReportNoAck"}}]}],"eventCoding":{"system":"http://terminology.hl7.org/CodeSystem/v2-0003","code":"R01","display":"ORU^R01^ORU_R01"},"destination":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.5"}],"name":"PRIME_DOH","_endpoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"receiver":{"reference":"Organization/1721775776585592000.ae2231f4-6676-4357-9b70-2c4ad956738b"}}],"sender":{"reference":"Organization/1721775776566284000.2b507720-bc81-466e-bacc-4a8a4d71034a"},"source":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CDC PRIME - Atlanta, Georgia (Dekalb)"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.237821"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.3"}],"software":"PRIME ReportStream","version":"0.1-SNAPSHOT","endpoint":"urn:oid:2.16.840.1.114222.4.1.237821"}}},{"fullUrl":"Organization/1721775776566284000.2b507720-bc81-466e-bacc-4a8a4d71034a","resource":{"resourceType":"Organization","id":"1721775776566284000.2b507720-bc81-466e-bacc-4a8a4d71034a","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"10D0876999"}],"address":[{"country":"USA"}]}},{"fullUrl":"Organization/1721775776585592000.ae2231f4-6676-4357-9b70-2c4ad956738b","resource":{"resourceType":"Organization","id":"1721775776585592000.ae2231f4-6676-4357-9b70-2c4ad956738b","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.6"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Prime ReportStream"}]}},{"fullUrl":"Provenance/1721775776805032000.a1b4e7fc-71e6-4875-844b-d2c8cad3209b","resource":{"resourceType":"Provenance","id":"1721775776805032000.a1b4e7fc-71e6-4875-844b-d2c8cad3209b","target":[{"reference":"MessageHeader/4aeed951-99a9-3152-8885-6b0acc6dd35e"},{"reference":"DiagnosticReport/1721775776971832000.9c36d68a-dbd6-45df-9584-5f03655a9455"}],"recorded":"2021-02-10T17:07:37Z","activity":{"coding":[{"display":"ORU^R01^ORU_R01"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"author"}]},"who":{"reference":"Organization/1721775776804410000.f0435dd8-a842-4e2c-a3c7-886ee9721b91"}}],"entity":[{"role":"source","what":{"reference":"Device/1721775776807384000.d1ecad9e-8635-4873-80ce-a286947706a0"}}]}},{"fullUrl":"Organization/1721775776804410000.f0435dd8-a842-4e2c-a3c7-886ee9721b91","resource":{"resourceType":"Organization","id":"1721775776804410000.f0435dd8-a842-4e2c-a3c7-886ee9721b91","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"10D0876999"}]}},{"fullUrl":"Organization/1721775776807225000.f6594df6-415a-49fa-bcba-69453080b134","resource":{"resourceType":"Organization","id":"1721775776807225000.f6594df6-415a-49fa-bcba-69453080b134","name":"Centers for Disease Control and Prevention"}},{"fullUrl":"Device/1721775776807384000.d1ecad9e-8635-4873-80ce-a286947706a0","resource":{"resourceType":"Device","id":"1721775776807384000.d1ecad9e-8635-4873-80ce-a286947706a0","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org","valueReference":{"reference":"Organization/1721775776807225000.f6594df6-415a-49fa-bcba-69453080b134"}}],"manufacturer":"Centers for Disease Control and Prevention","deviceName":[{"name":"PRIME ReportStream","type":"manufacturer-name"}],"modelNumber":"0.1-SNAPSHOT","version":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-install-date","valueDateTime":"2021-02-10","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20210210"}]}}],"value":"0.1-SNAPSHOT"}]}},{"fullUrl":"Provenance/1721775776812387000.70ce1ee1-7314-47a0-8c45-bae54640b6da","resource":{"resourceType":"Provenance","id":"1721775776812387000.70ce1ee1-7314-47a0-8c45-bae54640b6da","recorded":"2024-07-23T16:02:56Z","policy":[  "http://hl7.org/fhir/uv/v2mappings/message-oru-r01-to-bundle"],"activity":{"coding":[{"code":"v2-FHIR transformation"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"assembler"}]},"who":{"reference":"Organization/1721775776812084000.e757d7ad-d95c-4ccf-8bd1-e47f858928ef"}}]}},{"fullUrl":"Organization/1721775776812084000.e757d7ad-d95c-4ccf-8bd1-e47f858928ef","resource":{"resourceType":"Organization","id":"1721775776812084000.e757d7ad-d95c-4ccf-8bd1-e47f858928ef","identifier":[{"value":"CDC PRIME - Atlanta"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.237821"}]}},{"fullUrl":"Patient/1721775776830354000.2257bbcd-0013-4bbd-a806-0edd8fc55d59","resource":{"resourceType":"Patient","id":"1721775776830354000.2257bbcd-0013-4bbd-a806-0edd8fc55d59","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/pid-patient","extension":[{"url":"PID.8","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"F"}]}},{"url":"PID.30","valueString":"N"}]},{"url":"http://ibm.com/fhir/cdm/StructureDefinition/local-race-cd","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70005"}],"system":"http://terminology.hl7.org/CodeSystem/v3-Race","version":"2.5.1","code":"2106-3","display":"White"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/ethnic-group","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70189"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0189","code":"U","display":"Unknown"}]}}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cx-identifier","extension":[{"url":"CX.5","valueString":"PI"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"PID.3"}],"type":{"coding":[{"code":"PI"}]},"value":"2a14112c-ece1-4f82-915c-7b3a8d152eda","assigner":{"reference":"Organization/1721775776817038000.0d5afe58-de56-4089-8d24-47f1cd7e70a4"}}],"name":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xpn-human-name","extension":[{"url":"XPN.2","valueString":"Kareem"},{"url":"XPN.3","valueString":"Millie"},{"url":"XPN.7","valueString":"L"}]}],"use":"official","family":"Buckridge","given":[  "Kareem","Millie"]}],"telecom":[{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"211"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"2240784"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.1","valueString":"7275555555:1:"},{"url":"XTN.2","valueString":"PRN"},{"url":"XTN.4","valueString":"roscoe.wilkinson@email.com"},{"url":"XTN.7","valueString":"2240784"}]}],"system":"email","use":"home"}],"gender":"female","birthDate":"1958-08-10","_birthDate":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"19580810"}]},"deceasedBoolean":false,"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"688 Leighann Inlet"}]}]}],"line":[  "688 Leighann Inlet"],"city":"South Rodneychester","district":"48077","state":"TX","postalCode":"67071"}]}},{"fullUrl":"Organization/1721775776817038000.0d5afe58-de56-4089-8d24-47f1cd7e70a4","resource":{"resourceType":"Organization","id":"1721775776817038000.0d5afe58-de56-4089-8d24-47f1cd7e70a4","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"}]}},{"fullUrl":"Provenance/1721775776832407000.e1cf01b9-3c96-4405-8095-ed99c5691bfc","resource":{"resourceType":"Provenance","id":"1721775776832407000.e1cf01b9-3c96-4405-8095-ed99c5691bfc","target":[{"reference":"Patient/1721775776830354000.2257bbcd-0013-4bbd-a806-0edd8fc55d59"}],"recorded":"2024-07-23T16:02:56Z","activity":{"coding":[{"system":"https://terminology.hl7.org/CodeSystem/v3-DataOperation","code":"UPDATE"}]}}},{"fullUrl":"Observation/1721775776834966000.ffd72ae3-4175-42ec-bba8-d805497ba30e","resource":{"resourceType":"Observation","id":"1721775776834966000.ffd72ae3-4175-42ec-bba8-d805497ba30e","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/analysis-date-time","valueDateTime":"2021-02-09T00:00:00-06:00","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"},{"url":"OBX.17","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"99ELR"}],"code":"CareStart COVID-19 Antigen test_Access Bio, Inc._EUA"}]}}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1721775776830354000.2257bbcd-0013-4bbd-a806-0edd8fc55d59"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"performer":[{"reference":"Organization/1721775776835812000.b520a468-1329-4509-95d1-fd2e9d0e5a9a"}],"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","code":"260415000","display":"Not detected"}]},"interpretation":[{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70078"}],"code":"N","display":"Normal (applies to non-numeric results)"}]}],"method":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"99ELR"}],"code":"CareStart COVID-19 Antigen test_Access Bio, Inc._EUA"}]}}},{"fullUrl":"Organization/1721775776835812000.b520a468-1329-4509-95d1-fd2e9d0e5a9a","resource":{"resourceType":"Organization","id":"1721775776835812000.b520a468-1329-4509-95d1-fd2e9d0e5a9a","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"10D0876999"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.25"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CLIA"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.4.7"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"10D0876999"}],"name":"Avante at Ormond Beach","address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":[  "170 North King Road"],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}]}},{"fullUrl":"Observation/1721775776838516000.a321bb4c-5331-46ff-8f24-58b477dc40f0","resource":{"resourceType":"Observation","id":"1721775776838516000.a321bb4c-5331-46ff-8f24-58b477dc40f0","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.29","valueId":"QST"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"95418-0","display":"Whether patient is employed in a healthcare setting"}]},"subject":{"reference":"Patient/1721775776830354000.2257bbcd-0013-4bbd-a806-0edd8fc55d59"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"Y","display":"Yes"}]}}},{"fullUrl":"Observation/1721775776840665000.221ebc63-7174-4311-9037-351d5e44cbb1","resource":{"resourceType":"Observation","id":"1721775776840665000.221ebc63-7174-4311-9037-351d5e44cbb1","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.29","valueId":"QST"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"95417-2","display":"First test for condition of interest"}]},"subject":{"reference":"Patient/1721775776830354000.2257bbcd-0013-4bbd-a806-0edd8fc55d59"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"Y","display":"Yes"}]}}},{"fullUrl":"Observation/1721775776842590000.811192a7-dc1e-42f7-98a6-5f74bbee9fd7","resource":{"resourceType":"Observation","id":"1721775776842590000.811192a7-dc1e-42f7-98a6-5f74bbee9fd7","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.29","valueId":"QST"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"95421-4","display":"Resides in a congregate care setting"}]},"subject":{"reference":"Patient/1721775776830354000.2257bbcd-0013-4bbd-a806-0edd8fc55d59"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"N","display":"No"}]}}},{"fullUrl":"Observation/1721775776844377000.282c8de6-0eb8-42e0-854c-c0786dd5c537","resource":{"resourceType":"Observation","id":"1721775776844377000.282c8de6-0eb8-42e0-854c-c0786dd5c537","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.29","valueId":"QST"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"95419-8","display":"Has symptoms related to condition of interest"}]},"subject":{"reference":"Patient/1721775776830354000.2257bbcd-0013-4bbd-a806-0edd8fc55d59"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"N","display":"No"}]}}},{"fullUrl":"Specimen/1721775776957885000.4c690364-0ae3-4ca7-aa56-56994053ed4e","resource":{"resourceType":"Specimen","id":"1721775776957885000.4c690364-0ae3-4ca7-aa56-56994053ed4e","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"OBR"}]}},{"fullUrl":"Specimen/1721775776959452000.ea1a493c-0286-47f1-95c6-39d744eb33df","resource":{"resourceType":"Specimen","id":"1721775776959452000.ea1a493c-0286-47f1-95c6-39d744eb33df","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"SPM"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Component","valueString":"SPM.2.1"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PGN"}]},"value":"0cba76f5-35e0-4a28-803a-2f31308aae9b"}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","code":"258500001","display":"Nasopharyngeal swab"}]},"receivedTime":"2021-02-09T00:00:00-06:00","_receivedTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"collection":{"collectedDateTime":"2021-02-09T00:00:00-06:00","_collectedDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"bodySite":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"2020-09-01","code":"71836000","display":"Nasopharyngeal structure (body structure)"}]}}}},{"fullUrl":"ServiceRequest/1721775776968140000.24451912-efe6-4a87-bece-6e5452ed6d45","resource":{"resourceType":"ServiceRequest","id":"1721775776968140000.24451912-efe6-4a87-bece-6e5452ed6d45","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/business-event","valueCode":"RE"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/orc-common-order","extension":[{"url":"orc-21-ordering-facility-name","valueReference":{"reference":"Organization/1721775776964483000.c4fb82ad-39a9-4126-8836-bcf27f9b5532"}},{"url":"orc-22-ordering-facility-address","valueAddress":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":["170 North King Road"  ],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}},{"url":"orc-24-ordering-provider-address","valueAddress":{"postalCode":"32174"}},{"url":"orc-12-ordering-provider","valueReference":{"reference":"Practitioner/1721775776966085000.2696d71e-96bd-4ae2-94b1-1e43b7165baa"}},{"url":"ORC.15","valueString":"20210209"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request","extension":[{"url":"OBR.2","valueIdentifier":{"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}},{"url":"OBR.3","valueIdentifier":{"value":"0cba76f5-35e0-4a28-803a-2f31308aae9b"}},{"url":"OBR.22","valueString":"202102090000-0600"},{"url":"OBR.25","valueId":"F"},{"url":"OBR.16","valueReference":{"reference":"Practitioner/1721775776966998000.52902516-e7c6-4622-96ec-f154955df5d8"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"386"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"6825220"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.7","valueString":"6825220"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBR.17"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"use":"work"}}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}],"status":"unknown","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1721775776830354000.2257bbcd-0013-4bbd-a806-0edd8fc55d59"},"requester":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"386"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"6825220"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.7","valueString":"6825220"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.14"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"use":"work"}}],"reference":"PractitionerRole/1721775776960422000.39cd383b-a80f-4157-9024-7c0d9b55db19"}}},{"fullUrl":"Practitioner/1721775776961366000.b5a30d9c-771e-41ce-b606-1b6ed766c330","resource":{"resourceType":"Practitioner","id":"1721775776961366000.b5a30d9c-771e-41ce-b606-1b6ed766c330","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.12"}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":[  "Husam"]}],"address":[{"postalCode":"32174"}]}},{"fullUrl":"Organization/1721775776962726000.e310d472-2eee-415a-9148-e7837e242e69","resource":{"resourceType":"Organization","id":"1721775776962726000.e310d472-2eee-415a-9148-e7837e242e69","name":"Avante at Ormond Beach","telecom":[{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"407"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"7397506"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.4","valueString":"jbrush@avantecenters.com"},{"url":"XTN.7","valueString":"7397506"}]}],"system":"email","use":"work"}],"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":[  "170 North King Road"],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}]}},{"fullUrl":"PractitionerRole/1721775776960422000.39cd383b-a80f-4157-9024-7c0d9b55db19","resource":{"resourceType":"PractitionerRole","id":"1721775776960422000.39cd383b-a80f-4157-9024-7c0d9b55db19","practitioner":{"reference":"Practitioner/1721775776961366000.b5a30d9c-771e-41ce-b606-1b6ed766c330"},"organization":{"reference":"Organization/1721775776962726000.e310d472-2eee-415a-9148-e7837e242e69"}}},{"fullUrl":"Organization/1721775776964483000.c4fb82ad-39a9-4126-8836-bcf27f9b5532","resource":{"resourceType":"Organization","id":"1721775776964483000.c4fb82ad-39a9-4126-8836-bcf27f9b5532","name":"Avante at Ormond Beach"}},{"fullUrl":"Practitioner/1721775776966085000.2696d71e-96bd-4ae2-94b1-1e43b7165baa","resource":{"resourceType":"Practitioner","id":"1721775776966085000.2696d71e-96bd-4ae2-94b1-1e43b7165baa","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":[  "Husam"]}]}},{"fullUrl":"Practitioner/1721775776966998000.52902516-e7c6-4622-96ec-f154955df5d8","resource":{"resourceType":"Practitioner","id":"1721775776966998000.52902516-e7c6-4622-96ec-f154955df5d8","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":[  "Husam"]}]}},{"fullUrl":"DiagnosticReport/1721775776971832000.9c36d68a-dbd6-45df-9584-5f03655a9455","resource":{"resourceType":"DiagnosticReport","id":"1721775776971832000.9c36d68a-dbd6-45df-9584-5f03655a9455","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}],"basedOn":[{"reference":"ServiceRequest/1721775776968140000.24451912-efe6-4a87-bece-6e5452ed6d45"}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1721775776830354000.2257bbcd-0013-4bbd-a806-0edd8fc55d59"},"effectivePeriod":{"start":"2021-02-09T00:00:00-06:00","_start":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"end":"2021-02-09T00:00:00-06:00","_end":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]}},"issued":"2021-02-09T00:00:00-06:00","_issued":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"specimen":[{"reference":"Specimen/1721775776959452000.ea1a493c-0286-47f1-95c6-39d744eb33df"},{"reference":"Specimen/1721775776957885000.4c690364-0ae3-4ca7-aa56-56994053ed4e"}],"result":[{"reference":"Observation/1721775776834966000.ffd72ae3-4175-42ec-bba8-d805497ba30e"},{"reference":"Observation/1721775776838516000.a321bb4c-5331-46ff-8f24-58b477dc40f0"},{"reference":"Observation/1721775776840665000.221ebc63-7174-4311-9037-351d5e44cbb1"},{"reference":"Observation/1721775776842590000.811192a7-dc1e-42f7-98a6-5f74bbee9fd7"},{"reference":"Observation/1721775776844377000.282c8de6-0eb8-42e0-854c-c0786dd5c537"}]}}]}"""

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
    """{"resourceType":"Bundle","id":"1721775717664426000.66d913af-5494-4bca-a8b0-0a5f5a3e77e8","meta":{"lastUpdated":"2024-07-23T16:01:57.668-07:00"},"identifier":{"system":"https://reportstream.cdc.gov/prime-router","value":"371784"},"type":"message","timestamp":"2021-02-10T17:07:37.000-08:00","entry":[{"fullUrl":"MessageHeader/4aeed951-99a9-3152-8885-6b0acc6dd35e","resource":{"resourceType":"MessageHeader","id":"4aeed951-99a9-3152-8885-6b0acc6dd35e","meta":{"tag":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0103","code":"P"}]},"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/msh-message-header","extension":[{"url":"MSH.7","valueString":"20210210170737"},{"url":"MSH.15","valueString":"NE"},{"url":"MSH.16","valueString":"NE"},{"url":"MSH.21","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"ELR_Receiver"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.9.99"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"PHLabReportNoAck"}}]}],"eventCoding":{"system":"http://terminology.hl7.org/CodeSystem/v2-0003","code":"R01","display":"ORU^R01^ORU_R01"},"destination":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.5"}],"name":"PRIME_DOH","_endpoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"receiver":{"reference":"Organization/1721775717716406000.59452b39-c64b-48ae-b84b-9719e86b2897"}}],"sender":{"reference":"Organization/1721775717697012000.d7832fa1-d9ac-4783-b945-9a09d201c5f6"},"source":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CDC PRIME - Atlanta, Georgia (Dekalb)"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.237821"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.3"}],"software":"PRIME ReportStream","version":"0.1-SNAPSHOT","endpoint":"urn:oid:2.16.840.1.114222.4.1.237821"}}},{"fullUrl":"Organization/1721775717697012000.d7832fa1-d9ac-4783-b945-9a09d201c5f6","resource":{"resourceType":"Organization","id":"1721775717697012000.d7832fa1-d9ac-4783-b945-9a09d201c5f6","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"10D0876999"}],"address":[{"country":"USA"}]}},{"fullUrl":"Organization/1721775717716406000.59452b39-c64b-48ae-b84b-9719e86b2897","resource":{"resourceType":"Organization","id":"1721775717716406000.59452b39-c64b-48ae-b84b-9719e86b2897","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.6"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Prime ReportStream"}]}},{"fullUrl":"Provenance/1721775717929077000.751aef07-8acb-4b93-9564-da41a978387d","resource":{"resourceType":"Provenance","id":"1721775717929077000.751aef07-8acb-4b93-9564-da41a978387d","target":[{"reference":"MessageHeader/4aeed951-99a9-3152-8885-6b0acc6dd35e"},{"reference":"DiagnosticReport/1721775718089719000.b8b7c7e0-7b48-469c-87eb-d603ef73f86e"}],"recorded":"2021-02-10T17:07:37Z","activity":{"coding":[{"display":"ORU^R01^ORU_R01"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"author"}]},"who":{"reference":"Organization/1721775717928438000.a75b2746-83b5-4475-a282-1e7d113ab97d"}}],"entity":[{"role":"source","what":{"reference":"Device/1721775717931232000.93093895-1eff-48db-aa06-fac559fa2b6c"}}]}},{"fullUrl":"Organization/1721775717928438000.a75b2746-83b5-4475-a282-1e7d113ab97d","resource":{"resourceType":"Organization","id":"1721775717928438000.a75b2746-83b5-4475-a282-1e7d113ab97d","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"10D0876999"}]}},{"fullUrl":"Organization/1721775717931073000.5f40ef81-53fa-4044-8556-94a2971f20a9","resource":{"resourceType":"Organization","id":"1721775717931073000.5f40ef81-53fa-4044-8556-94a2971f20a9","name":"Centers for Disease Control and Prevention"}},{"fullUrl":"Device/1721775717931232000.93093895-1eff-48db-aa06-fac559fa2b6c","resource":{"resourceType":"Device","id":"1721775717931232000.93093895-1eff-48db-aa06-fac559fa2b6c","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org","valueReference":{"reference":"Organization/1721775717931073000.5f40ef81-53fa-4044-8556-94a2971f20a9"}}],"manufacturer":"Centers for Disease Control and Prevention","deviceName":[{"name":"PRIME ReportStream","type":"manufacturer-name"}],"modelNumber":"0.1-SNAPSHOT","version":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-install-date","valueDateTime":"2021-02-10","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20210210"}]}}],"value":"0.1-SNAPSHOT"}]}},{"fullUrl":"Provenance/1721775717936130000.70563e6a-0a51-4c49-aadb-9f40368cdcb0","resource":{"resourceType":"Provenance","id":"1721775717936130000.70563e6a-0a51-4c49-aadb-9f40368cdcb0","recorded":"2024-07-23T16:01:57Z","policy":["http://hl7.org/fhir/uv/v2mappings/message-oru-r01-to-bundle"],"activity":{"coding":[{"code":"v2-FHIR transformation"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"assembler"}]},"who":{"reference":"Organization/1721775717935900000.fbeb07b5-0aee-4170-868a-9fa68d05d5b3"}}]}},{"fullUrl":"Organization/1721775717935900000.fbeb07b5-0aee-4170-868a-9fa68d05d5b3","resource":{"resourceType":"Organization","id":"1721775717935900000.fbeb07b5-0aee-4170-868a-9fa68d05d5b3","identifier":[{"value":"CDC PRIME - Atlanta"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.237821"}]}},{"fullUrl":"Patient/1721775717953663000.616a5fe9-d5d8-42d3-bf31-8ddb1859bfe2","resource":{"resourceType":"Patient","id":"1721775717953663000.616a5fe9-d5d8-42d3-bf31-8ddb1859bfe2","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/pid-patient","extension":[{"url":"PID.8","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"F"}]}},{"url":"PID.30","valueString":"N"}]},{"url":"http://ibm.com/fhir/cdm/StructureDefinition/local-race-cd","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70005"}],"system":"http://terminology.hl7.org/CodeSystem/v3-Race","version":"2.5.1","code":"2106-3","display":"White"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/ethnic-group","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70189"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0189","code":"U","display":"Unknown"}]}}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cx-identifier","extension":[{"url":"CX.5","valueString":"PI"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"PID.3"}],"type":{"coding":[{"code":"PI"}]},"value":"2a14112c-ece1-4f82-915c-7b3a8d152eda","assigner":{"reference":"Organization/1721775717940696000.308f33b4-8ae1-4f8b-9f84-520a0535d1a3"}}],"name":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xpn-human-name","extension":[{"url":"XPN.2","valueString":"Kareem"},{"url":"XPN.3","valueString":"Millie"},{"url":"XPN.7","valueString":"L"}]}],"use":"official","family":"Buckridge","given":["Kareem", "Millie"]}],"telecom":[{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"211"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"2240784"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.1","valueString":"7275555555:1:"},{"url":"XTN.2","valueString":"PRN"},{"url":"XTN.4","valueString":"roscoe.wilkinson@email.com"},{"url":"XTN.7","valueString":"2240784"}]}],"system":"email","use":"home"}],"gender":"female","birthDate":"1958-08-10","_birthDate":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"19580810"}]},"deceasedBoolean":false,"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"688 Leighann Inlet"}]}]}],"line":["688 Leighann Inlet"],"city":"South Rodneychester","district":"48077","state":"TX","postalCode":"67071"}]}},{"fullUrl":"Organization/1721775717940696000.308f33b4-8ae1-4f8b-9f84-520a0535d1a3","resource":{"resourceType":"Organization","id":"1721775717940696000.308f33b4-8ae1-4f8b-9f84-520a0535d1a3","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"Avante at Ormond Beach"}]}},{"fullUrl":"Provenance/1721775717955884000.e4aa6f85-a08f-438c-9688-847a255bd1cd","resource":{"resourceType":"Provenance","id":"1721775717955884000.e4aa6f85-a08f-438c-9688-847a255bd1cd","target":[{"reference":"Patient/1721775717953663000.616a5fe9-d5d8-42d3-bf31-8ddb1859bfe2"}],"recorded":"2024-07-23T16:01:57Z","activity":{"coding":[{"system":"https://terminology.hl7.org/CodeSystem/v3-DataOperation","code":"UPDATE"}]}}},{"fullUrl":"Observation/1721775717958520000.bf80d599-759c-4883-8d9b-c141a17e2c16","resource":{"resourceType":"Observation","id":"1721775717958520000.bf80d599-759c-4883-8d9b-c141a17e2c16","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/analysis-date-time","valueDateTime":"2021-02-09T00:00:00-06:00","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"},{"url":"OBX.17","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"99ELR"}],"code":"CareStart COVID-19 Antigen test_Access Bio, Inc._EUA"}]}}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1721775717953663000.616a5fe9-d5d8-42d3-bf31-8ddb1859bfe2"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"performer":[{"reference":"Organization/1721775717959326000.79f6a053-36a3-462b-a717-d7f5c69aafac"}],"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","code":"260415000","display":"Not detected"}]},"interpretation":[{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70078"}],"code":"N","display":"Normal (applies to non-numeric results)"}]}],"method":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"99ELR"}],"code":"CareStart COVID-19 Antigen test_Access Bio, Inc._EUA"}]}}},{"fullUrl":"Organization/1721775717959326000.79f6a053-36a3-462b-a717-d7f5c69aafac","resource":{"resourceType":"Organization","id":"1721775717959326000.79f6a053-36a3-462b-a717-d7f5c69aafac","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"10D0876999"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.25"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CLIA"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.4.7"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"10D0876999"}],"name":"Avante at Ormond Beach","address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":["170 North King Road"],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}]}},{"fullUrl":"Observation/1721775717961843000.d8bf1eea-41bb-4078-9b9a-92012fdab498","resource":{"resourceType":"Observation","id":"1721775717961843000.d8bf1eea-41bb-4078-9b9a-92012fdab498","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.29","valueId":"QST"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"95418-0","display":"Whether patient is employed in a healthcare setting"}]},"subject":{"reference":"Patient/1721775717953663000.616a5fe9-d5d8-42d3-bf31-8ddb1859bfe2"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"Y","display":"Yes"}]}}},{"fullUrl":"Observation/1721775717963651000.2d3f3e50-9e7d-47ba-a979-fa1bb371be3b","resource":{"resourceType":"Observation","id":"1721775717963651000.2d3f3e50-9e7d-47ba-a979-fa1bb371be3b","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.29","valueId":"QST"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"95417-2","display":"First test for condition of interest"}]},"subject":{"reference":"Patient/1721775717953663000.616a5fe9-d5d8-42d3-bf31-8ddb1859bfe2"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"Y","display":"Yes"}]}}},{"fullUrl":"Observation/1721775717965402000.fa09fb2e-5077-4ff2-b5d3-4885368d1086","resource":{"resourceType":"Observation","id":"1721775717965402000.fa09fb2e-5077-4ff2-b5d3-4885368d1086","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.29","valueId":"QST"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"95421-4","display":"Resides in a congregate care setting"}]},"subject":{"reference":"Patient/1721775717953663000.616a5fe9-d5d8-42d3-bf31-8ddb1859bfe2"},"effectiveDateTime":"2021-02-09T00:00:00-06:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70136"}],"code":"N","display":"No"}]}}},{"fullUrl":"Specimen/1721775718076402000.112c980e-a3b1-40a7-828c-6158c85c56d0","resource":{"resourceType":"Specimen","id":"1721775718076402000.112c980e-a3b1-40a7-828c-6158c85c56d0","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"OBR"}]}},{"fullUrl":"Specimen/1721775718077941000.1da52bde-ba58-41ad-ae60-9d56be69e1a7","resource":{"resourceType":"Specimen","id":"1721775718077941000.1da52bde-ba58-41ad-ae60-9d56be69e1a7","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"SPM"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Component","valueString":"SPM.2.1"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PGN"}]},"value":"0cba76f5-35e0-4a28-803a-2f31308aae9b"}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","code":"258500001","display":"Nasopharyngeal swab"}]},"receivedTime":"2021-02-09T00:00:00-06:00","_receivedTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"collection":{"collectedDateTime":"2021-02-09T00:00:00-06:00","_collectedDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"bodySite":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"2020-09-01","code":"71836000","display":"Nasopharyngeal structure (body structure)"}]}}}},{"fullUrl":"ServiceRequest/1721775718086061000.85aec36c-8155-461d-b76a-8c3004863d99","resource":{"resourceType":"ServiceRequest","id":"1721775718086061000.85aec36c-8155-461d-b76a-8c3004863d99","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/business-event","valueCode":"RE"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/orc-common-order","extension":[{"url":"orc-21-ordering-facility-name","valueReference":{"reference":"Organization/1721775718082341000.7544116b-6b82-4c6c-9cd7-cc5885653b79"}},{"url":"orc-22-ordering-facility-address","valueAddress":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":["170 North King Road"],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}},{"url":"orc-24-ordering-provider-address","valueAddress":{"postalCode":"32174"}},{"url":"orc-12-ordering-provider","valueReference":{"reference":"Practitioner/1721775718083859000.10522a2e-d32a-4458-98bf-7269958ca971"}},{"url":"ORC.15","valueString":"20210209"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request","extension":[{"url":"OBR.2","valueIdentifier":{"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}},{"url":"OBR.3","valueIdentifier":{"value":"0cba76f5-35e0-4a28-803a-2f31308aae9b"}},{"url":"OBR.22","valueString":"202102090000-0600"},{"url":"OBR.25","valueId":"F"},{"url":"OBR.16","valueReference":{"reference":"Practitioner/1721775718085111000.4e06db72-beaf-4e16-b3ac-d7e3b09f5bab"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"386"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"6825220"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.7","valueString":"6825220"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBR.17"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"use":"work"}}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}],"status":"unknown","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1721775717953663000.616a5fe9-d5d8-42d3-bf31-8ddb1859bfe2"},"requester":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"386"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"6825220"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.7","valueString":"6825220"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.14"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"use":"work"}}],"reference":"PractitionerRole/1721775718079004000.bc40ad69-0cb9-4d7a-8b31-286e3035ebd5"}}},{"fullUrl":"Practitioner/1721775718079998000.4b4f67f3-17ce-4cf1-9911-b7e4c91a9530","resource":{"resourceType":"Practitioner","id":"1721775718079998000.4b4f67f3-17ce-4cf1-9911-b7e4c91a9530","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.12"}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":["Husam"]}],"address":[{"postalCode":"32174"}]}},{"fullUrl":"Organization/1721775718080844000.7b0033c4-ccaa-4ab9-88b3-019ed2734097","resource":{"resourceType":"Organization","id":"1721775718080844000.7b0033c4-ccaa-4ab9-88b3-019ed2734097","name":"Avante at Ormond Beach","telecom":[{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-country","valueString":"1"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"407"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"7397506"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.4","valueString":"jbrush@avantecenters.com"},{"url":"XTN.7","valueString":"7397506"}]}],"system":"email","use":"work"}],"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"170 North King Road"}]}]}],"line":["170 North King Road"],"city":"Ormond Beach","district":"12127","state":"FL","postalCode":"32174"}]}},{"fullUrl":"PractitionerRole/1721775718079004000.bc40ad69-0cb9-4d7a-8b31-286e3035ebd5","resource":{"resourceType":"PractitionerRole","id":"1721775718079004000.bc40ad69-0cb9-4d7a-8b31-286e3035ebd5","practitioner":{"reference":"Practitioner/1721775718079998000.4b4f67f3-17ce-4cf1-9911-b7e4c91a9530"},"organization":{"reference":"Organization/1721775718080844000.7b0033c4-ccaa-4ab9-88b3-019ed2734097"}}},{"fullUrl":"Organization/1721775718082341000.7544116b-6b82-4c6c-9cd7-cc5885653b79","resource":{"resourceType":"Organization","id":"1721775718082341000.7544116b-6b82-4c6c-9cd7-cc5885653b79","name":"Avante at Ormond Beach"}},{"fullUrl":"Practitioner/1721775718083859000.10522a2e-d32a-4458-98bf-7269958ca971","resource":{"resourceType":"Practitioner","id":"1721775718083859000.10522a2e-d32a-4458-98bf-7269958ca971","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":["Husam"]}]}},{"fullUrl":"Practitioner/1721775718085111000.4e06db72-beaf-4e16-b3ac-d7e3b09f5bab","resource":{"resourceType":"Practitioner","id":"1721775718085111000.4e06db72-beaf-4e16-b3ac-d7e3b09f5bab","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CMS"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.249"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Husam"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"NPI"}]},"system":"CMS","value":"1629082607"}],"name":[{"family":"Eddin","given":["Husam"]}]}},{"fullUrl":"DiagnosticReport/1721775718089719000.b8b7c7e0-7b48-469c-87eb-d603ef73f86e","resource":{"resourceType":"DiagnosticReport","id":"1721775718089719000.b8b7c7e0-7b48-469c-87eb-d603ef73f86e","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"73a6e9bd-aaec-418e-813a-0ad33366ca85"}],"basedOn":[{"reference":"ServiceRequest/1721775718086061000.85aec36c-8155-461d-b76a-8c3004863d99"}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1721775717953663000.616a5fe9-d5d8-42d3-bf31-8ddb1859bfe2"},"effectivePeriod":{"start":"2021-02-09T00:00:00-06:00","_start":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"end":"2021-02-09T00:00:00-06:00","_end":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]}},"issued":"2021-02-09T00:00:00-06:00","_issued":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202102090000-0600"}]},"specimen":[{"reference":"Specimen/1721775718077941000.1da52bde-ba58-41ad-ae60-9d56be69e1a7"},{"reference":"Specimen/1721775718076402000.112c980e-a3b1-40a7-828c-6158c85c56d0"}],"result":[{"reference":"Observation/1721775717958520000.bf80d599-759c-4883-8d9b-c141a17e2c16"},{"reference":"Observation/1721775717961843000.d8bf1eea-41bb-4078-9b9a-92012fdab498"},{"reference":"Observation/1721775717963651000.2d3f3e50-9e7d-47ba-a979-fa1bb371be3b"},{"reference":"Observation/1721775717965402000.fa09fb2e-5077-4ff2-b5d3-4885368d1086"}]}}]}"""

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
    """{"resourceType":"Bundle","id":"1721763140613584000.b0413b94-e2f5-4ab2-a899-15c37de79867","meta":{"lastUpdated":"2024-07-23T12:32:20.617-07:00"},"identifier":{"system":"https://reportstream.cdc.gov/prime-router","value":"3003786103_4988249_33033"},"type":"message","timestamp":"2023-05-01T07:25:31.000-07:00","entry":[{"fullUrl":"MessageHeader/0993dd0b-6ce5-3caf-a177-0b81cc780c18","resource":{"resourceType":"MessageHeader","id":"0993dd0b-6ce5-3caf-a177-0b81cc780c18","meta":{"tag":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0103","code":"T"}]},"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/encoding-characters","valueString":"^~\\&#"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/msh-message-header","extension":[{"url":"MSH.7","valueString":"20230501102531-0400"},{"url":"MSH.15","valueString":"NE"},{"url":"MSH.16","valueString":"NE"},{"url":"MSH.21","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"PHIN"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.9.11"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"PHLabReport-NoAck"}}]}],"eventCoding":{"system":"http://terminology.hl7.org/CodeSystem/v2-0003","code":"R01","display":"ORU^R01^ORU_R01"},"destination":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.6.2.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.5"}],"name":"MEDSS-ELR ","endpoint":"urn:oid:2.16.840.1.114222.4.3.3.6.2.1","receiver":{"reference":"Organization/1721763140672047000.1ce23b86-3239-4036-86b9-7a604f62e5cc"}}],"sender":{"reference":"Organization/1721763140646477000.6065230c-8a76-4ee0-b78b-ba396a48a685"},"source":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.3"}],"software":"STARLIMS","version":"ELIMS V11","endpoint":"urn:oid:2.16.840.1.114222.4.3.3.2.1.2"}}},{"fullUrl":"Organization/1721763140646477000.6065230c-8a76-4ee0-b78b-ba396a48a685","resource":{"resourceType":"Organization","id":"1721763140646477000.6065230c-8a76-4ee0-b78b-ba396a48a685","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"CDC Atlanta"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"11D0668319"}],"address":[{"country":"USA"}]}},{"fullUrl":"Organization/1721763140672047000.1ce23b86-3239-4036-86b9-7a604f62e5cc","resource":{"resourceType":"Organization","id":"1721763140672047000.1ce23b86-3239-4036-86b9-7a604f62e5cc","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.6"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"MNDOH"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"ISO"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.3661"}]}},{"fullUrl":"Provenance/1721763140902561000.032e82d9-07bf-4664-8145-a764f4039ada","resource":{"resourceType":"Provenance","id":"1721763140902561000.032e82d9-07bf-4664-8145-a764f4039ada","target":[{"reference":"MessageHeader/0993dd0b-6ce5-3caf-a177-0b81cc780c18"},{"reference":"DiagnosticReport/1721763141066965000.b397146d-3575-49d0-99bd-8bad7ccb6c13"}],"recorded":"2023-05-01T10:25:31-04:00","activity":{"coding":[{"display":"ORU^R01^ORU_R01"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"author"}]},"who":{"reference":"Organization/1721763140901869000.6b5d95b1-d74f-41bf-b7b0-c1db2a8f365b"}}],"entity":[{"role":"source","what":{"reference":"Device/1721763140905397000.9ec38ba1-ff42-4623-8abd-beef0a3d3cff"}}]}},{"fullUrl":"Organization/1721763140901869000.6b5d95b1-d74f-41bf-b7b0-c1db2a8f365b","resource":{"resourceType":"Organization","id":"1721763140901869000.6b5d95b1-d74f-41bf-b7b0-c1db2a8f365b","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"CDC Atlanta"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"11D0668319"}]}},{"fullUrl":"Organization/1721763140905141000.b9cc7ce1-7043-4c60-ba65-4aac34b697a7","resource":{"resourceType":"Organization","id":"1721763140905141000.b9cc7ce1-7043-4c60-ba65-4aac34b697a7","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"CDC CLIA"}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CDC"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/code-index-name","valueString":"identifier"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"XX"}]},"value":"CDC CLIA"}],"name":"CDC"}},{"fullUrl":"Device/1721763140905397000.9ec38ba1-ff42-4623-8abd-beef0a3d3cff","resource":{"resourceType":"Device","id":"1721763140905397000.9ec38ba1-ff42-4623-8abd-beef0a3d3cff","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org","valueReference":{"reference":"Organization/1721763140905141000.b9cc7ce1-7043-4c60-ba65-4aac34b697a7"}}],"manufacturer":"CDC","deviceName":[{"name":"STARLIMS","type":"manufacturer-name"}],"modelNumber":"Binary ID unknown","version":[{"value":"ELIMS V11"}]}},{"fullUrl":"Provenance/1721763140910038000.2481c12c-8753-4109-8f39-6114c23b06fc","resource":{"resourceType":"Provenance","id":"1721763140910038000.2481c12c-8753-4109-8f39-6114c23b06fc","recorded":"2024-07-23T12:32:20Z","policy":[ "http://hl7.org/fhir/uv/v2mappings/message-oru-r01-to-bundle" ],"activity":{"coding":[{"code":"v2-FHIR transformation"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"assembler"}]},"who":{"reference":"Organization/1721763140909771000.618b520c-7981-47ce-bf46-e88b9cef15e9"}}]}},{"fullUrl":"Organization/1721763140909771000.618b520c-7981-47ce-bf46-e88b9cef15e9","resource":{"resourceType":"Organization","id":"1721763140909771000.618b520c-7981-47ce-bf46-e88b9cef15e9","identifier":[{"value":"CDC PRIME - Atlanta"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.237821"}]}},{"fullUrl":"Patient/1721763140922533000.1a511090-a57c-4203-b893-d6c44bfcfe7a","resource":{"resourceType":"Patient","id":"1721763140922533000.1a511090-a57c-4203-b893-d6c44bfcfe7a","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/patient-notes","valueAnnotation":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/nte-annotation","extension":[{"url":"NTE.2","valueId":"L"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/note-comment","valueId":"SPHL Submitter: MN PHL Division, Minnesota Department of Health, Submitter ID: SPHL-000034, Address: 601 Robert St. N.  St. Paul, Minnesota 55164-0899 United States, Email: Health.idlabreports@state.mn.us, Submitter Patient ID: 10171284, Submitter Alt Patient ID: , Submitter Specimen ID: 230011927, Submitter Alt Specimen ID:"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/note-type","valueCodeableConcept":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/coding-system-oid","valueOid":"urn:oid:2.16.840.1.113883.12.364"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"HL70364"}],"version":"2.5.1","code":"RE","display":"Remark"}]}}],"text":"SPHL Submitter: MN PHL Division, Minnesota Department of Health, Submitter ID: SPHL-000034, Address: 601 Robert St. N.  St. Paul, Minnesota 55164-0899 United States, Email: Health.idlabreports@state.mn.us, Submitter Patient ID: 10171284, Submitter Alt Patient ID: , Submitter Specimen ID: 230011927, Submitter Alt Specimen ID:"}}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cx-identifier","extension":[{"url":"CX.5","valueString":"PI"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"PID.3"}],"type":{"coding":[{"code":"PI"}]},"system":"STARLIMS.CDC.Stag","_system":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"}]},"value":"PID03953346"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cx-identifier","extension":[{"url":"CX.5","valueString":"PI"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"PID.3"}],"type":{"coding":[{"code":"PI"}]},"system":"SPHL-000034","_system":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"}]},"value":"10171284"}],"name":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xpn-human-name","extension":[{"url":"XPN.7","valueString":"U"}]}]}],"_birthDate":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"0000"}]},"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"XAD.7","valueCode":"H"}]}],"use":"home","country":"USA"}]}},{"fullUrl":"Provenance/1721763140923818000.e0aa95c6-dcc4-4b41-ba1a-ab43394ba2fa","resource":{"resourceType":"Provenance","id":"1721763140923818000.e0aa95c6-dcc4-4b41-ba1a-ab43394ba2fa","target":[{"reference":"Patient/1721763140922533000.1a511090-a57c-4203-b893-d6c44bfcfe7a"}],"recorded":"2024-07-23T12:32:20Z","activity":{"coding":[{"system":"https://terminology.hl7.org/CodeSystem/v3-DataOperation","code":"UPDATE"}]}}},{"fullUrl":"Observation/1721763140929139000.f59fff56-c4c8-4b35-9014-cf139baf91de","resource":{"resourceType":"Observation","id":"1721763140929139000.f59fff56-c4c8-4b35-9014-cf139baf91de","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sub-id","valueString":"N8KHKA9H-1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/analysis-date-time","valueDateTime":"2023-04-27T09:29:00Z","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20230427092900"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"PLT"}],"version":"2.69","code":"PLT1228","display":"Mold and Yeast XXX MS.MALDI-TOF"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","version":"v_unknown","code":"3562","display":"MALDI-TOF-CLIA"}],"text":"MALDI-TOF-CLIA"},"subject":{"reference":"Patient/1721763140922533000.1a511090-a57c-4203-b893-d6c44bfcfe7a"},"effectiveDateTime":"2023-03-22","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20230322"}]},"performer":[{"reference":"Organization/1721763140929581000.c28d0379-529e-403d-9fc9-0b1625f398eb"},{"reference":"PractitionerRole/1721763140929780000.0a000816-ae16-434d-bc63-940d9a489951"},{"reference":"Organization/1721763140931396000.e47a654e-a0d8-4949-aba2-a3a28d4bd6ff"}],"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"09012018","code":"712760003","display":"Candida metapsilosis (organism)"}],"text":"Candida metapsilosis"}}},{"fullUrl":"Organization/1721763140929581000.c28d0379-529e-403d-9fc9-0b1625f398eb","resource":{"resourceType":"Organization","id":"1721763140929581000.c28d0379-529e-403d-9fc9-0b1625f398eb","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-organization","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"CLIA"}],"code":"11D0668319","display":"Centers for Disease Control and Prevention"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","code":"40","display":"Fungus Reference Laboratory"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.15"}],"identifier":[{"system":"CLIA","value":"11D0668319"}],"name":"Centers for Disease Control and Prevention"}},{"fullUrl":"Practitioner/1721763140930188000.22b0c14c-c5f3-41a6-8596-66f250b9d759","resource":{"resourceType":"Practitioner","id":"1721763140930188000.22b0c14c-c5f3-41a6-8596-66f250b9d759","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xcn-practitioner","extension":[{"url":"XCN.3","valueString":"Lalitha"}]}],"identifier":[{"value":"HVR0@cdc.gov"}],"name":[{"family":"Gade","given":[ "Lalitha" ]}]}},{"fullUrl":"PractitionerRole/1721763140929780000.0a000816-ae16-434d-bc63-940d9a489951","resource":{"resourceType":"PractitionerRole","id":"1721763140929780000.0a000816-ae16-434d-bc63-940d9a489951","practitioner":{"reference":"Practitioner/1721763140930188000.22b0c14c-c5f3-41a6-8596-66f250b9d759"},"code":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/practitioner-role","code":"responsibleObserver"}]}]}},{"fullUrl":"Organization/1721763140931396000.e47a654e-a0d8-4949-aba2-a3a28d4bd6ff","resource":{"resourceType":"Organization","id":"1721763140931396000.e47a654e-a0d8-4949-aba2-a3a28d4bd6ff","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/organization-name-type","valueCoding":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueCodeableConcept":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"XON.2"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"L"}]}}],"code":"L"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"11D0668319"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.25"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"CLIA"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.4.7"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/code-index-name","valueString":"identifier"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"XX"}]},"value":"11D0668319"}],"name":"Centers for Disease Control and Prevention","address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"1600 Clifton Rd"}]},{"url":"XAD.7","valueCode":"B"}]}],"use":"work","line":[ "1600 Clifton Rd" ],"city":"Atlanta","state":"GA","postalCode":"30329","country":"USA"}]}},{"fullUrl":"Specimen/1721763141048566000.96ee46fc-79c5-4169-92fd-c294f4803d58","resource":{"resourceType":"Specimen","id":"1721763141048566000.96ee46fc-79c5-4169-92fd-c294f4803d58","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"OBR"}]}},{"fullUrl":"Specimen/1721763141050856000.74417f3a-d059-4880-ab0d-b57d6d21fd98","resource":{"resourceType":"Specimen","id":"1721763141050856000.74417f3a-d059-4880-ab0d-b57d6d21fd98","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"SPM"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/filler-assigned-identifier","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/entity-identifier","valueString":"3003786103"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Component","valueString":"SPM.2.1"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PGN"}]},"value":"230011927"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/placer-assigned-identifier","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/entity-identifier","valueString":"230011927"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Component","valueString":"SPM.2.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FGN"}]},"value":"3003786103"}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"0912017","code":"119365002","display":"Specimen from wound"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","version":"Adobe_Code","code":"WND","display":"Wound"}],"text":"Wound"},"receivedTime":"2023-04-21T12:41:50Z","_receivedTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20230421124150"}]},"collection":{"collectedDateTime":"2023-03-22","_collectedDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20230322"}]},"bodySite":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"09012017","code":"56459004","display":"Foot"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","version":"Adobe_Code","code":"FOT","display":"Foot"}],"text":"Foot"}},"note":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"SPM.14"}],"text":"Isolate,"}]}},{"fullUrl":"ServiceRequest/1721763141061524000.5a8552d0-ff85-4fea-a974-6adf2f1055ee","resource":{"resourceType":"ServiceRequest","id":"1721763141061524000.5a8552d0-ff85-4fea-a974-6adf2f1055ee","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/business-event","valueCode":"RE"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/orc-common-order","extension":[{"url":"orc-21-ordering-facility-name","valueReference":{"reference":"Organization/1721763141057561000.90ff4489-a66a-4994-aeca-8b82a8504b0f"}},{"url":"orc-22-ordering-facility-address","valueAddress":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"601 Robert St. N."}]},{"url":"XAD.7","valueCode":"M"}]}],"type":"postal","line":[ "601 Robert St. N." ],"city":"St. Paul","state":"MN","postalCode":"55164-0899","country":"USA"}},{"url":"orc-24-ordering-provider-address","valueAddress":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"601 Robert St. N."}]},{"url":"XAD.7","valueCode":"M"}]}],"type":"postal","line":[ "601 Robert St. N." ],"city":"St. Paul","state":"MN","postalCode":"55164-0899","country":"USA"}},{"url":"orc-12-ordering-provider","valueReference":{"reference":"Practitioner/1721763141058933000.583cd3f0-a168-4242-842a-f8021e0ea87e"}}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request","extension":[{"url":"OBR.2","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"230011927"}},{"url":"OBR.3","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"40_3003786103_4988249_1087"}},{"url":"OBR.22","valueString":"202304271044-0400"},{"url":"OBR.25","valueId":"F"},{"url":"OBR.16","valueReference":{"reference":"Practitioner/1721763141060008000.3856a082-48db-484b-b538-3c464ceeab26"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"NET"},{"url":"XTN.3","valueString":"Internet"},{"url":"XTN.4","valueString":"Health.idlabreports@state.mn.us"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBR.17"}],"system":"email","value":"Health.idlabreports@state.mn.us"}}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"230011927"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.3"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"40_3003786103_4988249_1087"}],"status":"unknown","code":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/coding-system-oid","valueOid":"urn:oid:2.16.840.1.113883.6.1"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"PLT"}],"version":"2.69","code":"PLT1228","display":"Mold and Yeast XXX MS.MALDI-TOF"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"secondary-alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","code":"CDC-10179","display":"Fungal Identification"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","version":"v unknown","code":"1087","display":"MALDI-TOF-CLIA"}]},"subject":{"reference":"Patient/1721763140922533000.1a511090-a57c-4203-b893-d6c44bfcfe7a"},"requester":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"NET"},{"url":"XTN.3","valueString":"Internet"},{"url":"XTN.4","valueString":"Health.idlabreports@state.mn.us"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.14"}],"system":"email","value":"Health.idlabreports@state.mn.us"}}],"reference":"PractitionerRole/1721763141052302000.953cc078-735f-4781-8355-62f9b7a7f374"}}},{"fullUrl":"Practitioner/1721763141053654000.dc2d6e83-381d-41bc-aeb3-5ac6005cc6d2","resource":{"resourceType":"Practitioner","id":"1721763141053654000.dc2d6e83-381d-41bc-aeb3-5ac6005cc6d2","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.12"}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"XX"}]},"system":"STARLIMS.CDC.Stag","value":"SPHL-000034"}],"name":[{"family":"MN PHL Division, Minnesota Department of Health"}],"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"601 Robert St. N."}]},{"url":"XAD.7","valueCode":"M"}]}],"type":"postal","line":[ "601 Robert St. N." ],"city":"St. Paul","state":"MN","postalCode":"55164-0899","country":"USA"}]}},{"fullUrl":"Organization/1721763141055165000.4a1eed5a-8e1b-443f-8a79-733fbf21dc73","resource":{"resourceType":"Organization","id":"1721763141055165000.4a1eed5a-8e1b-443f-8a79-733fbf21dc73","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/organization-name-type","valueCoding":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueCodeableConcept":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"XON.2"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"D"}]}}],"code":"D"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"SPHL-000034"}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/code-index-name","valueString":"identifier"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"XX"}]},"value":"SPHL-000034"}],"name":"MN PHL Division, Minnesota Department of Health","telecom":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.2","valueString":"WPN"},{"url":"XTN.3","valueString":"Internet"},{"url":"XTN.4","valueString":"Health.idlabreports@state.mn.us"}]}],"system":"email","value":"Health.idlabreports@state.mn.us","use":"work"}],"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/sad-address-line","extension":[{"url":"SAD.1","valueString":"601 Robert St. N."}]},{"url":"XAD.7","valueCode":"M"}]}],"type":"postal","line":[ "601 Robert St. N." ],"city":"St. Paul","state":"MN","postalCode":"55164-0899","country":"USA"}]}},{"fullUrl":"PractitionerRole/1721763141052302000.953cc078-735f-4781-8355-62f9b7a7f374","resource":{"resourceType":"PractitionerRole","id":"1721763141052302000.953cc078-735f-4781-8355-62f9b7a7f374","practitioner":{"reference":"Practitioner/1721763141053654000.dc2d6e83-381d-41bc-aeb3-5ac6005cc6d2"},"organization":{"reference":"Organization/1721763141055165000.4a1eed5a-8e1b-443f-8a79-733fbf21dc73"}}},{"fullUrl":"Organization/1721763141057561000.90ff4489-a66a-4994-aeca-8b82a8504b0f","resource":{"resourceType":"Organization","id":"1721763141057561000.90ff4489-a66a-4994-aeca-8b82a8504b0f","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/organization-name-type","valueCoding":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueCodeableConcept":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"XON.2"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"D"}]}}],"code":"D"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"SPHL-000034"}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/code-index-name","valueString":"identifier"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"XX"}]},"value":"SPHL-000034"}],"name":"MN PHL Division, Minnesota Department of Health"}},{"fullUrl":"Practitioner/1721763141058933000.583cd3f0-a168-4242-842a-f8021e0ea87e","resource":{"resourceType":"Practitioner","id":"1721763141058933000.583cd3f0-a168-4242-842a-f8021e0ea87e","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"XX"}]},"system":"STARLIMS.CDC.Stag","value":"SPHL-000034"}],"name":[{"family":"MN PHL Division, Minnesota Department of Health"}]}},{"fullUrl":"Practitioner/1721763141060008000.3856a082-48db-484b-b538-3c464ceeab26","resource":{"resourceType":"Practitioner","id":"1721763141060008000.3856a082-48db-484b-b538-3c464ceeab26","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"identifier":[{"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/codeable-concept-id","valueBoolean":true}],"code":"XX"}]},"system":"STARLIMS.CDC.Stag","value":"SPHL-000034"}],"name":[{"family":"MN PHL Division, Minnesota Department of Health"}]}},{"fullUrl":"DiagnosticReport/1721763141066965000.b397146d-3575-49d0-99bd-8bad7ccb6c13","resource":{"resourceType":"DiagnosticReport","id":"1721763141066965000.b397146d-3575-49d0-99bd-8bad7ccb6c13","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"SPHL-000034"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.1.3661"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"PLAC"}]},"value":"230011927"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"STARLIMS.CDC.Stag"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.3.2.1.2"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]},"value":"40_3003786103_4988249_1087"}],"basedOn":[{"reference":"ServiceRequest/1721763141061524000.5a8552d0-ff85-4fea-a974-6adf2f1055ee"}],"status":"final","code":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/coding-system-oid","valueOid":"urn:oid:2.16.840.1.113883.6.1"}],"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"PLT"}],"version":"2.69","code":"PLT1228","display":"Mold and Yeast XXX MS.MALDI-TOF"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"secondary-alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","code":"CDC-10179","display":"Fungal Identification"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"alt-coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"L"}],"system":"https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL","version":"v unknown","code":"1087","display":"MALDI-TOF-CLIA"}]},"subject":{"reference":"Patient/1721763140922533000.1a511090-a57c-4203-b893-d6c44bfcfe7a"},"effectiveDateTime":"2023-03-22","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20230322"}]},"issued":"2023-04-27T10:44:00-04:00","_issued":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"202304271044-0400"}]},"specimen":[{"reference":"Specimen/1721763141050856000.74417f3a-d059-4880-ab0d-b57d6d21fd98"},{"reference":"Specimen/1721763141048566000.96ee46fc-79c5-4169-92fd-c294f4803d58"}],"result":[{"reference":"Observation/1721763140929139000.f59fff56-c4c8-4b35-9014-cf139baf91de"}]}}]}"""

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
    """{"resourceType":"Bundle","id":"1721769506350551000.f5e13c92-11ab-48c9-b738-d7d178d427fa","meta":{"lastUpdated":"2024-07-23T14:18:26.355-07:00"},"identifier":{"system":"https://reportstream.cdc.gov/prime-router","value":"20240403205305_dba7572cc6334f1ea0744c5f235c823e"},"type":"message","timestamp":"2024-04-03T13:53:05.000-07:00","entry":[{"fullUrl":"MessageHeader/df373c48-bfb2-36b0-b63c-5be13bc5d051","resource":{"resourceType":"MessageHeader","id":"df373c48-bfb2-36b0-b63c-5be13bc5d051","meta":{"tag":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0103","code":"P"}]},"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/msh-message-header","extension":[{"url":"MSH.7","valueString":"20240403205305+0000"},{"url":"MSH.15","valueString":"NE"},{"url":"MSH.16","valueString":"NE"},{"url":"MSH.21","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"ELR251R1_Rcvr_Prof"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.9.11"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"value":"PHLabReport-NoAck"}}]}],"eventCoding":{"system":"http://terminology.hl7.org/CodeSystem/v2-0003","code":"R01","display":"ORU^R01^ORU_R01"},"destination":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.114222.4.3.15.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.5"}],"name":"AIMS.INTEGRATION.PRD","endpoint":"urn:oid:2.16.840.1.114222.4.3.15.1","receiver":{"reference":"Organization/1721769506400224000.16f63de3-b784-4e2a-9e9a-6aeedb730930"}}],"sender":{"reference":"Organization/1721769506382358000.727e56bb-e199-4146-91f5-895c7f2c763b"},"source":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.3"}],"software":"MMTC.PROD","version":"2022","endpoint":"urn:oid:2.16.840.1.113883.3.8589.4.2.106.1"}}},{"fullUrl":"Organization/1721769506382358000.727e56bb-e199-4146-91f5-895c7f2c763b","resource":{"resourceType":"Organization","id":"1721769506382358000.727e56bb-e199-4146-91f5-895c7f2c763b","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"CAREEVOLUTION"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"00Z0000024"}]}},{"fullUrl":"Organization/1721769506400224000.16f63de3-b784-4e2a-9e9a-6aeedb730930","resource":{"resourceType":"Organization","id":"1721769506400224000.16f63de3-b784-4e2a-9e9a-6aeedb730930","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"MSH.6"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"AIMS.PLATFORM"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"ISO"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.217446"}]}},{"fullUrl":"Provenance/1721769506626284000.b224960f-99de-4964-8902-a96d22cf99d6","resource":{"resourceType":"Provenance","id":"1721769506626284000.b224960f-99de-4964-8902-a96d22cf99d6","target":[{"reference":"MessageHeader/df373c48-bfb2-36b0-b63c-5be13bc5d051"},{"reference":"DiagnosticReport/1721769506786531000.aa4767a7-d342-451b-86fb-67dacf59832b"}],"recorded":"2024-04-03T20:53:05Z","activity":{"coding":[{"display":"ORU^R01^ORU_R01"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"author"}]},"who":{"reference":"Organization/1721769506625647000.05680873-3c11-4cc9-b3fa-2d964d938cc5"}}],"entity":[{"role":"source","what":{"reference":"Device/1721769506629014000.f25c32a6-91f6-494a-888e-0fbbec3d48f0"}}]}},{"fullUrl":"Organization/1721769506625647000.05680873-3c11-4cc9-b3fa-2d964d938cc5","resource":{"resourceType":"Organization","id":"1721769506625647000.05680873-3c11-4cc9-b3fa-2d964d938cc5","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.1"}],"value":"CAREEVOLUTION"},{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"HD.2,HD.3"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301","code":"CLIA"}]},"value":"00Z0000024"}]}},{"fullUrl":"Organization/1721769506628860000.d9cabbe6-dbba-47f7-833c-085495d84bd1","resource":{"resourceType":"Organization","id":"1721769506628860000.d9cabbe6-dbba-47f7-833c-085495d84bd1","name":"CAREEVOLUTION"}},{"fullUrl":"Device/1721769506629014000.f25c32a6-91f6-494a-888e-0fbbec3d48f0","resource":{"resourceType":"Device","id":"1721769506629014000.f25c32a6-91f6-494a-888e-0fbbec3d48f0","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org","valueReference":{"reference":"Organization/1721769506628860000.d9cabbe6-dbba-47f7-833c-085495d84bd1"}}],"manufacturer":"CAREEVOLUTION","deviceName":[{"name":"MMTC.PROD","type":"manufacturer-name"}],"modelNumber":"16498","version":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/software-install-date","valueDateTime":"2024-04-02","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240402"}]}}],"value":"2022"}]}},{"fullUrl":"Provenance/1721769506634006000.56f3c2a2-e6be-4056-8570-832bba792588","resource":{"resourceType":"Provenance","id":"1721769506634006000.56f3c2a2-e6be-4056-8570-832bba792588","recorded":"2024-07-23T14:18:26Z","policy":["http://hl7.org/fhir/uv/v2mappings/message-oru-r01-to-bundle"],"activity":{"coding":[{"code":"v2-FHIR transformation"}]},"agent":[{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/provenance-participant-type","code":"assembler"}]},"who":{"reference":"Organization/1721769506633760000.15828fa9-34b2-4811-9ca7-d9ac1d0c6169"}}]}},{"fullUrl":"Organization/1721769506633760000.15828fa9-34b2-4811-9ca7-d9ac1d0c6169","resource":{"resourceType":"Organization","id":"1721769506633760000.15828fa9-34b2-4811-9ca7-d9ac1d0c6169","identifier":[{"value":"CDC PRIME - Atlanta"},{"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0301"}]},"system":"urn:ietf:rfc:3986","value":"2.16.840.1.114222.4.1.237821"}]}},{"fullUrl":"Patient/1721769506648030000.cb04af02-0996-4ffe-b9d6-8a74434dda05","resource":{"resourceType":"Patient","id":"1721769506648030000.cb04af02-0996-4ffe-b9d6-8a74434dda05","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cx-identifier","extension":[{"url":"CX.5","valueString":"PI"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"PID.3"}],"type":{"coding":[{"code":"PI"}]},"system":"MMTC.PROD","_system":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"}]},"value":"8be6fa3710374dcebe0174e0fd5a1a7c"}],"name":[{},{}],"telecom":[{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-area","valueString":"111"},{"url":"http://hl7.org/fhir/StructureDefinition/contactpoint-local","valueString":"1111111"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xtn-contact-point","extension":[{"url":"XTN.7","valueString":"1111111"}]}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]},"use":"home"}],"address":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xad-address"}],"postalCode":"02139","country":"USA"}]}},{"fullUrl":"Provenance/1721769506649265000.f33813bd-9831-4a6f-bb78-eb102521ae53","resource":{"resourceType":"Provenance","id":"1721769506649265000.f33813bd-9831-4a6f-bb78-eb102521ae53","target":[{"reference":"Patient/1721769506648030000.cb04af02-0996-4ffe-b9d6-8a74434dda05"}],"recorded":"2024-07-23T14:18:26Z","activity":{"coding":[{"system":"https://terminology.hl7.org/CodeSystem/v3-DataOperation","code":"UPDATE"}]}}},{"fullUrl":"Observation/1721769506651731000.9763163b-36c6-43e6-b5be-2e94f543963b","resource":{"resourceType":"Observation","id":"1721769506651731000.9763163b-36c6-43e6-b5be-2e94f543963b","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/analysis-date-time","valueDateTime":"2024-04-03T12:00:00-04:00","_valueDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240403120000-0400"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"CWE"},{"url":"OBX.11","valueString":"F"},{"url":"OBX.17","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"99ELR"}],"version":"Vunknown","code":"BinaxNOW COVID-19 Antigen Self Test_Abbott Diagnostics Scarborough, Inc._EUA"}]}}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","version":"2.71","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1721769506648030000.cb04af02-0996-4ffe-b9d6-8a74434dda05"},"performer":[{"reference":"Organization/1721769506652103000.1c90c701-3c64-4082-8d2e-3011ec1f509d"},{"reference":"Organization/1721769506652721000.30796b1c-7f2b-4f80-8d7d-cfa961343d8b"}],"valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"20200901","code":"260373001","display":"Detected"}]},"note":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/nte-annotation","extension":[{"url":"NTE.2","valueId":"L"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/note-comment","valueId":"Note"}],"text":"Note"}],"method":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"99ELR"}],"version":"Vunknown","code":"BinaxNOW COVID-19 Antigen Self Test_Abbott Diagnostics Scarborough, Inc._EUA"}]}}},{"fullUrl":"Organization/1721769506652103000.1c90c701-3c64-4082-8d2e-3011ec1f509d","resource":{"resourceType":"Organization","id":"1721769506652103000.1c90c701-3c64-4082-8d2e-3011ec1f509d","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-organization","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"00Z0000042"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.15"}],"identifier":[{"value":"00Z0000042"}]}},{"fullUrl":"Organization/1721769506652721000.30796b1c-7f2b-4f80-8d7d-cfa961343d8b","resource":{"resourceType":"Organization","id":"1721769506652721000.30796b1c-7f2b-4f80-8d7d-cfa961343d8b","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"00Z0000042"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.25"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.1.152"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/code-index-name","valueString":"identifier"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"XX"}]},"value":"00Z0000042"}],"name":"SA.OTCSelfReport"}},{"fullUrl":"Observation/1721769506656008000.96aaf005-6ef1-4980-a985-b34ed7a3e360","resource":{"resourceType":"Observation","id":"1721769506656008000.96aaf005-6ef1-4980-a985-b34ed7a3e360","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation","extension":[{"url":"OBX.2","valueId":"NM"},{"url":"OBX.6","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"UCUM"}],"system":"http://unitsofmeasure.org","version":"2.1","code":"a","display":"year"}]}},{"url":"OBX.29","valueId":"QST"},{"url":"OBX.11","valueString":"F"}]}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","version":"2.71","code":"35659-2","display":"Age at specimen collection"}]},"subject":{"reference":"Patient/1721769506648030000.cb04af02-0996-4ffe-b9d6-8a74434dda05"},"performer":[{"reference":"Organization/1721769506656407000.c836f547-598f-4be3-9b43-f5d3b07f5db0"},{"reference":"Organization/1721769506657132000.18025840-68d3-4103-835c-ba8888d25775"}],"valueQuantity":{"value" : 24,"unit":"year","system":"UCUM","code":"a"}}},{"fullUrl":"Organization/1721769506656407000.c836f547-598f-4be3-9b43-f5d3b07f5db0","resource":{"resourceType":"Organization","id":"1721769506656407000.c836f547-598f-4be3-9b43-f5d3b07f5db0","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-organization","valueCodeableConcept":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"}],"code":"00Z0000042"}]}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.15"}],"identifier":[{"value":"00Z0000042"}]}},{"fullUrl":"Organization/1721769506657132000.18025840-68d3-4103-835c-ba8888d25775","resource":{"resourceType":"Organization","id":"1721769506657132000.18025840-68d3-4103-835c-ba8888d25775","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/xon-organization","extension":[{"url":"XON.10","valueString":"00Z0000042"}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBX.25"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.1.152"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/code-index-name","valueString":"identifier"}],"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"XX"}]},"value":"00Z0000042"}],"name":"SA.OTCSelfReport"}},{"fullUrl":"Specimen/1721769506775693000.9e155cea-d324-416f-afb3-b9841790001a","resource":{"resourceType":"Specimen","id":"1721769506775693000.9e155cea-d324-416f-afb3-b9841790001a","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"OBR"}]}},{"fullUrl":"Specimen/1721769506777454000.8cb9df8d-4841-4582-89b1-0e452c513241","resource":{"resourceType":"Specimen","id":"1721769506777454000.8cb9df8d-4841-4582-89b1-0e452c513241","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Segment","valueString":"SPM"}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueString":"ISO"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Component","valueString":"SPM.2.2"}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FGN"}]},"value":"dba7572cc6334f1ea0744c5f235c823e"}],"type":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"SCT"}],"system":"http://snomed.info/sct","version":"20200901","code":"697989009","display":"Anterior nares swab"}]},"receivedTime":"2024-04-03T12:00:00-04:00","_receivedTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240403120000-0400"}]},"collection":{"collectedDateTime":"2024-04-03T12:00:00-04:00","_collectedDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240403120000-0400"}]}}}},{"fullUrl":"ServiceRequest/1721769506784011000.2c66df4d-e96a-4d61-b335-24c7bef713d2","resource":{"resourceType":"ServiceRequest","id":"1721769506784011000.2c66df4d-e96a-4d61-b335-24c7bef713d2","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/business-event","valueCode":"RE"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/orc-common-order","extension":[{"url":"orc-21-ordering-facility-name","valueReference":{"reference":"Organization/1721769506781657000.a1637f91-b0b7-498b-97b6-e4d9d4feb444"}},{"url":"orc-22-ordering-facility-address","valueAddress":{"postalCode":"02139"}},{"url":"orc-12-ordering-provider","valueReference":{"reference":"Practitioner/1721769506782327000.66233128-a923-49da-b3f4-93160bb017d1"}}]},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request","extension":[{"url":"OBR.3","valueIdentifier":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}]}},{"url":"OBR.22","valueString":"20240403120000-0400"},{"url":"OBR.25","valueId":"F"},{"url":"OBR.16","valueReference":{"reference":"Practitioner/1721769506783028000.bf53ac16-f9dd-407b-ae66-34b50e98f758"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"OBR.17"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]}}}]}],"identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.3"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]}}],"status":"unknown","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","version":"2.71","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1721769506648030000.cb04af02-0996-4ffe-b9d6-8a74434dda05"},"requester":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/callback-number","valueContactPoint":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.14"}],"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]}}}],"reference":"PractitionerRole/1721769506778465000.aa41010e-e480-493b-a23b-7719343f60b2"}}},{"fullUrl":"Practitioner/1721769506779187000.f34d063d-a212-46c6-ac07-0390c4571768","resource":{"resourceType":"Practitioner","id":"1721769506779187000.f34d063d-a212-46c6-ac07-0390c4571768","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2Field","valueString":"ORC.12"}]}},{"fullUrl":"Organization/1721769506780095000.cc04f4da-638f-464f-94eb-e2902b1e9570","resource":{"resourceType":"Organization","id":"1721769506780095000.cc04f4da-638f-464f-94eb-e2902b1e9570","name":"SA.OTCSelfReport","telecom":[{"_system":{"extension":[{"url":"http://hl7.org/fhir/StructureDefinition/data-absent-reason","valueCode":"unknown"}]}}],"address":[{"postalCode":"02139"}]}},{"fullUrl":"PractitionerRole/1721769506778465000.aa41010e-e480-493b-a23b-7719343f60b2","resource":{"resourceType":"PractitionerRole","id":"1721769506778465000.aa41010e-e480-493b-a23b-7719343f60b2","practitioner":{"reference":"Practitioner/1721769506779187000.f34d063d-a212-46c6-ac07-0390c4571768"},"organization":{"reference":"Organization/1721769506780095000.cc04f4da-638f-464f-94eb-e2902b1e9570"}}},{"fullUrl":"Organization/1721769506781657000.a1637f91-b0b7-498b-97b6-e4d9d4feb444","resource":{"resourceType":"Organization","id":"1721769506781657000.a1637f91-b0b7-498b-97b6-e4d9d4feb444","name":"SA.OTCSelfReport"}},{"fullUrl":"Practitioner/1721769506782327000.66233128-a923-49da-b3f4-93160bb017d1","resource":{"resourceType":"Practitioner","id":"1721769506782327000.66233128-a923-49da-b3f4-93160bb017d1"}},{"fullUrl":"Practitioner/1721769506783028000.bf53ac16-f9dd-407b-ae66-34b50e98f758","resource":{"resourceType":"Practitioner","id":"1721769506783028000.bf53ac16-f9dd-407b-ae66-34b50e98f758"}},{"fullUrl":"DiagnosticReport/1721769506786531000.aa4767a7-d342-451b-86fb-67dacf59832b","resource":{"resourceType":"DiagnosticReport","id":"1721769506786531000.aa4767a7-d342-451b-86fb-67dacf59832b","identifier":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority","extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id","valueString":"MMTC.PROD"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id","valueString":"2.16.840.1.113883.3.8589.4.2.106.1"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id-type","valueCode":"ISO"}]}],"type":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0203","code":"FILL"}]}}],"basedOn":[{"reference":"ServiceRequest/1721769506784011000.2c66df4d-e96a-4d61-b335-24c7bef713d2"}],"status":"final","code":{"coding":[{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding","valueString":"coding"},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/cwe-coding-system","valueString":"LN"}],"system":"http://loinc.org","version":"2.71","code":"94558-4","display":"SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"}]},"subject":{"reference":"Patient/1721769506648030000.cb04af02-0996-4ffe-b9d6-8a74434dda05"},"effectiveDateTime":"2024-04-03T12:00:00-04:00","_effectiveDateTime":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240403120000-0400"}]},"issued":"2024-04-03T12:00:00-04:00","_issued":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/hl7v2-date-time","valueString":"20240403120000-0400"}]},"specimen":[{"reference":"Specimen/1721769506777454000.8cb9df8d-4841-4582-89b1-0e452c513241"},{"reference":"Specimen/1721769506775693000.9e155cea-d324-416f-afb3-b9841790001a"}],"result":[{"reference":"Observation/1721769506651731000.9763163b-36c6-43e6-b5be-2e94f543963b"},{"reference":"Observation/1721769506656008000.96aaf005-6ef1-4980-a985-b34ed7a3e360"}]}}]}"""

@Suppress("ktlint:standard:max-line-length")
private const val invalidRadxMarsHL7Message =
    """MSH|^~\&|MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO|CAREEVOLUTION^00Z0000024^CLIA|AIMS.INTEGRATION.PRD^2.16.840.1.114222.4.3.15.1^ISO|AIMS.PLATFORM^2.16.840.1.114222.4.1.217446^ISO|20240403205305+0000||ORU^R01^ORU_R01|20240403205305_dba7572cc6334f1ea0744c5f235c823e|P|2.5.1|||NE|NE|||||PHLabReport-NoAck^ELR251R1_Rcvr_Prof^2.16.840.1.113883.9.12^ISO
SFT|CAREEVOLUTION|2022|MMTC.PROD|16498||20240402
PID|1||8be6fa3710374dcebe0174e0fd5a1a7c^^^MMTC.PROD&2.16.840.1.113883.3.8589.4.2.106.1&ISO^PI||^^^^^^~^^^^^^||||||^^^^02139^USA||^^^^^111^1111111
ORC|RE||^MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO|||||||||^^||^^^^^^|||||||SA.OTCSelfReport|^^^^02139^^^^|^^^^^^
OBR|1||^MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.71|||20240403120000|||||||||^^|^^^^^^|||||20240403120000|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.71||260373001^Detected^SCT^^^^20200901||||||F||||00Z0000042||BinaxNOW COVID-19 Antigen Self Test_Abbott Diagnostics Scarborough, Inc._EUA^^99ELR^^^^Vunknown||20240403120000||||SA.OTCSelfReport^^^^^&2.16.840.1.113883.3.8589.4.1.152&ISO^XX^^^00Z0000042|
NTE|1|L|Note
OBX|2|NM|35659-2^Age at specimen collection^LN^^^^2.71||24|a^year^UCUM^^^^2.1|||||F||||00Z0000042||||||||SA.OTCSelfReport^^^^^&2.16.840.1.113883.3.8589.4.1.152&ISO^XX^^^00Z0000042||||||QST
SPM|1|^dba7572cc6334f1ea0744c5f235c823e&MMTC.PROD&2.16.840.1.113883.3.8589.4.2.106.1&ISO||697989009^Anterior nares swab^SCT^^^^20200901|||||||||||||20240403120000-0400|20240403120000-0400"""

// TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FhirFunctionIntegrationTests {

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
                format = MimeFormat.HL7,
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
        fileFormat: MimeFormat,
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
        unmockkAll()
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    @Test
    fun `test does not update the DB or send messages on an error`() {
        val report = seedTask(
            MimeFormat.HL7,
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
            val nextTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.destination_filter))
                .fetchOneInto(Task.TASK)
            assertThat(nextTask).isNull()
            val convertReportFile =
                DSL.using(txn).select(RF.REPORT_FILE.asterisk())
                    .from(RF.REPORT_FILE)
                    .where(RF.REPORT_FILE.NEXT_ACTION.eq(TaskAction.destination_filter))
                    .fetchOneInto(RF.REPORT_FILE)
            assertThat(convertReportFile).isNull()
        }
        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }
    }

    @Test
    fun `test successfully processes a convert message for HL7`() {
        val report = seedTask(
            MimeFormat.HL7,
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
        } returns BlobAccess.BlobInfo(MimeFormat.FHIR, "", "".toByteArray())
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
            val nextTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.destination_filter))
                .fetchOneInto(Task.TASK)
            assertThat(nextTask).isNotNull()
            val convertReportFile =
                DSL.using(txn).select(RF.REPORT_FILE.asterisk())
                    .from(RF.REPORT_FILE)
                    .where(RF.REPORT_FILE.NEXT_ACTION.eq(TaskAction.destination_filter))
                    .fetchOneInto(RF.REPORT_FILE)
            assertThat(convertReportFile).isNotNull()
        }
        verify(exactly = 1) {
            QueueAccess.sendMessage(elrDestinationFilterQueueName, any())
            BlobAccess.uploadBody(MimeFormat.FHIR, any(), any(), any(), any())
        }
    }

    @Test
    fun `test successfully processes a convert message for bulk HL7 message`() {
        val validBatch = cleanHL7Record + "\n" + invalidHL7Record
        val report = seedTask(
            MimeFormat.HL7,
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
        } answers { BlobAccess.BlobInfo(MimeFormat.FHIR, UUID.randomUUID().toString(), "".toByteArray()) }
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
            val nextTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.destination_filter))
                .fetchInto(Task.TASK)
            assertThat(nextTask).hasSize(2)
            val convertReportFile =
                DSL.using(txn).select(RF.REPORT_FILE.asterisk())
                    .from(RF.REPORT_FILE)
                    .where(RF.REPORT_FILE.NEXT_ACTION.eq(TaskAction.destination_filter))
                    .fetchInto(RF.REPORT_FILE)
            assertThat(convertReportFile).hasSize(2)
        }
        verify(exactly = 2) {
            QueueAccess.sendMessage(elrDestinationFilterQueueName, any())
        }
        verify(exactly = 1) {
            BlobAccess.uploadBody(
                MimeFormat.FHIR,
                match { bytes ->
                    val result = CompareData().compare(
                        cleanHL7RecordConverted.byteInputStream(),
                        bytes.inputStream(),
                        MimeFormat.FHIR,
                        null
                    )
                    result.passed
                },
                any(), any(), any()
            )
            BlobAccess.uploadBody(
                MimeFormat.FHIR,
                match { bytes ->
                    val result = CompareData().compare(
                        invalidHL7RecordConverted.byteInputStream(),
                        bytes.inputStream(),
                        MimeFormat.FHIR,
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
            MimeFormat.HL7,
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
        } answers { BlobAccess.BlobInfo(MimeFormat.FHIR, UUID.randomUUID().toString(), "".toByteArray()) }
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
            val nextTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.destination_filter))
                .fetchInto(Task.TASK)
            assertThat(nextTask).hasSize(2)
            val convertReportFile =
                DSL.using(txn).select(RF.REPORT_FILE.asterisk())
                    .from(RF.REPORT_FILE)
                    .where(RF.REPORT_FILE.NEXT_ACTION.eq(TaskAction.destination_filter))
                    .fetchInto(RF.REPORT_FILE)
            assertThat(convertReportFile).hasSize(2)
            assertThat(actionLogger.errors).hasSize(2)
        }
        verify(exactly = 2) {
            QueueAccess.sendMessage(elrDestinationFilterQueueName, any())
        }

        verify(exactly = 1) {
            BlobAccess.uploadBody(
                MimeFormat.FHIR,
                match { bytes ->
                    val result = CompareData().compare(
                        cleanHL7RecordConverted.byteInputStream(),
                        bytes.inputStream(),
                        MimeFormat.FHIR,
                        null
                    )
                    result.passed
                },
                any(), any(), any()
            )
            BlobAccess.uploadBody(
                MimeFormat.FHIR,
                match { bytes ->
                    val result = CompareData().compare(
                        invalidHL7RecordConverted.byteInputStream(),
                        bytes.inputStream(),
                        MimeFormat.FHIR,
                        null
                    )
                    result.passed
                },
                any(), any(), any()
            )
        }
    }

    // TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
    @Test
    fun `test successfully processes a route message`() {
        val report = seedTask(
            MimeFormat.HL7,
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
        val mockReport = mockk<ReportFile>()
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
        } returns BlobAccess.BlobInfo(MimeFormat.FHIR, "", "".toByteArray())
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
        every { mockReport.reportId } returns UUID.randomUUID()
        mockkConstructor(ReportService::class)
        every { anyConstructed<ReportService>().getSenderName(any()) } returns "senderOrg.senderOrgClient"
        every { anyConstructed<ReportService>().getRootReport(any()) } returns mockReport

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = FHIRRouter(
            UnitTestUtils.simpleMetadata,
            settings,
            ReportStreamTestDatabaseContainer.testDatabaseAccess
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
            val nextTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.translate))
                .fetchOneInto(Task.TASK)
            assertThat(nextTask).isNotNull()
            val convertReportFile =
                DSL.using(txn).select(RF.REPORT_FILE.asterisk())
                    .from(RF.REPORT_FILE)
                    .where(
                        RF.REPORT_FILE.NEXT_ACTION
                            .eq(TaskAction.translate)
                    )
                    .fetchOneInto(RF.REPORT_FILE)
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
            MimeFormat.FHIR,
            TaskAction.translate,
            TaskAction.send,
            Event.EventAction.SEND,
            Topic.ELR_ELIMS,
            100,
            oneOrganization
        )
        val receiverFilteredReport = seedTask(
            MimeFormat.FHIR,
            TaskAction.receiver_filter,
            TaskAction.translate,
            Event.EventAction.TRANSLATE,
            Topic.ELR_ELIMS,
            99,
            oneOrganization,
            translateReport
        )
        val destinationFilteredReport = seedTask(
            MimeFormat.FHIR,
            TaskAction.destination_filter,
            TaskAction.receiver_filter,
            Event.EventAction.RECEIVER_FILTER,
            Topic.ELR_ELIMS,
            98,
            oneOrganization,
            receiverFilteredReport
        )
        val convertReport = seedTask(
            MimeFormat.FHIR,
            TaskAction.convert,
            TaskAction.destination_filter,
            Event.EventAction.DESTINATION_FILTER,
            Topic.ELR_ELIMS,
            97,
            oneOrganization,
            destinationFilteredReport
        )
        val receiveReport = seedTask(
            MimeFormat.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.ELR_ELIMS,
            96,
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
                DSL.using(txn).select(RF.REPORT_FILE.asterisk())
                    .from(RF.REPORT_FILE)
                    .where(
                        RF.REPORT_FILE.REPORT_ID
                            .eq(queueTask!!.reportId)
                    )
                    .fetchOneInto(RF.REPORT_FILE)
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
            MimeFormat.FHIR,
            TaskAction.translate,
            TaskAction.send,
            Event.EventAction.SEND,
            Topic.ELR_ELIMS,
            100,
            oneOrganization
        )
        val receiverFilteredReport = seedTask(
            MimeFormat.FHIR,
            TaskAction.receiver_filter,
            TaskAction.translate,
            Event.EventAction.TRANSLATE,
            Topic.ELR_ELIMS,
            99,
            oneOrganization,
            translateReport
        )
        val destinationFilteredReport = seedTask(
            MimeFormat.FHIR,
            TaskAction.destination_filter,
            TaskAction.receiver_filter,
            Event.EventAction.RECEIVER_FILTER,
            Topic.ELR_ELIMS,
            98,
            oneOrganization,
            receiverFilteredReport
        )
        val convertReport = seedTask(
            MimeFormat.FHIR,
            TaskAction.convert,
            TaskAction.route,
            Event.EventAction.ROUTE,
            Topic.ELR_ELIMS,
            97,
            oneOrganization,
            destinationFilteredReport
        )
        val receiveReport = seedTask(
            MimeFormat.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.ELR_ELIMS,
            96,
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
                DSL.using(txn).select(RF.REPORT_FILE.asterisk())
                    .from(RF.REPORT_FILE)
                    .where(
                        RF.REPORT_FILE.REPORT_ID
                            .eq(sendTask!!.reportId)
                    )
                    .fetchOneInto(RF.REPORT_FILE)
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
            MimeFormat.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)
        val fhirRecordBytes = fhirRecord.toByteArray()

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
        } returns BlobAccess.BlobInfo(MimeFormat.FHIR, "", "".toByteArray())
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
            MimeFormat.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            oneOrganization
        )
        val metadata = Metadata(UnitTestUtils.simpleSchema)
        val fhirRecordBytes = codelessFhirRecord.toByteArray()

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
        } returns BlobAccess.BlobInfo(MimeFormat.FHIR, "", "".toByteArray())
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