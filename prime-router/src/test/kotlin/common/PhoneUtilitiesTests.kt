package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isNullOrEmpty
import kotlin.test.Test

class PhoneUtilitiesTests {

    @Test
    fun `test try parse Phone Number`() {
        listOf(
            "+1-316-667-9400", // US
            "(230)7136595", // US
            "+61 2 6214 5600", // AU
            "613-688-5335", // CA
            "+1613-688-5335", // CA
            "+52 55 5080 2000" // MX
        ).forEach {
            assertThat(PhoneUtilities.getPhoneNumberPart(it, PhonePart.Country)).isNotNull()
            assertThat(PhoneUtilities.getPhoneNumberPart(it, PhonePart.AreaCode)).isNotNull()
            assertThat(PhoneUtilities.getPhoneNumberPart(it, PhonePart.Local)).isNotNull()
        }

        listOf(
            "",
            "abcdefghijk",
            "           ",
            "99999999999999999999999999999999999999999999999999999999999999",
            "9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9 9"
        ).forEach {
            val countryCode = PhoneUtilities.getPhoneNumberPart(it, PhonePart.Country)
            assertThat(countryCode).isNull()
        }
    }

    @Test
    fun `test getting phone number parts method, no extension`() {
        listOf(
            "+1-316-667-9400", // US
            "(717)531-0123", // US alternate formatting
            "+61 2 9667 9111", // AU
            "+61 491 578 888", // AU cell number
            "613-688-5335", // CA
            "+1613-688-5335", // CA
            "+52 55 5080 2000", // MX
            "(230)7136595", // US, but invalid area code, but pass it through anyway
            "213 555 5555", // US format
            "(985) 845-3258" // US format
        ).forEach {
            assertThat(PhoneUtilities.getPhoneNumberPart(it, PhonePart.Country)).isNotNull()
            assertThat(PhoneUtilities.getPhoneNumberPart(it, PhonePart.AreaCode)).isNotNull()
            assertThat(PhoneUtilities.getPhoneNumberPart(it, PhonePart.Local)).isNotNull()
            assertThat(PhoneUtilities.getPhoneNumberPart(it, PhonePart.Extension)).isNullOrEmpty()
        }
    }

    @Test
    fun `test getting phone number parts method, with extension`() {
        listOf(

            "+91(213) 555 5555 # 1234", // International (India) with extension number
            "356-683-6541 x 1234", // US with extension number
            "+91 (714) 726-1687 ext. 7923", // International (India) with extension
            "(818) 265-7536 ext. 5264", // US with extension
            "(874) 951-2157 # 8562", // US with extension
            "+52 (213)478 9621 x 548", // MX with extension
            "(310)852-9654ext.4562", // US extension variant
            "(946)451-7653ext1254", // US extension variant
            "(213)353-4836#852", // US extension variant
            "(661)187-6589x7328", // US extension variant
        ).forEach {
            assertThat(PhoneUtilities.getPhoneNumberPart(it, PhonePart.Country)).isNotNull()
            assertThat(PhoneUtilities.getPhoneNumberPart(it, PhonePart.AreaCode)).isNotNull()
            assertThat(PhoneUtilities.getPhoneNumberPart(it, PhonePart.Local)).isNotNull()
            assertThat(PhoneUtilities.getPhoneNumberPart(it, PhonePart.Extension)).isNotEqualTo("")
        }
    }

    @Test
    fun `test getting phone number parts method, correct country code`() {
        val withCountryCode = "+91(213) 555 5555 # 1234" // International (India) with extension number
        val usCountryCode = "+1 356-683-6541"
        val noCountryCode = "356-683-6541"
        assertThat(PhoneUtilities.getPhoneNumberPart(withCountryCode, PhonePart.Country)).equals("91")
        assertThat(PhoneUtilities.getPhoneNumberPart(usCountryCode, PhonePart.Country)).equals("1")
        assertThat(PhoneUtilities.getPhoneNumberPart(noCountryCode, PhonePart.Country)).equals("1")
    }
}