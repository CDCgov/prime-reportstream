package gov.cdc.prime.router.cli.helpers

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.model.Varies
import ca.uhn.hl7v2.model.v251.datatype.ST
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import kotlin.test.Test

class HL7DiffHelperTests {
    private val hL7DiffHelper = HL7DiffHelper()
    private val originalMessage = "MSH|^~\\&#|STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|CDC Atlanta^" +
        "11D0668319^CLIA|MEDSS-ELR ^2.16.840.1.114222.4.3.3.6.2.1^ISO|MNDOH^2.16.840.1.114222.4.1.3661^ISO|" +
        "20230501102531-0400||ORU^R01^ORU_R01|3003786103_4988249_33033|T|2.5.1|||NE|NE|USA||||" +
        "PHLabReport-NoAck^PHIN^2.16.840.1.113883.9.11^ISO\n" +
        "SFT|CDC^^^^^CDC&2.16.840.1.114222.4&ISO^XX^^^CDC CLIA|ELIMS V11|STARLIMS|Binary ID unknown\n" +
        "PID|1||PID03953346^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^PI~10171284^^^SPHL-000034" +
        "&2.16.840.1.114222.4.1.3661&ISO^PI||~^^^^^^U||0000||||^^^^^USA^H\n" +
        "NTE|1|L|SPHL Submitter: MN PHL Division, Minnesota Department of Health, Submitter ID: SPHL-000034, Address:" +
        " 601 Robert St. N.  St. Paul, Minnesota 55164-0899 United States, Email: Health.idlabreports@state.mn.us, " +
        "Submitter Patient ID: 10171284, Submitter Alt Patient ID: , Submitter Specimen ID: 230011927, Submitter " +
        "Alt Specimen ID:|RE^Remark^HL70364^^^^2.5.1^^^^^^^2.16.840.1.113883.12.364\n" +
        "ORC|RE|230011927^SPHL-000034^2.16.840.1.114222.4.1.3661^ISO|40_3003786103_4988249_1087^STARLIMS.CDC.Stag^" +
        "2.16.840.1.114222.4.3.3.2.1.2^ISO|||||||||SPHL-000034^MN PHL Division, Minnesota Department of Health" +
        "^^^^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^^^^XX||^NET^Internet^" +
        "Health.idlabreports@state.mn.us|||||||MN PHL Division, Minnesota Department of Health^D^^^^" +
        "STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^XX^^^SPHL-000034|601 Robert St. N.^^St. Paul^" +
        "MN^55164-0899^USA^M|^WPN^Internet^Health.idlabreports@state.mn.us|601 Robert St. N.^^St. Paul^" +
        "MN^55164-0899^USA^M\n" +
        "OBR|1|230011927^SPHL-000034^2.16.840.1.114222.4.1.3661^ISO|40_3003786103_4988249_1087^STARLIMS." +
        "CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|PLT1228^Mold and Yeast XXX MS.MALDI-TOF^PLT^1087^" +
        "MALDI-TOF-CLIA^L^2.69^v unknown^^CDC-10179^Fungal Identification^L^^2.16.840.1.113883.6.1|||" +
        "20230322|||||||||SPHL-000034^MN PHL Division, Minnesota Department of Health^^^^^^^STARLIMS." +
        "CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^^^^XX|^NET^Internet^Health.idlabreports@state.mn.us" +
        "|||||202304271044-0400|||F\n" +
        "NTE|1|L|SPHL Submitter: MN PHL Division, Minnesota Department of Health, Submitter ID: SPHL-000034," +
        " Address: 601 Robert St. N.  St. Paul, Minnesota 55164-0899 United States, " +
        "Email: Health.idlabreports@state.mn.us, Submitter Patient ID: 10171284, " +
        "Submitter Alt Patient ID: , Submitter Specimen ID: 230011927, Submitter Alt Specimen ID:|" +
        "RE^Remark^HL70364^^^^2.5.1^^^^^^^2.16.840.1.113883.12.364\n" +
        "OBX|1|CWE|PLT1228^Mold and Yeast XXX MS.MALDI-TOF^PLT^3562^MALDI-TOF-CLIA^L^2.69^v_unknown^" +
        "MALDI-TOF-CLIA|N8KHKA9H-1|712760003^Candida metapsilosis (organism)^SCT^^^^09012018^^" +
        "Candida metapsilosis||||||F|||20230322|11D0668319^Centers for Disease Control and Prevention^" +
        "CLIA^40^Fungus Reference Laboratory^L|HVR0@cdc.gov^Gade^Lalitha|||20230427092900||||" +
        "Centers for Disease Control and Prevention^L^^^^CLIA&2.16.840.1.113883.4.7&ISO^XX^^^11D0668319|" +
        "1600 Clifton Rd^^Atlanta^GA^30329^USA^B\n" +
        "SPM|1|230011927&SPHL-000034&2.16.840.1.114222.4.1.3661&ISO^3003786103&STARLIMS.CDC.Stag" +
        "&2.16.840.1.114222.4.3.3.2.1.2&ISO||119365002^Specimen from wound^SCT^WND^Wound^L^0912017" +
        "^Adobe_Code^Wound||||56459004^Foot^SCT^FOT^Foot^L^09012017^Adobe_Code^Foot||||||Isolate,|||" +
        "20230322|20230421124150\n"
    private val comparisonMessage = "MSH|^~\\&#|STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|CDC Atlanta^" +
        "11D0668319^CLIA|MEDSS-ELR ^2.16.840.1.114222.4.3.3.6.2.1^ISO|MNDOH^2.16.840.1.114222.4.1.3661^ISO|" +
        "20230501102531-0400||ORU^R01^ORU_R01|3003786103_4988249_33033|T|2.5.1|||NE|NE|USA|UNICODE UTF-8|||" +
        "PHLabReport-NoAck^PHIN^2.16.840.1.113883.9.11^ISO\n" +
        "SFT|CDC^^^^^CDC&2.16.840.1.114222.4&ISO^XX^^^CDC CLIA|ELIMS V11|STARLIMS|Binary ID unknown\n" +
        "PID|1||PID03953346^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^PI~10171284^^^SPHL-" +
        "000034&2.16.840.1.114222.4.1.3661&ISO^PI||||||||^^^^^USA^H\n" +
        "NTE|1|L|Note 1 Submitter: MN PHL Division, Minnesota Department of Health, Submitter ID: SPHL-000034," +
        " Address: 601 Robert St. N.  St. Paul, Minnesota 55164-0899 United States, Email: " +
        "Health.idlabreports@state.mn.us, Submitter Patient ID: 10171284, Submitter Alt Patient ID: , " +
        "Submitter Specimen ID: 230011927, Submitter Alt Specimen ID:|RE^Remark^HL70364\n" +
        "ORC|RE|230011927^SPHL-000034^2.16.840.1.114222.4.1.3661^ISO|40_3003786103_4988249_1087^" +
        "STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|||||||||SPHL-000034^MN PHL Division, " +
        "Minnesota Department of Health^^^^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^^^^XX" +
        "||^NET^Internet^Health.idlabreports@state.mn.us|||||||MN PHL Division, Minnesota Department of Health" +
        "^D^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^XX^^^SPHL-000034|601 Robert St. N.^^St. Paul" +
        "^MN^55164-0899^USA^M|^NET^Internet^Health.idlabreports@state.mn.us|601 Robert St. N.^^St. Paul^" +
        "MN^55164-0899^USA^M\n" +
        "OBR|1|230011927^SPHL-000034^2.16.840.1.114222.4.1.3661^ISO|40_3003786103_4988249_1087^" +
        "STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|PLT1228^Mold and Yeast XXX MS.MALDI-TOF^" +
        "PLT^1087^MALDI-TOF-CLIA^L|||20230322|||||||||SPHL-000034^MN PHL Division, " +
        "Minnesota Department of Health^^^^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^^^^XX|" +
        "^NET^Internet^Health.idlabreports@state.mn.us|||||20230427104400-0400|||F\n" +
        "NTE|1|L|SPHL SubmitterTwo: MN PHL Division, Minnesota Department of Health, Submitter ID: SPHL-000034," +
        " Address: 601 Robert St. N.  St. Paul, Minnesota 55164-0899 United States," +
        " Email: Health.idlabreports@state.mn.us, Submitter Patient ID: 10171284, Submitter Alt Patient ID: , " +
        "Submitter Specimen ID: 230011927, Submitter Alt Specimen ID:|RE^Remark^HL70364\n" +
        "OBX|1|CWE|PLT1228^Mold and Yeast XXX MS.MALDI-TOF^PLT^3562^MALDI-TOF-CLIA^L|N8KHKA9H-1|" +
        "712760003^Candida metapsilosis (organism)^SCT^^^^09012018^^Candida metapsilosis||||||F|||" +
        "20230322|11D0668319^Centers for Disease Control and Prevention^CLIA^40^Fungus Reference Laboratory" +
        "^L|HVR0@cdc.gov^Gade^Lalitha|||20230427092900+0000||||Centers for Disease Control and Prevention^L" +
        "^^^^CLIA&2.16.840.1.113883.4.7&ISO^XX^^^11D0668319|1600 Clifton Rd^^Atlanta^GA^30329^USA^B\n" +
        "SPM|1|230011927&SPHL-000034&2.16.840.1.114222.4.1.3661&ISO^3003786103&STARLIMS.CDC.Stag&" +
        "2.16.840.1.114222.4.3.3.2.1.2&ISO||119365002^Specimen from wound^SCT^WND^Wound^L^0912017^Adobe_Code" +
        "^Wound||||56459004^Foot^SCT^FOT^Foot^L^09012017^Adobe_Code^Foot||||||Isolate,|||20230322|20230421124150+0000\n"

