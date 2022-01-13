package gov.cdc.prime.router

import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import org.jooq.BindingGetResultSetContext
import org.jooq.BindingSQLContext
import org.jooq.BindingSetStatementContext
import org.jooq.Converter
import org.jooq.JSONB
import org.jooq.impl.AbstractBinding
import org.jooq.impl.DSL
import java.util.Objects

class JsonConverter<T> (val c: Class<T>) : Converter<JSONB, T> {
    private val mapper = jacksonMapperBuilder().build()

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