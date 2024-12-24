package gov.cdc.prime.router.cli.helpers

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.model.Varies
import ca.uhn.hl7v2.model.v27.datatype.ID
import ca.uhn.hl7v2.model.v27.datatype.NM
import ca.uhn.hl7v2.model.v27.datatype.ST
import fhirengine.translation.hl7.structures.fhirinventory.message.ORU_R01
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import kotlin.test.Test

class HL7DiffHelperTests {
    private val hl7DiffHelper = HL7DiffHelper()
    private val originalMessage = "MSH|^~\\&#|STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|CDC Atlanta2^" +
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
    private val msgMSH8OBR49Blank =
        """MSH|^~\&#|ProPhase^2.16.840.1.114222.4.1.238646^ISO|ProPhase^33D2215033^CLIA|CDC Prime^2.16.840.1.114222.4.1.237821^ISO|CDC Prime^2.16.840.1.114222.4.1.237821^ISO|20230816123358-0500| |ORU^R01^ORU_R01|20230816123358|P|2.5.1|||||||||PHLabReport-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO|
SFT|Orchard|9.0|Orchard Enterprise|9.0.211217.220208||20220411
PID|1||0008115-23-02^^^PROPHASE DIAGNOSTICS&2.16.840.1.114222.4.1.238646&ISO^PI^PROPHASE DIAGNOSTICS&2.16.840.1.114222.4.1.238646&ISO||Test^Male||19701031|M||2131-1^Other Race^HL70005|1234 Adams place^^New York^NY^10457||^^^noemail@prophasedx.com^^^|||||||||U^Unknown^HL70189^^^^2.5.1||||||||N||||||
ORC|RE||232270000212^ProPhase Diagnostics^2.16.840.1.114222.4.1.238646^ISO|^^^|||||20230815164300-0500|1234567890^Chriscoe^Matthew||1528068368^Israel^Rosa^^^^^^&2.16.840.1.113883.19.4.6&ISO^^^^NPI^^^^^^^^MD|^^^ProPhase Labs NY Clinical&CL||20230815164300-0500||||||ProPhase Diagnostics|711 Stewart Ave Ste 200^^Garden City^NY^11530|^^PH^^1^866^7522837|711 Stewart Ave Ste 200^^Garden City^NY^11530
OBR|1||232270000212^ProPhase Diagnostics^2.16.840.1.114222.4.1.238646^ISO|55454-3^Hemoglobin A1C ^LN|||20230815164300-0500|||1234567890^Chriscoe^Matthew||||||1528068368^Israel^Rosa^^^^^^&2.16.840.1.113883.19.4.6&ISO^^^^NPI^^^^^^^^MD||||||20230815164519-0500|||F|||||||||&Chriscoe&Matthew|||||||||||||^^^^^^^^|||||||||||||"""
    private val msgMSH8OBR49Empty =
        """MSH|^~\&#|ProPhase^2.16.840.1.114222.4.1.238646^ISO|ProPhase^33D2215033^CLIA|CDC Prime^2.16.840.1.114222.4.1.237821^ISO|CDC Prime^2.16.840.1.114222.4.1.237821^ISO|20230816123358-0500||ORU^R01^ORU_R01|20230816123358|P|2.5.1|||||||||PHLabReport-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO|
SFT|Orchard|9.0|Orchard Enterprise|9.0.211217.220208||20220411
PID|1||0008115-23-02^^^PROPHASE DIAGNOSTICS&2.16.840.1.114222.4.1.238646&ISO^PI^PROPHASE DIAGNOSTICS&2.16.840.1.114222.4.1.238646&ISO||Test^Male||19701031|M||2131-1^Other Race^HL70005|1234 Adams place^^New York^NY^10457||^^^noemail@prophasedx.com^^^|||||||||U^Unknown^HL70189^^^^2.5.1||||||||N||||||
ORC|RE||232270000212^ProPhase Diagnostics^2.16.840.1.114222.4.1.238646^ISO|^^^|||||20230815164300-0500|1234567890^Chriscoe^Matthew||1528068368^Israel^Rosa^^^^^^&2.16.840.1.113883.19.4.6&ISO^^^^NPI^^^^^^^^MD|^^^ProPhase Labs NY Clinical&CL||20230815164300-0500||||||ProPhase Diagnostics|711 Stewart Ave Ste 200^^Garden City^NY^11530|^^PH^^1^866^7522837|711 Stewart Ave Ste 200^^Garden City^NY^11530
OBR|1||232270000212^ProPhase Diagnostics^2.16.840.1.114222.4.1.238646^ISO|55454-3^Hemoglobin A1C ^LN|||20230815164300-0500|||1234567890^Chriscoe^Matthew||||||1528068368^Israel^Rosa^^^^^^&2.16.840.1.113883.19.4.6&ISO^^^^NPI^^^^^^^^MD||||||20230815164519-0500|||F|||||||||&Chriscoe&Matthew||||||||||||||||||||||||||"""

    @Test
    fun `diff hl7`() {
        val inputMessage = HL7Reader.parseHL7Message(originalMessage, null)
        val outputMessage = HL7Reader.parseHL7Message(comparisonMessage, null)
        val differences = hl7DiffHelper.diffHl7(inputMessage, outputMessage)
        assertThat(differences.size).isEqualTo(15)
        val differences2 = hl7DiffHelper.diffHl7(outputMessage, inputMessage)
        assertThat(differences2.size).isEqualTo(15)
    }

