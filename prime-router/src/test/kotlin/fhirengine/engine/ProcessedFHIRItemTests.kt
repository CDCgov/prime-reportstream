package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.isEqualTo
import fhirengine.engine.ProcessedFHIRItem
import fhirengine.engine.ProcessedHL7Item
import fhirengine.translation.hl7.structures.fhirinventory.message.ORU_R01
import gov.cdc.prime.router.ErrorCode
import org.hl7.fhir.r4.model.Bundle
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProcessedFHIRItemTests {

    @Test
    fun `should return an error when present`() {
        val error = FHIRConverter.InvalidItemActionLogDetail(ErrorCode.UNKNOWN, 0, "")
        assertThat(
            ProcessedFHIRItem("", 0)
                .updateParsed(error).getError()
        ).isEqualTo(error)

        assertThat(
            ProcessedFHIRItem("", 0)
                .updateParsed(Bundle()).updateValidation(error).getError()
        ).isEqualTo(error)
    }

    @Test
    fun `should only update validation when there is parsed item`() {
        val error = FHIRConverter.InvalidItemActionLogDetail(ErrorCode.UNKNOWN, 0, "")
        assertThrows<RuntimeException> {
            ProcessedFHIRItem("", 0)
                .updateValidation(error)
        }
    }

    @Test
    fun `should only set the bundle if there is no parse error or validation error`() {
        val error = FHIRConverter.InvalidItemActionLogDetail(ErrorCode.UNKNOWN, 0, "")
        assertThrows<RuntimeException> {
            ProcessedFHIRItem("", 0)
                .updateParsed(error)
                .setBundle(Bundle())
        }

        assertThrows<RuntimeException> {
            ProcessedFHIRItem("", 0)
                .updateParsed(Bundle())
                .updateValidation(error)
                .setBundle(Bundle())
        }
    }
}

class ProcessedHL7ItemTests {

    @Test
    fun `should return an error when present`() {
        val error = FHIRConverter.InvalidItemActionLogDetail(ErrorCode.UNKNOWN, 0, "")
        assertThat(
            ProcessedHL7Item("", 0)
                .updateParsed(error).getError()
        ).isEqualTo(error)

        assertThat(
            ProcessedHL7Item("", 0)
                .updateParsed(ORU_R01()).updateValidation(error).getError()
        ).isEqualTo(error)

        assertThat(
            ProcessedHL7Item("", 0)
                .updateParsed(ORU_R01()).updateValidation(error).getError()
        ).isEqualTo(error)

        assertThat(
            ProcessedHL7Item("", 0)
                .updateParsed(ORU_R01()).setConversionError(error).getError()
        ).isEqualTo(error)
    }

    @Test
    fun `should only update validation when there is parsed item`() {
        val error = FHIRConverter.InvalidItemActionLogDetail(ErrorCode.UNKNOWN, 0, "")
        assertThrows<RuntimeException> {
            ProcessedHL7Item("", 0)
                .updateValidation(error)
        }
    }

    @Test
    fun `should only set the bundle if there is no parse error, validation error or conversion error`() {
        val error = FHIRConverter.InvalidItemActionLogDetail(ErrorCode.UNKNOWN, 0, "")
        assertThrows<RuntimeException> {
            ProcessedHL7Item("", 0)
                .updateParsed(error)
                .setBundle(Bundle())
        }

        assertThrows<RuntimeException> {
            ProcessedHL7Item("", 0)
                .updateParsed(ORU_R01())
                .updateValidation(error)
                .setBundle(Bundle())
        }

        assertThrows<RuntimeException> {
            ProcessedHL7Item("", 0)
                .updateParsed(ORU_R01())
                .setConversionError(error)
                .setBundle(Bundle())
        }
    }
}