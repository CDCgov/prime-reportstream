package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import java.util.UUID
import kotlin.test.Test

class ActionLogTests {
    @Test
    fun `test action log creation`() {
        val logger = ActionLogger()

        assertThat(logger.isEmpty()).isTrue()

        // Add a warning
        logger.warn(InvalidReportMessage("some message"))
        assertThat(logger.isEmpty()).isFalse()
        assertThat(logger.hasErrors()).isFalse()
        assertThat(logger.logs.size).isEqualTo(1)
        assertThat(logger.warnings.size).isEqualTo(1)
        assertThat(logger.errors).isEmpty()

        // Now an error
        logger.error(InvalidTranslationMessage("another message"))
        assertThat(logger.hasErrors()).isTrue()
        assertThat(logger.logs.size).isEqualTo(2)
        assertThat(logger.warnings.size).isEqualTo(1)
        assertThat(logger.warnings[0].detail is InvalidReportMessage).isTrue()
        assertThat(logger.errors.size).isEqualTo(1)
        assertThat(logger.errors[0].detail is InvalidTranslationMessage).isTrue()

        // Item logs
        assertThat { logger.getItemLogger(0) }.isFailure()
        assertThat { logger.getItemLogger(-100) }.isFailure()
        val index = 1
        val trackingId = "tracking"
        assertThat { logger.error(InvalidEquipmentMessage("some mapping")) }.isFailure()
        val itemLogger = logger.getItemLogger(index, trackingId)
        assertThat { itemLogger.error(InvalidEquipmentMessage("some mapping")) }.isSuccess()
        assertThat(logger.errors.size).isEqualTo(2)
        assertThat(logger.errors[1].index).isEqualTo(index)
        assertThat(logger.errors[1].trackingId).isEqualTo(trackingId)

        // Set the report ID
        logger.logs.forEach { assertThat(it.reportId).isNull() }
        val reportId = UUID.randomUUID()
        logger.setReportId(reportId)
        // Make sure all logs now have a report ID
        logger.logs.forEach { assertThat(it.reportId).isEqualTo(reportId) }
        // Also make sure any new logs has a report ID.
        logger.error(InvalidReportMessage("some other message"))
        logger.logs.forEach { assertThat(it.reportId).isEqualTo(reportId) }

        // Exception creation
        val exception: ActionError = logger.exception
        assertThat(exception.message).isNotEmpty()
    }
}