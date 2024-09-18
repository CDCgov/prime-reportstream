package gov.cdc.prime.reportstream.shared


annotation class OptionDeprecated

enum class ReportOptions {
    None,
    ValidatePayload,
    CheckConnections,
    SkipSend,
    SendImmediately,

    @OptionDeprecated
    SkipInvalidItems,

    ;

    class InvalidOptionException(message: String) : Exception(message)

    /**
     * Checks to see if the enum constant has an @OptionDeprecated annotation.
     * If the annotation is present, the constant is no longer in use.
     */

    val isDeprecated = this.declaringJavaClass.getField(this.name)
        .getAnnotation(OptionDeprecated::class.java) != null

    companion object {
        /**
         * ActiveValues is a list of the non-deprecated options that can be used when submitting a report
         */

        val activeValues = mutableListOf<ReportOptions>()

        init {
            ReportOptions.values().forEach {
                if (!it.isDeprecated) activeValues.add(it)
            }
        }

        /**
         * Handles invalid values, which are technically not allowed in an enum. In this case if the [input]
         *  is not one that is supported, it will be set to None.
         */
        fun valueOfOrNone(input: String): ReportOptions = try {
            valueOf(input)
        } catch (ex: IllegalArgumentException) {
            val msg = "$input is not a valid Option. Valid options: ${ReportOptions.activeValues.joinToString()}"
            throw InvalidOptionException(msg)
        }
    }

}