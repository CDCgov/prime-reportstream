package gov.cdc.prime.router.validation

class NoopItemValidationResult : IItemValidationResult {
    override fun isValid(): Boolean = true

    override fun getErrorsMessage(validator: IItemValidator): String = ""
}

class NoopItemValidator : AbstractItemValidator() {

    override fun validate(message: Any): IItemValidationResult = NoopItemValidationResult()

    override val validatorProfileName: String = "NOOP"
}