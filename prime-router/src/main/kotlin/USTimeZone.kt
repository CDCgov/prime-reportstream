package gov.cdc.prime.router

/** The time zones our partners are in */
enum class USTimeZone(val zoneId: String) {
    PACIFIC("US/Pacific"), // For US Pacific timezone
    MOUNTAIN("US/Mountain"), // For US Mountain timezone
    ARIZONA("US/Arizona"), // For Arizona timezone (no daylight savings)
    CENTRAL("US/Central"), // For US Central timezone
    EASTERN("US/Eastern"), // For US Eastern timezone
    SAMOA("US/Samoa"), // For American Samoa timezone
    HAWAII("US/Hawaii"), // For Hawaii timezone
    EAST_INDIANA("US/East-Indiana"), // For Eastern Indiana timezone
    INDIANA_STARKE("US/Indiana-Starke"), // For Starke County, Indiana timezone
    MICHIGAN("US/Michigan"), // For Michigan timezone
    CHAMORRO("Pacific/Guam"), // For Guam and Chamorro timezone
    ALASKA("US/Alaska"), // For Alaska timezone
    PUERTO_RICO("Cuba"), // For Puerto Rico timezone
    US_VIRGIN_ISLANDS("America/St_Thomas"), // For US Virgin Islands timezone
    CHUUK("Pacific/Chuuk"), // For Chuuk, Micronesia timezone
    POHNPEI("Pacific/Pohnpei"), // For Pohnpei, Micronesia timezone
    KOSRAE("Pacific/Kosrae"), // For Kosrae, Micronesia timezone
    NORTHERN_MARIANA_ISLANDS("Pacific/Guam"), // For Northern Mariana Islands timezone

    // not technically a US time zone but we need it
    UTC("UTC"),

    // Marshall Island timezone (UTC+12)
    MAJURO("Pacific/Majuro"),
}