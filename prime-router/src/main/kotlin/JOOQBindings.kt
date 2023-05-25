package gov.cdc.prime.router

import gov.cdc.prime.router.common.JacksonMapperUtilities
import org.jooq.BindingGetResultSetContext
import org.jooq.BindingSQLContext
import org.jooq.BindingSetStatementContext
import org.jooq.Converter
import org.jooq.JSONB
import org.jooq.impl.AbstractBinding
import org.jooq.impl.DSL
import java.lang.Exception
import java.util.Objects

/**
 * Provides a converter for POJOs into JSONB and back using the jackson library
 * and it's kotlin extensions.
 *
 * @param c The class of the POJO for a converter
 */
class JsonConverter<T>(val c: Class<T>) : Converter<JSONB, T> {
    private val mapper = JacksonMapperUtilities.defaultMapper

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

/**
 * Provides a binding for POJOs into JSONB and back using the jackson library
 * and it's kotlin extensions.
 *
 * NOTE: these then have to be registered as a forced type in build.gradle.kts
 *
 * @param klass The class of the POJO this should be used in the inheritance for the POJO specific binding
 */
abstract class JsonBinding<T>(val klass: Class<T>) : AbstractBinding<JSONB, T>() {
    override fun converter(): Converter<JSONB, T> {
        return JsonConverter(klass)
    }

    override fun sql(ctx: BindingSQLContext<T>) {
        val convert = ctx.convert(converter())
        val value = convert.value()

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

/**
 * A binding for ActionLogDetails to be converted to and from a JSONB column by JOOQ
 */
class ActionLogDetailBinding : JsonBinding<ActionLogDetail>(ActionLogDetail::class.java)

class TopicConverter : Converter<String, Topic> {
    override fun from(dbObject: String): Topic {
        return when (dbObject) {
            "full-elr" -> Topic.FULL_ELR
            "etor-ti" -> Topic.ETOR_TI
            "covid-19" -> Topic.COVID_19
            "monkeypox" -> Topic.MONKEYPOX
            "CsvFileTests-topic" -> Topic.CSV_TESTS
            "test" -> Topic.TEST
            else -> throw Exception()
        }
    }

    override fun to(topic: Topic): String {
        return topic.jsonVal
    }

    override fun fromType(): Class<String> {
        return String::class.java
    }

    override fun toType(): Class<Topic> {
        return Topic::class.java
    }
}

class TopicBinding : AbstractBinding<String, Topic>() {
    override fun converter(): Converter<String, Topic> {
        return TopicConverter()
    }

    override fun get(ctx: BindingGetResultSetContext<Topic>) {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()))
    }

    override fun set(ctx: BindingSetStatementContext<Topic>) {
        ctx.statement()
            .setString(
                ctx.index(),
                Objects.toString(ctx.convert(converter()).value(), null)
            )
    }
}