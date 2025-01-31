package gov.cdc.prime.router.config.validation

/**
 * Top level trait for a validation result
 */
sealed interface ConfigurationValidationResult<T>

/**
 * A successful configuration validation containing
 * the parsed class
 */
data class ConfigurationValidationSuccess<T>(val parsed: T) : ConfigurationValidationResult<T>

/**
 * A failed configuration validation containing errors
 * and an optional thrown exception
 */
data class ConfigurationValidationFailure<T>(val errors: List<String>, val cause: Throwable? = null) :
    ConfigurationValidationResult<T>