    @Test
    fun `diff hl7`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)
        val inputMessage = hl7Reader.getMessages(originalMessage)
        val outputMessage = hl7Reader.getMessages(comparisonMessage)
        val differences = hL7DiffHelper.diffHl7(inputMessage[0], outputMessage[0])
        assertThat(differences.size).isEqualTo(14)
        val differences2 = hL7DiffHelper.diffHl7(outputMessage[0], inputMessage[0])
        assertThat(differences2.size).isEqualTo(14)
    }

    @Test
    fun `test index structure`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)
        val outputMessage = hl7Reader.getMessages(comparisonMessage)
        val outputNames = outputMessage[0].names
        val outputMap: MutableMap<String, Segment> = mutableMapOf()

        hL7DiffHelper.filterNames(outputMessage[0], outputNames, outputMap)

        assertThat(outputMap.size).isEqualTo(9)
        assertThat(
            outputMap["PATIENT_RESULT(1)-PATIENT(1)-NTE(1)"]!!.getField(3)[0].toString()
        ).contains("Note 1 Submitter")
        assertThat(
            outputMap["PATIENT_RESULT(1)-ORDER_OBSERVATION(1)-NTE(1)"]!!.getField(3)[0].toString()
        ).contains("SPHL SubmitterTwo")
    }

    @Test
    fun `test compareHl7Type primitive`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)
        val inputMessage = hl7Reader.getMessages(originalMessage)
        val outputMessage = hl7Reader.getMessages(comparisonMessage)
        val inputVal = ST(inputMessage[0])
        inputVal.value = "blah"
        val outputVal = ST(outputMessage[0])
        outputVal.value = "blah"

        val samePrimitive = hL7DiffHelper.compareHl7Type(
            "",
            inputVal,
            outputVal,
            "",
            0,
            0,
            0
        )

        assertThat(samePrimitive).isNull()

        outputVal.value = "test"
        val differentPrimitive = hL7DiffHelper.compareHl7Type(
            "",
            inputVal,
            outputVal,
            "",
            0,
            0,
            0
        )

        assertThat(differentPrimitive).isNotNull()
    }

    @Test
    fun `test compareHl7Type varies`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)
        val inputMessage = hl7Reader.getMessages(originalMessage)
        val outputMessage = hl7Reader.getMessages(comparisonMessage)
        val inputType = ST(inputMessage[0])
        inputType.value = "blah"
        val outputType = ST(outputMessage[0])
        outputType.value = "blah"

        val inputVal = Varies(inputMessage[0])
        inputVal.data = inputType
        val outputVal = Varies(outputMessage[0])
        outputVal.data = outputType

        val sameVaries = hL7DiffHelper.compareHl7Type(
            "",
            inputVal,
            outputVal,
            "",
            0,
            0,
            0
        )

        assertThat(sameVaries).isNull()

        outputType.value = "test"
        val differentVaries = hL7DiffHelper.compareHl7Type(
            "",
            inputVal,
            outputVal,
            "",
            0,
            0,
            0
        )

        assertThat(differentVaries).isNotNull()
    }