    @Test
    fun `test index structure`() {
        val outputMessage = HL7Reader.parseHL7Message(comparisonMessage, null)
        val outputNames = outputMessage.names
        val outputMap: MutableMap<String, Segment> = mutableMapOf()

        hl7DiffHelper.filterNames(outputMessage, outputNames, outputMap)

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
        val inputMessage = HL7Reader.parseHL7Message(originalMessage, null)
        val outputMessage = HL7Reader.parseHL7Message(comparisonMessage, null)
        val inputVal = ST(inputMessage)
        inputVal.value = "blah"
        val outputVal = ST(outputMessage)
        outputVal.value = "blah"

        val samePrimitive = hl7DiffHelper.compareHl7Type(
            "",
            inputVal,
            outputVal,
            "",
            0,
            0,
            0
        )

        assertThat(samePrimitive).isEmpty()

        outputVal.value = "test"
        val differentPrimitive = hl7DiffHelper.compareHl7Type(
            "",
            inputVal,
            outputVal,
            "",
            0,
            0,
            0
        )

        assertThat(differentPrimitive).isNotEmpty()
    }

    @Test
    fun `test compareHl7Type varies`() {
        val inputMessage = HL7Reader.parseHL7Message(originalMessage, null)
        val outputMessage = HL7Reader.parseHL7Message(comparisonMessage, null)
        val inputType = ST(inputMessage)
        inputType.value = "blah"
        val outputType = ST(outputMessage)
        outputType.value = "blah"

        val inputVal = Varies(inputMessage)
        inputVal.data = inputType
        val outputVal = Varies(outputMessage)
        outputVal.data = outputType

        val sameVaries = hl7DiffHelper.compareHl7Type(
            "",
            inputVal,
            outputVal,
            "",
            0,
            0,
            0
        )

        assertThat(sameVaries).isEmpty()

        outputType.value = "test"
        val differentVaries = hl7DiffHelper.compareHl7Type(
            "",
            inputVal,
            outputVal,
            "",
            0,
            0,
            0
        )

        assertThat(differentVaries).isNotEmpty()
    }

    @Test
    fun `test compareHl7Type composite`() {
        val inputMessage = HL7Reader.parseHL7Message(originalMessage, null)
        val id = ID(inputMessage)
        id.value = "blah"
        val nm = NM(inputMessage)
        nm.value = "blah2"
        val sameComposite = hl7DiffHelper.compareHl7Type(
            "",
            (inputMessage as ORU_R01).msh.getField(4)[0],
            inputMessage.msh.getField(4)[0],
            "",
            0,
            0,
            0
        )
        assertThat(sameComposite).isEmpty()

        val outputMessage = HL7Reader.parseHL7Message(comparisonMessage, null)
        val differentComposite = hl7DiffHelper.compareHl7Type(
            "",
            inputMessage.msh.getField(4)[0],
            (outputMessage as ORU_R01).msh.getField(4)[0],
            "",
            0,
            0,
            0
        )
        assertThat(differentComposite).isNotEmpty()
    }

    @Test
    fun `test compareHl7Type different types`() {
        val inputMessage = HL7Reader.parseHL7Message(originalMessage, null)
        val outputMessage = HL7Reader.parseHL7Message(comparisonMessage, null)
        val inputType = ST(inputMessage)
        inputType.value = "blah"
        val outputType = ST(outputMessage)
        outputType.value = "blah"

        val inputVal = Varies(inputMessage)
        inputVal.data = inputType

        val differentVaries = hl7DiffHelper.compareHl7Type(
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

    @Test
    fun `expect no diff messages have blank vs empty MSH 8 (ST), OBR 49 (CWE) respectively`() {
        val inputMessage = HL7Reader.parseHL7Message(msgMSH8OBR49Blank, null)
        val outputMessage = HL7Reader.parseHL7Message(msgMSH8OBR49Empty, null)
        val differences = hl7DiffHelper.diffHl7(inputMessage, outputMessage)
        assertThat(differences.size).isEqualTo(0)
        val differences2 = hl7DiffHelper.diffHl7(outputMessage, inputMessage)
        assertThat(differences2.size).isEqualTo(0)
    }

    @Test
    fun `diff output, input missing segments`() {
        val msg = originalMessage.split("\n").toMutableList()
        msg.removeAt(1)
        val inputMessage = HL7Reader.parseHL7Message(msg.joinToString("\n"), null)
        val outputMessage = HL7Reader.parseHL7Message(originalMessage, null)
        val differences = hl7DiffHelper.diffHl7(inputMessage, outputMessage)
        // input missing seg SFT
        assertThat(differences.size).isEqualTo(1)
        assertThat(differences[0].toString().contains("Input missing segment SFT"))
        val differences2 = hl7DiffHelper.diffHl7(outputMessage, inputMessage)
        // output missing seg SFT
        assertThat(differences2.size).isEqualTo(1)
        assertThat(differences[0].toString().contains("Output missing segment SFT"))
    }
}