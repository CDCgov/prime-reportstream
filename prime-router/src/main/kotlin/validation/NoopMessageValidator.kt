package gov.cdc.prime.router.validation

class NoopMessageValidationResult : IMessageValidationResult {
    override fun isValid(): Boolean {
        return true
    }

    override fun getErrorsMessage(): String {
        return ""
    }
}

class NoopMessageValidator : AbstractMessageValidator() {

    override fun validate(message: Any): IMessageValidationResult {
        return NoopMessageValidationResult()
    }
}