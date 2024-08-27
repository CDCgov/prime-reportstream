package gov.cdc.prime.reportstream.auth.controller

import org.apache.logging.log4j.kotlin.Logging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionHandlerController : Logging {

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<Any> {
        logger.error("Uncaught exception", ex)

        return ResponseEntity(
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}