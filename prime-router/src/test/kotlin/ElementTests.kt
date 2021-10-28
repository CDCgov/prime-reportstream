package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.startsWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertFails

internal class ElementTests {

    @Test
    fun `create element`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        assertThat(elem1).isNotNull()
    }

    @Test
    fun `compare elements`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        val elem2 = Element(name = "first", type = Element.Type.NUMBER)
        assertThat(elem1).isEqualTo(elem2)

        val elem3 = Element(name = "first", type = Element.Type.TEXT)
        assertThat(elem1).isNotEqualTo(elem3)
    }

    @Test
    fun `test schema element`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        val elem2 = Element(name = "first", type = Element.Type.NUMBER)
        assertThat(elem1).isEqualTo(elem2)
        val elem3 = Element(name = "first", type = Element.Type.TEXT)
        assertThat(elem1).isNotEqualTo(elem3)
    }

    @Test
    fun `test toNormalize altValues`() {
        val one = Element(
            "b",
            type = Element.Type.CODE,
            altValues = listOf(
                // Use french as an alternative display for these code
                ValueSet.Value("Y", "Oui"),
                ValueSet.Value("N", "Non"),
                ValueSet.Value("UNK", "?")
            ),
            csvFields = Element.csvFields("b", format = "\$alt")
        )
        one.toNormalized("Non", "\$alt").run {
            assertThat(this).isEqualTo("N")
        }
        one.toNormalized("Oui", "\$alt").run {
            assertThat(this).isEqualTo("Y")
        }
    }

    @Test
    fun `test toNormalize date`() {
        val one = Element(
            "a",
            type = Element.Type.DATE,
        )
        // Iso formatted should work
        one.toNormalized("1998-03-30").run {
            assertThat(this).isEqualTo("19980330")
        }
        one.toNormalized("1998-30-03", "yyyy-dd-MM").run {
            assertThat(this).isEqualTo("19980330")
        }
    }

    @Test
    fun `test toNormalize dateTime`() {
        val one = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Iso formatted should work
        one.toNormalized("1998-03-30T12:00Z").run {
            assertThat(this).isEqualTo("199803301200+0000")
        }
        one.toNormalized("199803300000+0000").run {
            assertThat(this).isEqualTo("199803300000+0000")
        }
        one.toNormalized("20210908105903").run {
            assertThat(this).startsWith("202109081059")
        }
        val two = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        val o = ZoneId.of(USTimeZone.CENTRAL.zoneId).rules.getOffset(Instant.now()).toString()
        val offset = if (o == "Z") {
            "+0000"
        } else {
            o.replace(":", "")
        }
        two.toNormalized("19980330", "yyyyMMdd").run {
            assertThat(this).isEqualTo("199803300000$offset")
        }
        val three = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        three.toNormalized("2020-12-09", "yyyy-MM-dd").run {
            assertThat(this).isEqualTo("202012090000$offset")
        }
        mapOf(
            "20210908105903" to "20210908105903",
            "199803300000+0000" to "19980330000000"
        ).forEach {
            val optionalDateTime = "[yyyyMMddHHmmssZ][yyyyMMddHHmmZ][yyyyMMddHHmmss]"
            val df = DateTimeFormatter.ofPattern(optionalDateTime)
            val ta = df.parseBest(it.key, OffsetDateTime::from, LocalDateTime::from, Instant::from)
            val dt = LocalDateTime.from(ta)
            assertThat(df.format(dt)).isEqualTo(it.value)
        }
    }

    @Test
    fun `test toFormatted dateTime`() {
        val one = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Iso formatted should work
        one.toFormatted("199803301200").run {
            assertThat(this).isEqualTo("199803301200")
        }
        one.toFormatted("199803300000").run {
            assertThat(this).isEqualTo("199803300000")
        }

        val two = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Other formats should work too, including sans the time.
        two.toFormatted("202012090000+0000", "yyyy-MM-dd").run {
            assertThat(this).isEqualTo("2020-12-09")
        }
    }

    @Test
    fun `test toNormalized zip`() {
        val one = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        one.toNormalized("99999").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toNormalized("99999-9999").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        // format should not affect normalization
        one.toNormalized("999999999", "\$zipFive").run {
            assertThat(this).isEqualTo("999999999")
        }
        one.toNormalized("KY1-6666").run {
            assertThat(this).isEqualTo("KY1-6666")
        } // Cayman zipcode
        one.toNormalized("KX33-77777").run {
            assertThat(this).isEqualTo("KX33-77777")
        } // Letters and numbers, but a made up zipcode
        assertFails {
            one.toNormalized("%%%%XXXX") // Unreasonable
        }
    }

    @Test
    fun `test toNormalized phone`() {
        val one = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
        one.toNormalized("5559938322").run {
            assertThat(this).isEqualTo("5559938322:1:")
        }
        one.toNormalized("1(555)-968-5052").run {
            assertThat(this).isEqualTo("5559685052:1:")
        }
        one.toNormalized("555-968-5052").run {
            assertThat(this).isEqualTo("5559685052:1:")
        }
        one.toNormalized("+1(555)-968-5052").run {
            assertThat(this).isEqualTo("5559685052:1:")
        }
        one.toNormalized("968-5052").run {
            assertThat(this).isEqualTo("0009685052:1:")
        }
        one.toNormalized("+1(555)-968-5052 x5555").run {
            assertThat(this).isEqualTo("5559685052:1:5555")
        }
        // MX phone number
        one.toNormalized("+52-65-8888-8888").run {
            assertThat(this).isEqualTo("6588888888:52:")
        }
    }

    @Test
    fun `test toFormatted phone`() {
        val one = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
        one.toFormatted("5559938322:1:").run {
            assertThat(this).isEqualTo("5559938322")
        }
        one.toFormatted("5559938322:1:", "\$country-\$area-\$exchange-\$subscriber").run {
            assertThat(this).isEqualTo("1-555-993-8322")
        }
        one.toFormatted("5559938322:1:", "(\$area)\$exchange-\$subscriber").run {
            assertThat(this).isEqualTo("(555)993-8322")
        }
    }

    @Test
    fun `test toFormatted zip`() {
        val one = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        one.toFormatted("99999").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("99999-9999").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        one.toFormatted("99999-9999", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        one.toFormatted("99999-9999", "\$zipFive").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("999999999", "\$zipFive").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("999999999", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        one.toFormatted("99999", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("KY1-5555", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("KY1-5555")
        }
        one.toFormatted("XZ5555", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("XZ5555")
        }
    }

    @Test
    fun `test inherit from`() {
        val parent = Element(
            "parent",
            type = Element.Type.TEXT,
            csvFields = Element.csvFields("sampleField"),
            documentation = "I am the parent element"
        )
        // instead of using var, just call run and transmogrify the child then and there
        val child = Element("child").run { inheritFrom(parent) }
        assertThat(
            parent.documentation,
            name = "Documentation value didn't carry over from parent"
        ).isEqualTo(child.documentation)
        assertThat(
            parent.type,
            name = "Element type didn't carry over from parent"
        ).isEqualTo(child.type)
    }

    @Test
    fun `test normalize and formatted round-trips`() {
        val postal = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        assertThat(
            postal.toNormalized("94040")
        ).isEqualTo(
            postal.toFormatted("94040")
        )
        assertThat(
            postal.toNormalized("94040-6000")
        ).isEqualTo(
            postal.toFormatted("94040-6000")
        )
        assertThat(
            postal.toFormatted(
                postal.toNormalized("94040-3600", Element.zipFiveToken), Element.zipFiveToken
            )
        ).isEqualTo(
            "94040"
        )
        assertThat(
            postal.toFormatted(
                postal.toNormalized("94040-3600", Element.zipFivePlusFourToken),
                Element.zipFivePlusFourToken
            )
        ).isEqualTo(
            "94040-3600"
        )

        val telephone = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
        assertThat(
            telephone.toFormatted(telephone.toNormalized("6509999999"))
        ).isEqualTo(
            "6509999999"
        )
        assertThat(
            telephone.toFormatted(telephone.toNormalized("+16509999999"))
        ).isEqualTo(
            "6509999999"
        )

        val date = Element(
            "a",
            type = Element.Type.DATE,
            csvFields = Element.csvFields("date")
        )
        assertThat(
            date.toFormatted(date.toNormalized("20201220"))
        ).isEqualTo(
            "20201220"
        )
        assertThat(
            date.toFormatted(date.toNormalized("2020-12-20"))
        ).isEqualTo(
            "20201220"
        )

        val datetime = Element(
            "a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("datetime")
        )
        assertThat(
            datetime.toFormatted(datetime.toNormalized("202012200000+0000"))
        ).isEqualTo(
            "202012200000+0000"
        )
        assertThat(
            datetime.toFormatted(datetime.toNormalized("2020-12-20T00:00Z"))
        ).isEqualTo(
            "202012200000+0000"
        )

        val hd = Element(
            "a",
            type = Element.Type.HD,
            csvFields = Element.csvFields("hd")
        )
        assertThat(
            hd.toFormatted(hd.toNormalized("HDName"))
        ).isEqualTo(
            "HDName"
        )
        assertThat(postal.toFormatted(hd.toNormalized("HDName^0.0.0.0.0.1^ISO"))).isEqualTo(
            "HDName^0.0.0.0.0.1^ISO"
        )

        val ei = Element(
            "a",
            type = Element.Type.EI,
            csvFields = Element.csvFields("ei")
        )
        assertThat(ei.toFormatted(ei.toNormalized("EIName")))
            .isEqualTo(
                "EIName"
            )
        assertThat(
            postal.toFormatted(
                ei.toNormalized("EIName^EINamespace^0.0.0.0.0.1^ISO")
            )
        )
            .isEqualTo(
                "EIName^EINamespace^0.0.0.0.0.1^ISO"
            )
    }

    @Test
    fun `test normalize with multiple csvFields`() {
        val sendingApp = Element(
            "a",
            type = Element.Type.HD,
            csvFields = listOf(
                Element.CsvField("sending_app", format = "\$name"),
                Element.CsvField("sending_oid", format = "\$universalId")
            )
        )
        val normalized = sendingApp.toNormalized(
            listOf(
                Element.SubValue("sending_app", "happy", "\$name"),
                Element.SubValue("sending_oid", "0.0.0.011", "\$universalId")
            )
        )
        assertThat("happy^0.0.0.011^ISO").isEqualTo(normalized)

        val sendingAppName = sendingApp.toFormatted(normalized, Element.hdNameToken)
        assertThat(sendingAppName).isEqualTo("happy")
        val sendingOid = sendingApp.toFormatted(normalized, Element.hdUniversalIdToken)
        assertThat(sendingOid).isEqualTo("0.0.0.011")
    }

    @Test
    fun `test code format field`() {
        val values = ValueSet(
            "test",
            system = ValueSet.SetSystem.LOCAL,
            values = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "U", display = "Unk")
            )
        )
        val one = Element(
            "a",
            valueSet = "test",
            valueSetRef = values,
            type = Element.Type.CODE,
            altValues = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "U", display = "Unknown")
            )
        )
        // because something is a
        assertThat(one.toFormatted("Y", "\$code")).isEqualTo("Y")
        assertThat(one.toNormalized("Y", "\$code")).isEqualTo("Y")
        assertThat(one.checkForError("Y", "\$code")).isNull()
    }

    @Test
    fun `test truncate`() {
        Element(
            name = "uno",
            type = Element.Type.TEXT,
            maxLength = 2,
        ).run {
            assertThat(this.truncateIfNeeded("abcde")).isEqualTo("ab")
        }
        Element(
            name = "dos",
            type = Element.Type.ID_CLIA, // this type is never truncated.
            maxLength = 2,
        ).run {
            assertThat(this.truncateIfNeeded("abcde")).isEqualTo("abcde")
        }
        Element(
            name = "tres",
            type = Element.Type.TEXT,
            maxLength = 20, // max > actual strlen, nothing to truncate
        ).run {
            assertThat(this.truncateIfNeeded("abcde")).isEqualTo("abcde")
        }
        Element( // zilch is an ok valuer = Element(  // maxLength is null, don't truncate.
            name = "cuatro",
            type = Element.Type.TEXT,
        ).run {
            assertThat(this.truncateIfNeeded("abcde")).isEqualTo("abcde")
        }
        Element(
            name = "cinco",
            type = Element.Type.TEXT,
            maxLength = 0, // zilch is an ok value
        ).run {
            assertThat(this.truncateIfNeeded("abcde")).isEqualTo("")
        }
    }

    @Test
    fun `test toFormatted values for UNK`() {
        val values = ValueSet(
            "hl70136",
            system = ValueSet.SetSystem.HL7,
            values = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "UNK", display = "Unk")
            )
        )
        val element = Element(
            "a",
            valueSet = "test",
            valueSetRef = values,
            type = Element.Type.CODE,
            altValues = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "UNK", display = "Unknown")
            )
        )
        element.toFormatted("UNK", "\$system").run {
            assertThat(this).isEqualTo("NULLFL")
        }
    }

    @Test
    fun `test processing of raw data`() {
        val elements = listOf(
            Element("a", Element.Type.TEXT),
            Element("b", Element.Type.TEXT, default = "someDefault"),
            Element(
                "c", Element.Type.TEXT, mapper = "concat(a,e)", mapperRef = ConcatenateMapper(),
                mapperArgs = listOf("a", "e"), default = "someDefault"
            ),
            Element(
                "d", Element.Type.TEXT, mapper = "concat(a,e)", mapperRef = ConcatenateMapper(),
                mapperArgs = listOf("a", "e"), default = "someDefault"
            ),
            Element(
                "e", Element.Type.TEXT, mapper = "concat(a,e)", mapperRef = ConcatenateMapper(),
                mapperArgs = listOf("a", "e"), mapperOverridesValue = true, default = "someDefault"
            ),
            Element(
                "f", Element.Type.TEXT, mapper = "concat(a,e,\$index)", mapperRef = ConcatenateMapper(),
                mapperArgs = listOf("a", "e", "\$index"), mapperOverridesValue = true, default = "someDefault",
                delimiter = "-"
            ),
            Element(
                "g", Element.Type.TEXT, mapper = "concat(a,e,\$currentDate)", mapperRef = ConcatenateMapper(),
                mapperArgs = listOf("a", "e", "\$currentDate"), mapperOverridesValue = true, default = "someDefault",
                delimiter = "-"
            )
        )
        val schema = Schema("one", "covid-19", elements)
        val currentDate = LocalDate.now().format(Element.dateFormatter)
        val mappedValues = mutableMapOf(
            elements[0].name to "TEST",
            elements[1].name to "",
            elements[2].name to "",
            elements[3].name to "TEST3",
            elements[4].name to "TEST4",
            elements[5].name to "TEST-TEST4-1",
            elements[6].name to "TEST-TEST4-$currentDate"
        )

        // Element has value and mapperAlwaysRun is false, so we get the raw value
        var finalValue = elements[0].processValue(mappedValues, schema)
        assertThat(finalValue).isEqualTo(mappedValues[elements[0].name])

        // Element with no raw value, no mapper and default returns a default.
        finalValue = elements[1].processValue(mappedValues, schema)
        assertThat(finalValue).isEqualTo(elements[1].default)

        // Element with mapper and no raw value returns mapper value
        finalValue = elements[2].processValue(mappedValues, schema)
        assertThat(finalValue).isEqualTo("${mappedValues[elements[0].name]}, ${mappedValues[elements[4].name]}")

        // Element with raw value and mapperAlwaysRun to false returns raw value
        finalValue = elements[3].processValue(mappedValues, schema)
        assertThat(finalValue).isEqualTo(mappedValues[elements[3].name])

        // Element with raw value and mapperAlwaysRun to true returns mapper value
        finalValue = elements[4].processValue(mappedValues, schema)
        assertThat(finalValue).isEqualTo("${mappedValues[elements[0].name]}, ${mappedValues[elements[4].name]}")

        // Element with $index
        finalValue = elements[5].processValue(mappedValues, schema, emptyMap(), 1)
        assertThat(finalValue).isEqualTo("${mappedValues[elements[5].name]}")

        // Element with $currentDate
        finalValue = elements[6].processValue(mappedValues, schema)
        assertThat(finalValue).isEqualTo("${mappedValues[elements[6].name]}")
    }

    @Test
    fun `test use mapper check`() {
        val elementA = Element("a")
        val elementB = Element("b", mapper = "concat(a,b)", mapperRef = ConcatenateMapper())
        val elementC = Element(
            "b", mapper = "concat(a,b)", mapperRef = ConcatenateMapper(),
            mapperOverridesValue = true
        )

        assertThat(elementA.useMapper("")).isFalse()
        assertThat(elementA.useMapper("dummyValue")).isFalse()

        assertThat(elementB.useMapper("")).isTrue()
        assertThat(elementB.useMapper("dummyValue")).isFalse()

        assertThat(elementC.useMapper("")).isTrue()
        assertThat(elementC.useMapper("dummyValue")).isTrue()
    }

    @Test
    fun `test use default check`() {
        val elementA = Element("a")

        assertThat(elementA.useDefault("")).isTrue()
        assertThat(elementA.useDefault("dummyValue")).isFalse()
    }

    @Test
    fun `test tokenized value mapping`() {
        val elementNameIndex = "\$index"
        val elementNameCurrentDate = "\$currentDate"

        val mockElement = Element("mock")
        val elementAndValueIndex = mockElement.tokenizeMapperValue(elementNameIndex, 3)
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val currentDate = LocalDate.now().format(formatter)
        val elementAndValueCurrentDate = mockElement.tokenizeMapperValue(elementNameCurrentDate)

        assertThat(elementAndValueIndex?.value).isEqualTo("3")
        assertThat(elementAndValueCurrentDate?.value).isEqualTo(currentDate)
    }
}