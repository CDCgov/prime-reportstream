package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Property
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.Date
import java.util.stream.Stream
import kotlin.streams.toList
import kotlin.test.Test

class FhirTranscoderTests {
    @Test
    fun `test get message engine`() {
        // Just make sure we can get an engine, which means the config is setup correctly.
        assertThat(FhirTranscoder.getMessageEngine()).isNotNull()
    }

    @Test
    fun `test default parser encode FHIR bundle`() {
        fun isValidJSONObject(jsonVal: String): Boolean {
            try {
                JSONObject(jsonVal)
            } catch (ex: JSONException) {
                return false
            }
            return true
        }
        // Create test FHIR Bundle
        val testBundle = Bundle()
        testBundle.type = Bundle.BundleType.MESSAGE
        testBundle.id = "someid"
        testBundle.timestamp = Date()
        testBundle.addEntry()

        val encodedBundle = FhirTranscoder.encode(testBundle)

        assertThat(encodedBundle).isNotEmpty()
        assertThat(isValidJSONObject(encodedBundle)).isTrue()

        // Ensure contains id and type
        val jsonObj = JSONObject(encodedBundle)
        assertThat(jsonObj.get("id")).equals(testBundle.id)
        assertThat(jsonObj.get("type")).equals(testBundle.type.name.lowercase())
    }

    @Test
    fun `test default parser decode FHIR bundle`() {
        val encodedBundle = """
            {"resourceType":"Bundle","id":"someid","type":"message","timestamp":"2022-06-14T12:11:50.281-04:00"}
        """.trimIndent()
        val decodedBundle = FhirTranscoder.decode(encodedBundle)
        assertThat(decodedBundle).isNotNull()
        assertThat(decodedBundle.id).equals("someid")
        assertThat(decodedBundle.type).equals(Bundle.BundleType.MESSAGE)
    }

    @Test
    fun `test encoding and decoding combination`() {
        // Create test FHIR Bundle
        val testBundle = Bundle()
        testBundle.type = Bundle.BundleType.MESSAGE
        testBundle.id = "someid"
        testBundle.timestamp = Date()
        testBundle.addEntry()

        val encodedBundle = FhirTranscoder.encode(testBundle)
        val decodedBundle = FhirTranscoder.decode(encodedBundle)
        // Ensure the decoded output is the same as the input pre-encode
        assertThat(decodedBundle).isNotNull()
        assertThat(decodedBundle).equals(testBundle)
    }

    @Test
    fun `test decoding of bad FHIR messages`() {
        val actionLogger = ActionLogger()

        // Empty data
        val badData1 = ""
        FhirTranscoder.getBundles(badData1, actionLogger)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()

        // Empty bundle
        val emptyBundle = """
            {"resourceType":"Bundle"}
        """.trimIndent()
        FhirTranscoder.getBundles(emptyBundle, actionLogger)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()

        // Some CSV was sent
        val badData2 = """
            a,b,c
            1,2,3
        """.trimIndent()
        FhirTranscoder.getBundles(badData2, actionLogger)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()

        // Some truncated HL7
        val badData3 = "MSH|^~\\&#|MEDITECH^2.16.840.1.114222.4.3.2.2.1.321.111^ISO|COCAA^1.2."
        FhirTranscoder.getBundles(badData3, actionLogger)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()
    }

    @Test
    fun `test decoding of good FHIR messages`() {
        val actionLogger = ActionLogger()

        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        var messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(actionLogger.hasErrors()).isFalse()
        assertThat(messages.size).isEqualTo(1)
        actionLogger.logs.clear()

        val bulkFhirData = File("src/test/resources/fhirengine/engine/bulk_valid_data.fhir").readText()
        messages = FhirTranscoder.getBundles(bulkFhirData, actionLogger)
        assertThat(actionLogger.hasErrors()).isFalse()
        assertThat(messages.size).isEqualTo(2)
        actionLogger.logs.clear()
    }

    fun getReferences(resource: Base): List<String> {
        return filterPropertyList(resource.children().stream().flatMap { getValues(it) }.toList())
    }

    fun filterPropertyList(properties: List<Property>): List<String> {
        return properties.filter { it.hasValues() }.flatMap { it.values }
            .filterIsInstance<Reference>().map { it.reference }
    }

    fun getValues(property: Property): Stream<Property> {
        return Stream.concat(
            Stream.of(property),
            property.values.flatMap { it.children() }.stream().flatMap { getValues(it) }
        )
    }

