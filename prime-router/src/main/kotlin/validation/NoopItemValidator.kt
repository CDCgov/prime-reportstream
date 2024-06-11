package gov.cdc.prime.router.validation

class NoopItemValidationResult : IItemValidationResult {
    override fun isValid(): Boolean {
        return true
    }

    override fun getErrorsMessage(validator: IItemValidator): String {
        return ""
    }
}

class NoopItemValidator : AbstractItemValidator() {

    override fun validate(message: Any): IItemValidationResult {
        return NoopItemValidationResult()
    }

    override val validatorProfileName: String = "NOOP"
}