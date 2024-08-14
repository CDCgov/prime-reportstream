package gov.cdc.prime.router.metadata

class ObservationMappingConstants {
    companion object {
        const val TEST_CODE_KEY = "Code"
        const val TEST_CODESYSTEM_KEY = "Code System"
        const val TEST_OID_KEY = "Member OID"
        const val TEST_NAME_KEY = "Name"
        const val TEST_DESCRIPTOR_KEY = "Descriptor"
        const val TEST_VERSION_KEY = "Version"
        const val TEST_STATUS_KEY = "Status"

        const val CONDITION_NAME_KEY = "condition_name"
        const val CONDITION_CODE_KEY = "condition_code"
        const val CONDITION_CODE_SYSTEM_KEY = "Condition Code System"
        const val CONDITION_CODE_SYSTEM_VERSION_KEY = "Condition Code System Version"
        const val CONDITION_VALUE_SOURCE_KEY = "Value Source"
        const val CONDITION_CREATED_AT = "Created At"

        const val TEST_CODESYSTEM_LOINC = "LOINC"
        const val TEST_CODESYSTEM_SNOMEDCT = "SNOMEDCT"
        const val VSAC_CODESYSTEM_LOINC = "http://loinc.org"
        const val VSAC_CODESYSTEM_SNOMEDCT = "http://snomed.info/sct"

        val TEST_KEYS = listOf(
            TEST_CODE_KEY, TEST_CODESYSTEM_KEY, TEST_OID_KEY, TEST_NAME_KEY,
            TEST_DESCRIPTOR_KEY, TEST_VERSION_KEY, TEST_STATUS_KEY
        )
        val CONDITION_KEYS = listOf(
            CONDITION_NAME_KEY, CONDITION_CODE_KEY, CONDITION_CODE_SYSTEM_KEY,
            CONDITION_CODE_SYSTEM_VERSION_KEY, CONDITION_VALUE_SOURCE_KEY, CONDITION_CREATED_AT
        )

        val TEST_CODESYSTEM_MAP = mapOf(
            VSAC_CODESYSTEM_SNOMEDCT to TEST_CODESYSTEM_SNOMEDCT,
            VSAC_CODESYSTEM_LOINC to TEST_CODESYSTEM_LOINC
        )

        val ALL_KEYS = TEST_KEYS + CONDITION_KEYS
    }
}