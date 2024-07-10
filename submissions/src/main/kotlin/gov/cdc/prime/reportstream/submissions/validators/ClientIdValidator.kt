package gov.cdc.prime.reportstream.submissions.validators

import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

@Component
class ClientIdValidator : Validator {
    override fun supports(clazz: Class<*>): Boolean = String::class.java == clazz

    override fun validate(target: Any, errors: Errors) {
        val clientId = target as String
        if (clientId.isEmpty()) {
            errors.rejectValue("client_id", "client_id.empty", "client_id must not be empty")
        }
    }
}