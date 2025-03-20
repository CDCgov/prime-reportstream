package gov.cdc.prime.router.config.validation

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.ValidationError

/**
 * Service to validate configuration values
 */
interface ConfigurationValueValidationService {

    /**
     * Validate Configuration values using the Konform library
     */
    fun <T> validate(
        configType: ConfigurationType<T>,
        config: T,
    ): ConfigurationValidationResult<T>
}

class ConfigurationValueValidationServiceImpl : ConfigurationValueValidationService {

    override fun <T> validate(
        configType: ConfigurationType<T>,
        config: T,
    ): ConfigurationValidationResult<T> = when (val result = configType.konformValidation.validation.validate(config)) {
            is Valid -> {
                ConfigurationValidationSuccess(config)
            }
            is Invalid -> {
                val errors = result.errors.map(::formatErrorMessage)
                ConfigurationValidationFailure(errors)
            }
        }

    private fun formatErrorMessage(error: ValidationError): String = "path=${error.dataPath}, message=${error.message}"
}