package gov.cdc.prime.router

class FakeTable {
    companion object {
        fun build(name: String, schema: Schema, count: Int = 10): MappableTable {
            return MappableTable(name, schema, listOf())
        }
    }
}