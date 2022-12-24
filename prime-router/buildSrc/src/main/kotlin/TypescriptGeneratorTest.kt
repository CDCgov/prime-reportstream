import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreType

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TsExport

@JsonIgnoreType
class TsExportJsonIgnoreTypeTest (
    val bar: String
)

interface TsExportInterface {
    val foo: String?
}

/**
 * Typescript export should only have "test" and "foo" properties
 * (unable to handle JsonIgnore override currently)
 */
class TsExportManualTest (
    @JsonIgnore val ignoreField: String,
    @get:JsonIgnore val ignoreGet: String,
    val ignoreType: TsExportJsonIgnoreTypeTest,
    val bar: String
) : TsExportInterface {
    @get:JsonIgnore
    override val foo: String? = null
}

@TsExport
class TsExportAnnotationTest(val foo: String)