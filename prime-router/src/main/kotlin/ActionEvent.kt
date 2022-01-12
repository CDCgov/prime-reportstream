package gov.cdc.prime.router
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import org.jooq.BindingGetResultSetContext
import org.jooq.BindingSQLContext
import org.jooq.BindingSetStatementContext
import org.jooq.Converter
import org.jooq.JSONB
import org.jooq.impl.AbstractBinding
import org.jooq.impl.DSL
import java.time.Instant
import java.util.Objects
import java.util.UUID

private val mapper = jacksonMapperBuilder().build()

class JsonConverter<T> (val c: Class<T>) : Converter<JSONB, T> {

    override fun from(dbObject: JSONB): T {
        return mapper.readValue(dbObject.toString(), c)
    }

    override fun to(experiment: T): JSONB {
        return JSONB.valueOf(mapper.writeValueAsString(experiment))
    }

    override fun fromType(): Class<JSONB> {
        return JSONB::class.java
    }

    override fun toType(): Class<T> {
        return c
    }
}

abstract class JsonBinding<T> (val klass: Class<T>) : AbstractBinding<JSONB, T>() {
    override fun converter(): Converter<JSONB, T> {
        return JsonConverter(klass)
    }

    override fun sql(ctx: BindingSQLContext<T>) {
        val convert = ctx.convert(converter())
        var value = convert.value()

        val param = DSL.`val`(value)
        val visit = ctx.render().visit(param)
        visit.sql("::jsonb")
    }

    override fun get(ctx: BindingGetResultSetContext<T>) {
        ctx.convert(converter()).value(JSONB.valueOf(ctx.resultSet().getString(ctx.index())))
    }

    override fun set(ctx: BindingSetStatementContext<T>) {
        ctx.statement()
            .setString(
                ctx.index(),
                Objects.toString(ctx.convert(converter()).value(), null)
            )
    }
}

class ActionEventDetailBinding : JsonBinding<ActionEventDetail>(ActionEventDetail::class.java)

/**
 * @property scope of the result detail
 * @property id of the result (depends on scope)
 * @property details of the result
 * @property row of csv related to message (set to -1 if not applicable)
 */
data class ActionEvent(
    val scope: DetailScope,
    val trackingId: String,
    val detail: ActionEventDetail,
    val index: Int? = null,
    var reportId: UUID? = null,
    var action: Action? = null,
    val type: Type = Type.info,
    val time: Instant = Instant.now(),
) {

    val rowNumber: Int
        get() = index?.let { it + 1 } ?: -1

    fun getActionId(): Long {
        return action!!.actionId
    }

    /**
     * @property REPORT scope for the detail
     * @property ITEM scope for the detail
     */
    enum class DetailScope { parameter, report, item, translation }

    enum class Type { info, warning, error, filter }

    companion object {
        fun report(message: ActionEventDetail, type: Type, reportId: UUID? = null): ActionEvent {
            return ActionEvent(DetailScope.report, "", message, type = type, reportId = reportId)
        }

        fun report(message: String, type: Type): ActionEvent {
            val reportMessage = InvalidReportMessage(message)
            return ActionEvent(DetailScope.report, "", reportMessage, type = type)
        }

        fun item(trackingId: String, message: ActionEventDetail, row: Int, type: Type): ActionEvent {
            return ActionEvent(DetailScope.item, trackingId, message, row, type = type)
        }

        fun param(trackingId: String, message: ActionEventDetail, type: Type = Type.error): ActionEvent {
            return ActionEvent(DetailScope.parameter, trackingId, message, type = type)
        }
    }
}

/**
 * ActionError is a throwable for cases where an event during an action
 * is a true error that prevents subsequent behavior.
 */
class ActionError(message: String?, val details: List<ActionEvent>) : Error(message) {
    constructor(message: String?, detail: ActionEvent) : this(message, listOf(detail))
    constructor(details: List<ActionEvent>) : this(null, details)
}