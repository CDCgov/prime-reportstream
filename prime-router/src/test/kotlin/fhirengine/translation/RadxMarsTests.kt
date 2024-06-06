package gov.cdc.prime.router.fhirengine.translation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory
import fhirengine.translation.hl7.structures.fhirinventory.message.ORU_R01
import gov.cdc.prime.router.fhirengine.engine.encodePreserveEncodingChars
import kotlin.test.Test

class RadxMarsTests {
    @Test
    fun `test radxmars java class`() {
        val rawMessage = """
MSH|^~\&|MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO|CAREEVOLUTION^00Z0000024^CLIA|AIMS.INTEGRATION.PRD^2.16.840.1.114222.4.3.15.1^ISO|AIMS.PLATFORM^2.16.840.1.114222.4.1.217446^ISO|20240404110305+0000||ORU^R01^ORU_R01|20240404110305_433848dc33ca4ce89ff4773fed48e8f6|P|2.5.1|||NE|NE|||||PHLabReport-NoAck^ELR251R1_Rcvr_Prof^2.16.840.1.113883.9.11^ISO
SFT|CAREEVOLUTION|2022|MMTC.PROD|16498||20240402
PID|1||f7aa0855e93244f19cb10feda529cc41^^^MMTC.PROD&2.16.840.1.113883.3.8589.4.2.106.1&ISO^PI||DeIdentified^DeIdentified^DeIdentified||19800101|M||2106-3^White^HL70005^^^^2.5.1|DeIdentified^^DeIdentified^VA^22043^USA||^^PH^^^555^1111234~^^Internet^DeIdentified|||||||||N^Not Hispanic or Latino^HL70189^^^^2.5.1
ORC|RE||^MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO||||||||||||||||||SA.OTCSelfReport|^^^VA^22043
OBR|1||^MMTC.PROD^2.16.840.1.113883.3.8589.4.2.106.1^ISO|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.71|||20240404120000-0400|||||||||||||||20240404120000-0400|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.71||260415000^Not detected^SCT^^^^20200901||||||F||||00Z0000042||BinaxNOW COVID-19 Antigen Self Test_Abbott Diagnostics Scarborough, Inc._EUA^^99ELR^^^^Vunknown||20240404120000||||SA.OTCSelfReport^^^^^&2.16.840.1.113883.3.8589.4.1.152&ISO^XX^^^00Z0000042
NTE|1||note
OBX|2|NM|35659-2^Age at specimen collection^LN^^^^2.71||39|a^year^UCUM^^^^2.1|||||F||||00Z0000042||||||||SA.OTCSelfReport^^^^^&2.16.840.1.113883.3.8589.4.1.152&ISO^XX^^^00Z0000042|4861&20TH AVE&1^Other Designation^THUNDER MOUNTAIN^IG^99999^USA^B^Other Geographic Designation^County^6059^A^^20220501102531-0400^20230501102531-0400^^^^^Adressee|1^BEETHOVEN&VAN&Medical Director&VAL&ROGER^LUDWIG^B^2ND^DR^MD^SRC^Namespace&AssigningSystem&UUID^B^A^NPI^DL^^A^NameContext^^G^20220501102531-0400^20230501102531-0400^MD^AssignJ^AssignA|Release|1^Cause^LN|1^Local^LN|QST
SPM|1|^433848dc33ca4ce89ff4773fed48e8f6&MMTC.PROD&2.16.840.1.113883.3.8589.4.2.106.1&ISO||697989009^Anterior nares swab^SCT^^^^20200901|||||||||||||20240404120000-0400|20240404120000-0400    
    """.trimIndent()
        val messages: MutableList<Message> = mutableListOf()
        val validationContext = ValidationContextFactory.noValidation()
        val context = DefaultHapiContext(CanonicalModelClassFactory(ORU_R01::class.java))
        context.validationContext = validationContext

        val iterator = Hl7InputStreamMessageIterator(rawMessage.byteInputStream(), context)
        while (iterator.hasNext()) {
            messages.add(iterator.next())
        }

        assertThat(messages).isNotEmpty()
        assertThat(messages[0].encodePreserveEncodingChars()).isNotEmpty()
        assertThat(messages[0].encodePreserveEncodingChars().trimIndent()).isEqualTo(rawMessage)
    }
}