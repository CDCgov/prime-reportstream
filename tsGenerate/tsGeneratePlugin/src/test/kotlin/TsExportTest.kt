package gov.cdc.prime.tsGeneratePlugin

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreType
import gov.cdc.prime.tsGenerateLibrary.TsExport

/**
 * Typescript export will not include properties with this type
 */
@JsonIgnoreType
class TsExportJsonIgnoreTypeTest(
    val bar: String
)

/**
 * Nullable type interface
 */
interface TsExportInterface {
    val foo: String?
}

/**
 * Test class load by annotation
 */
@TsExport
class TsExportAnnotationTest(val foo: String)

/**
 * Test class load by manual fqn.
 * Typescript export will only have "test" and "foo" properties.
 * "foo" will be overriden to be undefined only.
 */
class TsExportManualTest(
    @JsonIgnore val ignoreField: String,
    @get:JsonIgnore val ignoreGet: String,
    val ignoreType: TsExportJsonIgnoreTypeTest,
    val bar: String
) : TsExportInterface {
    @get:JsonIgnore
    override val foo: String? = null
}