package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.apache.logging.log4j.kotlin.Logging

/**
 * Enumeration representing the different parts of a phone number.
 */
enum class PhonePart {
    Country,
    AreaCode,
    Local,
    Extension
}

/**
 * A collection of methods for dealing with phone parts.
 */
object PhoneUtilities : Logging {

    /**
     *  Parses the [cleanedValue] into a PhoneNumber and returns the [part] requested.
     *  @return either the part of the phone number requested, or null if the phone number was invalid
     */
    fun getPhoneNumberPart(cleanedValue: String, part: PhonePart): String? {
        val phoneNumberUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

        // determine if this is a phone number
        if (
            phoneNumberUtil.isPossibleNumber(cleanedValue, "US")
        ) {
            val phone = phoneNumberUtil.parse(cleanedValue, "US")
            return try {
                when (part) {
                    PhonePart.Country -> phone.countryCode.toString()
                    PhonePart.AreaCode -> phone.nationalNumber.toString().substring(0, 3)
                    PhonePart.Local -> phone.nationalNumber.toString().substring(3)
                    PhonePart.Extension -> phone.extension
                }
            } catch (e: StringIndexOutOfBoundsException) {
                logger.warn("Invalid phone number sent.")
                null
            }
        }
        return null
    }

    /**
     * Parses a passed in [cleanedValue] into a PhoneNumber and determines if it has an extension or not.
     * @return a boolean indicating if the value passed in is a phone number with an extension
     */
    fun hasPhoneNumberExtension(cleanedValue: String): Boolean {
        val phoneNumberUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
        // determine if this is a phone number
        if (
            phoneNumberUtil.isPossibleNumber(cleanedValue, "US")
        ) {
            val phone = phoneNumberUtil.parse(cleanedValue, "US")
            return phone.hasExtension()
        }
        return false
    }
}