package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

/** The time zones our partners are in */
enum class USTimeZone(val zoneId: String) {
    PACIFIC("US/Pacific"),
    MOUNTAIN("US/Mountain"),
    ARIZONA("US/Arizona"),
    CENTRAL("US/Central"),

    @JsonEnumDefaultValue
    EASTERN("US/Eastern"),
    SAMOA("US/Samoa"),
    HAWAII("US/Hawaii"),
    EAST_INDIANA("US/East-Indiana"),
    INDIANA_STARKE("US/Indiana-Starke"),
    MICHIGAN("US/Michigan"),
    CHAMORRO("Pacific/Guam"),

    // not technically a US time zone but we need it
    UTC("UTC"),
}