package gov.cdc.prime.router

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PropertyConversionMethod

val supabase = createSupabaseClient(
    supabaseUrl = "https://postgrest:3002",
    supabaseKey = ""

) {
    install(Postgrest) {
        defaultSchema = "public" // default: "public"
        propertyConversionMethod =
            PropertyConversionMethod.SERIAL_NAME // default: PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE
        customUrl = "http://localhost:3002"
        jwtToken = null
        // serializer = JacksonSerializer()
    }
}