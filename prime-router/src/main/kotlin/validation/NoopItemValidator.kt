package gov.cdc.prime.router.validation

class NoopItemValidationResult : IItemValidationResult {
    override fun isValid(): Boolean {
        return true
    }

    override fun getErrorsMessage(): String {
        return ""
    }
}

class NoopItemValidator : AbstractItemValidator() {

    override fun validate(message: Any): IItemValidationResult {
        return NoopItemValidationResult()
    }
}