//    @Test
//    fun `test compareHl7Type composite`() {
//        val actionLogger = ActionLogger()
//        val hl7Reader = HL7Reader(actionLogger)
//        val inputMessage = hl7Reader.getMessages("MSH|^~\\&|STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|CDC Atlanta^11D0668319^CLIA|MEDSS-ELR ^2.16.840.1.114222.4.3.3.6.2.1^ISO|MNDOH^2.16.840.1.114222.4.1.3661^ISO|20230501102531-0400||ORU^R01^ORU_R01|3003786103_4988249_33033|T|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLabReport-NoAck^PHIN^2.16.840.1.113883.9.11^ISO\n" +
//            "TQ1|1||||||20280802025201-0600|20280808092805-0600|S\n" +
//            "OBX|1|ST|600-7^Bacteria identified in Blood by Culture^LN^BLOODC^BLOOD CULTURE^L|1.1|||||||C|||20280802025201-0600|RML^TMCA, SOUTH CAMPUS^LB||||20210810062500-0600||||TMCA, SOUTH CAMPUS^L^^^^TMCA, SOUTH CAMPUS&2.16.840.1.114222.4.1.144&ISO^XX^^^06D0055551|1501 S. POTOMAC^^AURORA^CO^80012^USA^B^^S|1740336429^KOTNIS^GREGORY^R^MD^DR.^^^RML^L^^^FI^TMCA, SOUTH CAMPUS")
//        val outputMessage = hl7Reader.getMessages("MSH|^~\\&|STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|CDC Atlanta^11D0668319^CLIA|MEDSS-ELR ^2.16.840.1.114222.4.3.3.6.2.1^ISO|MNDOH^2.16.840.1.114222.4.1.3661^ISO|20230501102531-0400||ORU^R01^ORU_R01|3003786103_4988249_33033|T|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLabReport-NoAck^PHIN^2.16.840.1.113883.9.11^ISO\n" +
//            "TQ1|1||||||20280802025201-0600|20280808092805-0600|S")
//
//        ORC()
//
//        val retVal: Array<TQ> = originalMessage.get(0)..getTypedField<TQ>(27, arrayOfNulls<TQ>(0))
//
//        val tryIt = Terser.get(outputMessage[0].get("TQ1") as Segment?, 1, 1, 1, 1) as TQ
//        val sameVaries = hL7DiffHelper.compareHl7Type(
//            "",
//            TQ(inputMessage[0]),
//            TQ(tryIt),
//            "",
//            0,
//            0,
//            0
//        )
//
//        assertThat(sameVaries).isNull()
//
// //        val outputMessage2 = hl7Reader.getMessages("MSH|^~\\&#|STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|CDC Atlanta^11D0668319^CLIA|MEDSS-ELR ^2.16.840.1.114222.4.3.3.6.2.1^ISO|MNDOH^2.16.840.1.114222.4.1.3661^ISO|20230501102531-0400||ORU^R01^ORU_R01|3003786103_4988249_33033|T|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLabReport-NoAck^PHIN^2.16.840.1.113883.9.11^ISO\n" +
// //            "OBX|3|CWE|57713123-0^INFANT FACTORS THAT AFFECT NEWBORN SCREENING INTERPRETATION^LN|1|LA12419-0^INFANT IN NICU AT TIME OF SPECIMEN COLLECTION^LN||||||O|||20230506050000-0500|||||||||||||||QST|AOE")
// //        val outputVal2 = CWE(outputMessage2[0])
// //        val differentVaries = hL7DiffHelper.compareHl7Type(
// //            "",
// //            inputVal,
// //            outputVal2,
// //            "",
// //            0,
// //            0,
// //            0
// //        )
// //
// //        assertThat(differentVaries).isNotNull()
//    }

    @Test
    fun `test compareHl7Type different types`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)
        val inputMessage = hl7Reader.getMessages(originalMessage)
        val outputMessage = hl7Reader.getMessages(comparisonMessage)
        val inputType = ST(inputMessage[0])
        inputType.value = "blah"
        val outputType = ST(outputMessage[0])
        outputType.value = "blah"

        val inputVal = Varies(inputMessage[0])
        inputVal.data = inputType

        val differentVaries = hL7DiffHelper.compareHl7Type(
            "",
            inputVal,
            outputType,
            "",
            0,
            0,
            0
        )

        assertThat(differentVaries).isNotNull()
    }
}