    fun deleteResource(resource: Base, bundle: Bundle) {
        // Get the resource children references
        val children = getReferences(resource)
        // get all resources except the resource being removed
        val resources = bundle.entry.filter { it.resource.id != resource.idBase }
        // get all references for every resource
        val references = filterPropertyList(
            resources.map { it.resource }.flatMap { it.children() }.stream().flatMap { getValues(it) }.toList()
        )

        // remove orphaned children
        children.forEach { child ->
            if (!references.contains(child)) {
                bundle.entry.removeIf { it.resource.id == child }
            }
        }

        // Go through every resource and check if the resource has a reference to the resource being deleted
        // if there is remove the reference
        bundle.entry.forEach { res ->
            res.resource.children().stream().flatMap { getValues(it) }.forEach { property ->
                property.values.forEach {
                    if (it is Reference && it.reference == resource.idBase) {
                        it.reference = null
                        it.resource = null
                    }
                }
            }
        }

        // finally remove the resource from the bundle
        bundle.entry.removeIf { it.fullUrl == resource.idBase }
    }
    @Test
    fun `Test Removing organization from Bundle`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        val bundle = messages[0]
        bundle.id
        var observation = bundle.entry.find {
            it.fullUrl == "Observation/1671741861219479500.1e349936-127c-4edc-8d77-39fb231f4391"
        }!!.resource
        var diagnosticReport = bundle.entry.find {
            it.fullUrl == "DiagnosticReport/1671741861666720300.74d1219e-940e-4280-bf75-1018bdb1655a"
        }!!.resource
        var diagnosticReportReferences = getReferences(diagnosticReport)
        var observationReportReferences = getReferences(observation)

        assertThat(diagnosticReportReferences.contains(observation.id)).isTrue()
        assertThat(observationReportReferences.count()).isEqualTo(4)
        assertThat(
            bundle.entry.find {
                it.fullUrl == "PractitionerRole/1671741861200480800.16fc7859-a7b9-4896-9603-64eeeb9abe5d"
            }
        ).isNotNull()

        deleteResource(observation, messages[0])

        val newBundle = messages[0]
        diagnosticReport = FhirPathUtils.evaluate(
            null, bundle, bundle, "Bundle.entry.resource.ofType(DiagnosticReport)[0]"
        )[0] as Resource
        diagnosticReportReferences = getReferences(diagnosticReport)

        assertThat(
            newBundle.entry.find {
                it.fullUrl == "Observation/1671741861219479500.1e349936-127c-4edc-8d77-39fb231f4391"
            }
        ).isNull()
        assertThat(
            newBundle.entry.find {
                it.fullUrl == "PractitionerRole/1671741861200480800.16fc7859-a7b9-4896-9603-64eeeb9abe5d"
            }
        ).isNull()
        assertThat(
            newBundle.entry.find {
                it.fullUrl == "PractitionerRole/1671741861200480800.16fc7859-a7b9-4896-9603-64eeeb9abe5d"
            }
        ).isNull()
        assertThat(diagnosticReportReferences.contains(observation.id)).isFalse()
    }

    @Test
    fun `Test Removing organization from Bundle 2`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        val bundle = messages[0]

        var diagnosticReport = FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(DiagnosticReport)[0]"
        )[0]

        var observation = (
            (
                FhirPathUtils.evaluate(
                    null,
                    diagnosticReport,
                    bundle,
                    "%resource.result[0]"
                )[0] as Reference
                ).resource
            ) as Base

        var diagnosticReportReferences = getReferences(diagnosticReport)
        var observationReportReferences = getReferences(observation)

        print(diagnosticReportReferences)
        print(observationReportReferences)

        assertThat(diagnosticReportReferences.contains(observation.idBase)).isTrue()
        assertThat(observationReportReferences.count()).isEqualTo(4)
        assertThat(
            bundle.entry.find {
                it.fullUrl == "PractitionerRole/1671741861200480800.16fc7859-a7b9-4896-9603-64eeeb9abe5d"
            }
        ).isNotNull()

        deleteResource(observation, messages[0])

        val newBundle = messages[0]
        diagnosticReport =
            FhirPathUtils.evaluate(null, bundle, bundle, "Bundle.entry.resource.ofType(DiagnosticReport)[0]")[0]
        diagnosticReportReferences = getReferences(diagnosticReport)

        assertThat(
            newBundle.entry.find {
                it.fullUrl == "Observation/1671741861219479500.1e349936-127c-4edc-8d77-39fb231f4391"
            }
        ).isNull()
        assertThat(
            newBundle.entry.find {
                it.fullUrl == "PractitionerRole/1671741861200480800.16fc7859-a7b9-4896-9603-64eeeb9abe5d"
            }
        ).isNull()
        assertThat(
            newBundle.entry.find {
                it.fullUrl == "Organization/1671741861218481400.fc5a9a6d-b7af-42ae-9af6-fd23fa7512da"
            }
        ).isNull()
        assertThat(diagnosticReportReferences.contains(observation.idBase)).isFalse()
    }
}