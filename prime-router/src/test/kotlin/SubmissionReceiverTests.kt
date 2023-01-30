package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.ReportWriter
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.fhirengine.engine.elrConvertQueueName
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.ReadResult
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.IllegalStateException
import kotlin.test.assertTrue

class SubmissionReceiverTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val timing1 = mockkClass(Receiver.Timing::class)

    val oneOrganization = DeepOrganization(
        "phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "phd",
                Topic.TEST,
                CustomerStatus.INACTIVE,
                "one",
                timing = timing1
            )
        )
    )

    val csvString_2Records = "senderId,processingModeCode,testOrdered,testName,testResult,testPerformed," +
        "testResultDate,testReportDate,deviceIdentifier,deviceName,specimenId,testId,patientAge,patientRace," +
        "patientEthnicity,patientSex,patientZip,patientCounty,orderingProviderNpi,orderingProviderLname," +
        "orderingProviderFname,orderingProviderZip,performingFacility,performingFacilityName," +
        "performingFacilityState,performingFacilityZip,specimenSource,patientUniqueId,patientUniqueIdHash," +
        "patientState,firstTest,previousTestType,previousTestResult,healthcareEmployee,symptomatic,symptomsList," +
        "hospitalized,symptomsIcu,congregateResident,congregateResidentType,pregnant\n" +
        "abbott,P,95209-3,SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay,419984006,95209-3,202112181841-0500,202112151325-0500,LumiraDx SARS-CoV-2 Ag Test_LumiraDx " +
        "UK Ltd.,LumiraDx SARS-CoV-2 Ag Test*,SomeEntityID,SomeEntityID,3,2131-1,2135-2,F,19931,Sussex,1404270765," +
        "Reichert,NormanA,19931,97D0667471,Any lab USA,DE,19931,122554006,esyuj9,vhd3cfvvt,DE,NO,bgq0b2e,840533007," +
        "NO,NO,h8jev96rc,YES,YES,YES,257628001,60001007\n" +
        "abbott,P,95209-3,SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay,419984006,95209-3,202112181841-0500,202112151325-0500,LumiraDx SARS-CoV-2 Ag Test_LumiraDx " +
        "UK Ltd.,LumiraDx SARS-CoV-2 Ag Test*,SomeEntityID,SomeEntityID,3,2131-1,2135-2,F,19931,Sussex,1404270765," +
        "Reicherts,NormanB,19931,97D0667471,Any lab USA,DE,19931,122554006,esyuj9,vhd3cfvvt,DE,NO,bgq0b2e," +
        "840533007,NO,NO,h8jev96rc,YES,YES,YES,257628001,60001007"
    val hl7_record = "MSH|^~\\&|CDC PRIME - Atlanta,^2.16.840.1.114222.4.1.237821^ISO|Winchester House^05D2222542^" +
        "ISO|CDPH FL REDIE^2.16.840.1.114222.4.3.3.10.1.1^ISO|CDPH_CID^2.16.840.1.114222.4.1.214104^ISO|202108031315" +
        "11.0147+0000||ORU^R01^ORU_R01|1234d1d1-95fe-462c-8ac6-46728dba581c|P|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLab" +
        "Report-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO\n" +
        "SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210726\n" +
        "PID|1||09d12345-0987-1234-1234-111b1ee0879f^^^Winchester House&05D2222542&ISO^PI^&05D2222542&ISO||Bunny^Bug" +
        "s^C^^^^L||19000101|M||2106-3^White^HL70005^^^^2.5.1|12345 Main St^^San Jose^FL^95125^USA^^^06085||(123)456-" +
        "7890^PRN^PH^^1^123^4567890|||||||||N^Non Hispanic or Latino^HL70189^^^^2.9||||||||N\n" +
        "ORC|RE|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728db" +
        "a581c^Winchester House^05D2222542^ISO|||||||||1679892871^Doolittle^Doctor^^^^^^CMS&2.16.840.1.113883.3.249&" +
        "ISO^^^^NPI||(123)456-7890^WPN^PH^^1^123^4567890|20210802||||||Winchester House|6789 Main St^^San Jose^FL^95" +
        "126^^^^06085|(123)456-7890^WPN^PH^^1^123^4567890|6789 Main St^^San Jose^FL^95126\n" +
        "OBR|1|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728dba" +
        "581c^Winchester House^05D2222542^ISO|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by" +
        " Rapid immunoassay^LN^^^^2.68|||202108020000-0500|202108020000-0500||||||||1679892871^Doolittle^Doctor^^^^" +
        "^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|(123)456-7890^WPN^PH^^1^123^4567890|||||202108020000-0500|||F\n" +
        "OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2." +
        "68||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078^^^^2.7|||F|||20210802000" +
        "0-0500|05D2222542^ISO||10811877011290_DIT^10811877011290^99ELR^^^^2.68^^10811877011290_DIT||20" +
        "2108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^" +
        "San Jose^FL^95126^^^^06085\n" +
        "OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||N^No^HL70136||||||F|||202" +
        "108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX" +
        "^^^05D2222542|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202108020000-0500" +
        "|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|" +
        "6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202108020000-05" +
        "00|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D22225" +
        "42|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||2021080" +
        "20000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^" +
        "05D2222542|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "SPM|1|1234d1d1-95fe-462c-8ac6-46728dba581c&&05D2222542&ISO^1234d1d1-95fe-462c-8ac6-46728dba581c&&05D22225" +
        "42&ISO||445297001^Swab of internal nose^SCT^^^^2.67||||53342003^Internal nose structure (body structure)^" +
        "SCT^^^^2020-09-01|||||||||202108020000-0500|20210802000006.0000-0500"
    val hl7_record_batch_headers = "FHS|^~\\&|||0.0.0.0.1|0.0.0.0.1|202106221314-0400\n" +
        "BHS|^~\\&|||0.0.0.0.1|0.0.0.0.1|202106221314-0400\n" +
        "MSH|^~\\&||Any facility USA^00D1063590^CLIA|0.0.0.0.1|0.0.0.0.1|20210622131413.8343-0400||ORU^R01^ORU_R01|" +
        "858625|P|2.5.1|||NE|NE|USA|UNICODE UTF-8\n" +
        "SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210622\n" +
        "PID|1||ty6vmz^^^Any lab USA&00D1063590&CLIA^sk6yx07d^&00D1063590&CLIA||Walker^Caleb^Elida^V^^^w9tdt9||" +
        "19280828|A||2106-3^White^HL70005^^^^2.5.1|1663 Simonis Loaf^^^IG^^2ljq2bmyq|" +
        "|^NET^Internet^gaylord.schumm@email.com~(235)8597464^PRN^PH^^1^235^8597464|||||||470-08-1020|" +
        "|N^Non Hispanic or Latino^HL70189^^^^2.9|||||||20210619|UNK|||||||||83\n" +
        "ORC|RE|094127^Any lab USA^91D6499987^CLIA|790928^Any lab USA^91D6499987^CLIA|" +
        "429385^zpa15khbo^23D3640684^CLIA||||||||9761354546^Hane^Merrill^Winston^^^^^0.0.0.0.1^^^^uyheq064u||" +
        "(203)9088367^WPN^PH^^1^203^9088367|20210613||||||Any facility USA|52005 Jaime Courts^^^IG^^^^CSV|" +
        "(226)4923361^WPN^PH^^1^226^4923361|495 Li Orchard^^^IG\n" +
        "OBR|1|094127^Any lab USA^91D6499987^CLIA|790928^Any lab USA^91D6499987^CLIA|" +
        "94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^e3lcoj^^^^2.68|||" +
        "202106130143-0400|202106130143-0400||||||||9761354546^Hane^Merrill^Winston^^^^^0.0.0.0.1^^^^uyheq064u|" +
        "(203)9088367^WPN^PH^^1^203^9088367|||||202106151804-0400|||F^Final results^HL70123||||||khi2a\n" +
        "OBX|1|CWE^^HL70125|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid" +
        " immunoassay^5380sz^^^^2.68|752053|260373001^Detected^SCT|eq1bvbvr|Normal|L^Below low normal^HL70078^^^^2.7|" +
        "||F^Final results; Can only be changed with a corrected result^HL70085|||202106130143-0400|91D6499987^CLIA||" +
        "^^^^^^2.68|BD Veritor Plus System_Becton Dickinson^^MNI|202106140503-0400|||202106192007-0400|" +
        "Any lab USA^^^^^0.0.0.0.1^XX^^^91D6499987^CLIA|84684 Hans River^^^IG\n" +
        "NTE||L^Ancillary (filler) department is source of comment^HL70105|0aswk4cws|" +
        "AI^Ancillary Instructions^HL70364^^^^2.5.1\n" +
        "OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||UNK^Unknown^HL70136|" +
        "|||||F|||202106130143-0400|91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|" +
        "84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||UNK^Unknown^HL70136||||||F|||" +
        "202106130143-0400|91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|" +
        "84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|4|CWE|77974-4^Patient was hospitalized because of this condition^LN||Y^Yes^HL70136||||||F|||" +
        "202106130143-0400|91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|" +
        "84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|5|CWE|95420-6^Admitted to intensive care unit for condition of interest^LN^^^^2.69||" +
        "UNK^Unknown^HL70136||||||F|||202106130143-0400|91D6499987||||202106140503-0400||||" +
        "Any lab USA^^^^^^XX^^^91D6499987|84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|6|DT|65222-2^Date and time of symptom onset^LN^^^^2.68||20210614||||||F|||202106130143-0400|" +
        "91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|7|CWE|30525-0^Age^LN^^^^2.68||3|mo^months^UCUM|||||F|||202106130143-0400|91D6499987||||" +
        "202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|8|CWE|82810-3^Pregnancy status^LN^^^^2.68||77386006^Pregnant^SCT||||||F|||202106130143-0400|" +
        "91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|9|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||" +
        "202106130143-0400|91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|" +
        "84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|10|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||" +
        "202106130143-0400|91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|" +
        "84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "SPM|1|428303&&00D1063590&CLIA^790928&&00D1063590&CLIA||258580003^Whole blood sample^SCT^^^^2.67|||" +
        "TMSC^Transport Media, Stool Culture^HL70048|71836000^Nasopharyngeal structure" +
        " (body structure)^SCT^^^^2020-09-01||i0rqgdf|R|||8lls9tlx|||202106130143-0400|202106132135-0400\n" +
        "BTS|25\n" +
        "FTS|1\n"
    val hl7_multiple_records_no_headers = "MSH|^~\\&||Any facility USA^00D1063590^CLIA|0.0.0.0.1|0.0.0.0.1|" +
        "20210622131413.8343-0400||ORU^R01^ORU_R01|858625|P|2.5.1|||NE|NE|USA|UNICODE UTF-8\n" +
        "SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210622\n" +
        "PID|1||ty6vmz^^^Any lab USA&00D1063590&CLIA^sk6yx07d^&00D1063590&CLIA||Walker^Caleb^Elida^V^^^w9tdt9||" +
        "19280828|A||2106-3^White^HL70005^^^^2.5.1|1663 Simonis Loaf^^^IG^^2ljq2bmyq||" +
        "^NET^Internet^gaylord.schumm@email.com~(235)8597464^PRN^PH^^1^235^8597464|||||||470-08-1020||" +
        "N^Non Hispanic or Latino^HL70189^^^^2.9|||||||20210619|UNK|||||||||83\n" +
        "ORC|RE|094127^Any lab USA^91D6499987^CLIA|790928^Any lab USA^91D6499987^CLIA|" +
        "429385^zpa15khbo^23D3640684^CLIA||||||||9761354546^Hane^Merrill^Winston^^^^^0.0.0.0.1^^^^uyheq064u||" +
        "(203)9088367^WPN^PH^^1^203^9088367|20210613||||||Any facility USA|52005 Jaime Courts^^^IG^^^^CSV|" +
        "(226)4923361^WPN^PH^^1^226^4923361|495 Li Orchard^^^IG\n" +
        "OBR|1|094127^Any lab USA^91D6499987^CLIA|790928^Any lab USA^91D6499987^CLIA|" +
        "94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^e3lcoj^^^^2.68|" +
        "||202106130143-0400|202106130143-0400||||||||9761354546^Hane^Merrill^Winston^^^^^0.0.0.0.1^^^^uyheq064u|" +
        "(203)9088367^WPN^PH^^1^203^9088367|||||202106151804-0400|||F^Final results^HL70123||||||khi2a\n" +
        "OBX|1|CWE^^HL70125|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay^5380sz^^^^2.68|752053|260373001^Detected^SCT|eq1bvbvr|Normal|L^Below low normal^HL70078^^^^2.7|" +
        "||F^Final results; Can only be changed with a corrected result^HL70085|||202106130143-0400|91D6499987^CLIA|" +
        "|^^^^^^2.68|BD Veritor Plus System_Becton Dickinson^^MNI|202106140503-0400|||202106192007-0400|" +
        "Any lab USA^^^^^0.0.0.0.1^XX^^^91D6499987^CLIA|84684 Hans River^^^IG\n" +
        "NTE||L^Ancillary (filler) department is source of comment^HL70105|0aswk4cws|" +
        "AI^Ancillary Instructions^HL70364^^^^2.5.1\n" +
        "OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||UNK^Unknown^HL70136|" +
        "|||||F|||202106130143-0400|91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|" +
        "84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||UNK^Unknown^HL70136||||||F|||" +
        "202106130143-0400|91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|" +
        "84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|4|CWE|77974-4^Patient was hospitalized because of this condition^LN||Y^Yes^HL70136||||||F|||" +
        "202106130143-0400|91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|" +
        "84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|5|CWE|95420-6^Admitted to intensive care unit for condition of interest^LN^^^^2.69||" +
        "UNK^Unknown^HL70136||||||F|||202106130143-0400|91D6499987||||202106140503-0400||||" +
        "Any lab USA^^^^^^XX^^^91D6499987|84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|6|DT|65222-2^Date and time of symptom onset^LN^^^^2.68||20210614||||||F|||202106130143-0400|91D6499987|" +
        "|||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|7|CWE|30525-0^Age^LN^^^^2.68||3|mo^months^UCUM|||||F|||202106130143-0400|91D6499987||||" +
        "202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|8|CWE|82810-3^Pregnancy status^LN^^^^2.68||77386006^Pregnant^SCT||||||F|||202106130143-0400|" +
        "91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|9|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||" +
        "202106130143-0400|91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|" +
        "84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "OBX|10|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||" +
        "202106130143-0400|91D6499987||||202106140503-0400||||Any lab USA^^^^^^XX^^^91D6499987|" +
        "84684 Hans River^^^IG^^^^^CSV|||||QST\n" +
        "SPM|1|428303&&00D1063590&CLIA^790928&&00D1063590&CLIA||258580003^Whole blood sample^SCT^^^^2.67|||" +
        "TMSC^Transport Media, Stool Culture^HL70048|71836000^Nasopharyngeal structure (body structure)^SCT^^^^" +
        "2020-09-01||i0rqgdf|R|||8lls9tlx|||202106130143-0400|202106132135-0400\n" +
        "MSH|^~\\&||Any facility USA^89D0179727^CLIA|0.0.0.0.1|0.0.0.0.1|20210622131413.8343-0400||ORU^R01^ORU_R01|" +
        "444647|P|2.5.1|||NE|NE|USA|UNICODE UTF-8\n" +
        "SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210622\n" +
        "PID|1||pym78h^^^Any lab USA&89D0179727&CLIA^7iwz8b7^&89D0179727&CLIA||Boyle^Milagro^America^^^^r8qat6||" +
        "19530809|U||2028-9^Asian^HL70005^^^^2.5.1|13833 Gerlach Pine^^^IG^^9zylw94i||" +
        "^NET^Internet^amparo.donnelly@email.com~(218)2671915^PRN^PH^^1^218^2671915|||||||612-20-4749||" +
        "U^Unknown^NULLFL^^^^2.9|||||||20210616|Y|||||||||73\n" +
        "ORC|RE|343803^Any lab USA^52D3802033^CLIA|801572^Any lab USA^52D3802033^CLIA|143237^fn9qu^36D3186071^CLIA|" +
        "|||||||6295335628^Kutch^Deidre^Gilbert^^^^^0.0.0.0.1^^^^73bjs5f||(210)2187892^WPN^PH^^1^210^2187892|" +
        "20210620||||||Any facility USA|858 Lowe Roads^^^IG^^^^HL7|(215)4870805^WPN^PH^^1^215^4870805|" +
        "8339 Fahey Isle^^^IG\n" +
        "OBR|1|343803^Any lab USA^52D3802033^CLIA|801572^Any lab USA^52D3802033^CLIA|" +
        "95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay^rofbw^^^^2.68|||202106191723-0400|202106191723-0400||||||||" +
        "6295335628^Kutch^Deidre^Gilbert^^^^^0.0.0.0.1^^^^73bjs5f|(210)2187892^WPN^PH^^1^210^2187892|||||" +
        "202106161517-0400|||F^Order received; specimen not yet received^HL70123||||||airqhmcn\n" +
        "OBX|1|CWE^^HL70125|95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay^og3lqsx6l^^^^2.68|321016|419984006^Inconclusive^SCT|o74lqd5po|Normal|>^Above absolute high-off" +
        " instrument scale^HL70078^^^^2.7|||F^Not asked; used to affirmatively document that the observation" +
        " identified in the OBX was not sought when the universal service ID in OBR-4 implies that it would be" +
        " sought.^HL70085|||202106191723-0400|52D3802033^CLIA||^^^^^^2.68|LumiraDx Platform_LumiraDx^^MNI|" +
        "202106210113-0400|||202106140838-0400|Any lab USA^^^^^0.0.0.0.1^XX^^^52D3802033^CLIA|" +
        "39111 Zemlak Route^^^IG\n" +
        "NTE||P^Orderer (placer) is source of comment^HL70105|x3fmq|RE^Remark^HL70364^^^^2.5.1\n" +
        "OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||UNK^Unknown^HL70136|" +
        "|||||F|||202106191723-0400|52D3802033||||202106210113-0400||||Any lab USA^^^^^^XX^^^52D3802033|" +
        "39111 Zemlak Route^^^IG^^^^^HL7|||||QST\n" +
        "OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||UNK^Unknown^HL70136||||||F|||" +
        "202106191723-0400|52D3802033||||202106210113-0400||||Any lab USA^^^^^^XX^^^52D3802033|" +
        "39111 Zemlak Route^^^IG^^^^^HL7|||||QST\n" +
        "OBX|4|CWE|77974-4^Patient was hospitalized because of this condition^LN||Y^Yes^HL70136||||||F|||" +
        "202106191723-0400|52D3802033||||202106210113-0400||||Any lab USA^^^^^^XX^^^52D3802033|" +
        "39111 Zemlak Route^^^IG^^^^^HL7|||||QST\n" +
        "OBX|5|CWE|95420-6^Admitted to intensive care unit for condition of interest^LN^^^^2.69||Y^Yes^HL70136|" +
        "|||||F|||202106191723-0400|52D3802033||||202106210113-0400||||Any lab USA^^^^^^XX^^^52D3802033|" +
        "39111 Zemlak Route^^^IG^^^^^HL7|||||QST\n" +
        "OBX|6|DT|65222-2^Date and time of symptom onset^LN^^^^2.68||20210614||||||F|||202106191723-0400|" +
        "52D3802033||||202106210113-0400||||Any lab USA^^^^^^XX^^^52D3802033|" +
        "39111 Zemlak Route^^^IG^^^^^HL7|||||QST\n" +
        "OBX|7|CWE|30525-0^Age^LN^^^^2.68||9|wk^weeks^UCUM|||||F|||202106191723-0400|52D3802033||||" +
        "202106210113-0400||||Any lab USA^^^^^^XX^^^52D3802033|39111 Zemlak Route^^^IG^^^^^HL7|||||QST\n" +
        "OBX|8|CWE|82810-3^Pregnancy status^LN^^^^2.68||261665006^Unknown^SCT||||||F|||" +
        "202106191723-0400|52D3802033||||202106210113-0400||||Any lab USA^^^^^^XX^^^52D3802033|" +
        "39111 Zemlak Route^^^IG^^^^^HL7|||||QST\n" +
        "OBX|9|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||" +
        "202106191723-0400|52D3802033||||202106210113-0400||||Any lab USA^^^^^^XX^^^52D3802033|" +
        "39111 Zemlak Route^^^IG^^^^^HL7|||||QST\n" +
        "OBX|10|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||Y^Yes^HL70136|" +
        "|||||F|||202106191723-0400|52D3802033||||202106210113-0400||||Any lab USA^^^^^^XX^^^52D3802033|" +
        "39111 Zemlak Route^^^IG^^^^^HL7|||||QST\n" +
        "SPM|1|716531&&89D0179727&CLIA^801572&&89D0179727&CLIA||258500001^Nasopharyngeal swab^SCT^^^^2.67|" +
        "||TMVI^Transport Media, Viral^HL70048|71836000^Nasopharyngeal structure " +
        "(body structure)^SCT^^^^2020-09-01||m074nakh|B|||pvnlh|||202106191723-0400|202106212137-0400"
    val hl7_record_bad_type = "MSH|^~\\&|CDC PRIME - Atlanta,^2.16.840.1.114222.4.1.237821^ISO|" +
        "Winchester House^05D2222542^" +
        "ISO|CDPH FL REDIE^2.16.840.1.114222.4.3.3.10.1.1^ISO|CDPH_CID^2.16.840.1.114222.4.1.214104^ISO|202108031315" +
        "11.0147+0000||ORU^R02^ORU_R02|1234d1d1-95fe-462c-8ac6-46728dba581c|P|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLab" +
        "Report-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO\n" +
        "SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210726\n" +
        "PID|1||09d12345-0987-1234-1234-111b1ee0879f^^^Winchester House&05D2222542&ISO^PI^&05D2222542&ISO||Bunny^Bug" +
        "s^C^^^^L||19000101|M||2106-3^White^HL70005^^^^2.5.1|12345 Main St^^San Jose^FL^95125^USA^^^06085||(123)456-" +
        "7890^PRN^PH^^1^123^4567890|||||||||N^Non Hispanic or Latino^HL70189^^^^2.9||||||||N\n" +
        "ORC|RE|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728db" +
        "a581c^Winchester House^05D2222542^ISO|||||||||1679892871^Doolittle^Doctor^^^^^^CMS&2.16.840.1.113883.3.249&" +
        "ISO^^^^NPI||(123)456-7890^WPN^PH^^1^123^4567890|20210802||||||Winchester House|6789 Main St^^San Jose^FL^95" +
        "126^^^^06085|(123)456-7890^WPN^PH^^1^123^4567890|6789 Main St^^San Jose^FL^95126\n" +
        "OBR|1|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728dba" +
        "581c^Winchester House^05D2222542^ISO|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by" +
        " Rapid immunoassay^LN^^^^2.68|||202108020000-0500|202108020000-0500||||||||1679892871^Doolittle^Doctor^^^^" +
        "^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|(123)456-7890^WPN^PH^^1^123^4567890|||||202108020000-0500|||F\n" +
        "OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2." +
        "68||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078^^^^2.7|||F|||20210802000" +
        "0-0500|05D2222542^ISO||10811877011290_DIT^10811877011290^99ELR^^^^2.68^^10811877011290_DIT||20" +
        "2108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^" +
        "San Jose^FL^95126^^^^06085\n" +
        "OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||N^No^HL70136||||||F|||202" +
        "108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX" +
        "^^^05D2222542|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202108020000-0500" +
        "|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|" +
        "6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202108020000-05" +
        "00|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D22225" +
        "42|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||2021080" +
        "20000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^" +
        "05D2222542|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "SPM|1|1234d1d1-95fe-462c-8ac6-46728dba581c&&05D2222542&ISO^1234d1d1-95fe-462c-8ac6-46728dba581c&&05D22225" +
        "42&ISO||445297001^Swab of internal nose^SCT^^^^2.67||||53342003^Internal nose structure (body structure)^" +
        "SCT^^^^2020-09-01|||||||||202108020000-0500|20210802000006.0000-0500"

    val bulk_fhir_bundles = """
        { "resourceType": "Bundle", "id": "1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347", "meta": { "lastUpdated": "2022-11-07T22:56:07.832+00:00" }, "identifier": { "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, "type": "message", "timestamp": "2021-08-03T13:15:11.015+00:00", "entry": [ { "fullUrl": "MessageHeader/c03f1b6b-cfc3-3477-89c0-d38316cd1a38", "resource": { "resourceType": "MessageHeader", "id": "c03f1b6b-cfc3-3477-89c0-d38316cd1a38", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "eventCoding": { "system": "http://terminology.hl7.org/CodeSystem/v2-0003", "code": "R01", "display": "ORU/ACK - Unsolicited transmission of an observation message" }, "destination": [ { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/facility-identifier", "valueOid": "urn:oid:2.16.840.1.114222.4.1.214104" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/application-identifier", "valueOid": "urn:oid:2.16.840.1.114222.4.3.3.10.1.1" } ], "name": "CDPH CA REDIE", "endpoint": "CDPH_CID" } ], "sender": { "reference": "Organization/1667861767851428000.4998bfc4-e2a0-4174-ab9a-ec1889429a64" }, "source": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org", "valueReference": { "reference": "Organization/1667861767863091000.87c27799-355a-422e-9338-e9b936a761c2" } }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/software-install-date", "valueDateTime": "2021-07-26" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/software-binary-id", "valueString": "0.1-SNAPSHOT" } ], "name": "CDC PRIME - Atlanta,", "software": "PRIME Data Hub", "version": "0.1-SNAPSHOT", "endpoint": "urn:oid:2.16.840.1.114222.4.1.237821" } } }, { "fullUrl": "Organization/1667861767851428000.4998bfc4-e2a0-4174-ab9a-ec1889429a64", "resource": { "resourceType": "Organization", "id": "1667861767851428000.4998bfc4-e2a0-4174-ab9a-ec1889429a64", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "urn:oid:05D2222542" } ], "name": "Winchester House", "address": [ { "country": "USA" } ] } }, { "fullUrl": "Organization/1667861767863091000.87c27799-355a-422e-9338-e9b936a761c2", "resource": { "resourceType": "Organization", "id": "1667861767863091000.87c27799-355a-422e-9338-e9b936a761c2", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "name": "Centers for Disease Control and Prevention" } }, { "fullUrl": "Provenance/1667861767909887000.56c99862-7264-4c8f-9d5e-896ac70a0d75", "resource": { "resourceType": "Provenance", "id": "1667861767909887000.56c99862-7264-4c8f-9d5e-896ac70a0d75", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "occurredDateTime": "2021-08-03T13:15:11.0147Z", "recorded": "2021-08-03T13:15:11.0147Z", "activity": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0003", "code": "R01", "display": "ORU_R01" } ] }, "agent": [ { "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/provenance-participant-type", "code": "author" } ] }, "who": { "reference": "Organization/1667861767907057000.c3030f30-9467-4e1d-95c7-30cce09dd5f1" } } ], "entity": [ { "role": "source", "what": { "reference": "Device/1667861767911610000.bd8aa647-0013-41c3-9d51-e05fa01876ea" } } ] } }, { "fullUrl": "Organization/1667861767907057000.c3030f30-9467-4e1d-95c7-30cce09dd5f1", "resource": { "resourceType": "Organization", "id": "1667861767907057000.c3030f30-9467-4e1d-95c7-30cce09dd5f1", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "name": "Winchester House", "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Device/1667861767911610000.bd8aa647-0013-41c3-9d51-e05fa01876ea", "resource": { "resourceType": "Device", "id": "1667861767911610000.bd8aa647-0013-41c3-9d51-e05fa01876ea", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "type": { "coding": [ { "system": "http://terminology.hl7.org/NamingSystem/uri", "code": "2.16.840.1.114222.4.1.237821", "display": "CDC PRIME - Atlanta," } ] } } ], "deviceName": [ { "name": "CDC PRIME - Atlanta,", "type": "user-friendly-name" } ] } }, { "fullUrl": "Observation/1667861767955966000.f3f94c27-e225-4aac-b6f5-2750f45dac4f", "resource": { "resourceType": "Observation", "id": "1667861767955966000.f3f94c27-e225-4aac-b6f5-2750f45dac4f", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/producer-id", "valueReference": { "reference": "Organization/1667861767955540000.c1a6e3bd-7a1f-4cd3-8ec2-9db7f4ce6039" } } ], "identifier": [ { "system": "urn:id:extID", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "final", "category": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory", "display": "Laboratory" } ] } ], "code": { "coding": [ { "system": "http://loinc.org", "code": "94558-4" } ], "text": "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectiveDateTime": "2021-08-02T00:00:00-05:00", "issued": "2021-08-02T00:00:00-05:00", "performer": [ { "reference": "Organization/1667861767948602000.25043715-a246-4abb-8f4b-7277ebc17027" } ], "valueCodeableConcept": { "coding": [ { "system": "http://snomed.info/sct", "code": "260415000" } ], "text": "Not detected" }, "interpretation": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0078", "code": "N", "display": "Normal" } ] } ], "method": { "coding": [ { "system": "99ELR", "code": "10811877011290_DIT" } ], "text": "10811877011290" } } }, { "fullUrl": "Organization/1667861767948602000.25043715-a246-4abb-8f4b-7277ebc17027", "resource": { "resourceType": "Organization", "id": "1667861767948602000.25043715-a246-4abb-8f4b-7277ebc17027", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:2.16.840.1.113883.19.4.6" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id", "valueString": "ISO" } ], "identifier": [ { "value": "05D2222542" } ], "type": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "XX" } ] } ], "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126" } ], "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Organization/1667861767955540000.c1a6e3bd-7a1f-4cd3-8ec2-9db7f4ce6039", "resource": { "resourceType": "Organization", "id": "1667861767955540000.c1a6e3bd-7a1f-4cd3-8ec2-9db7f4ce6039", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "05D2222542" } ], "name": "ISO" } }, { "fullUrl": "Observation/1667861767978508000.c6cb1063-2f87-45d5-b8f9-798ac822ecdd", "resource": { "resourceType": "Observation", "id": "1667861767978508000.c6cb1063-2f87-45d5-b8f9-798ac822ecdd", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/producer-id", "valueReference": { "reference": "Organization/1667861767978089000.af51483e-b1e1-4523-b6ad-3c1d5742d470" } } ], "identifier": [ { "system": "urn:id:extID", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "final", "category": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory", "display": "Laboratory" } ] } ], "code": { "coding": [ { "system": "http://loinc.org", "code": "95418-0" } ], "text": "Whether patient is employed in a healthcare setting" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectiveDateTime": "2021-08-02T00:00:00-05:00", "issued": "2021-08-02T00:00:00-05:00", "performer": [ { "reference": "Organization/1667861767970948000.74f6a304-5bbc-40b0-8f3f-a423d1bc3fb8" } ], "valueCodeableConcept": { "coding": [ { "system": "HL70136", "code": "N" } ], "text": "No" } } }, { "fullUrl": "Organization/1667861767970948000.74f6a304-5bbc-40b0-8f3f-a423d1bc3fb8", "resource": { "resourceType": "Organization", "id": "1667861767970948000.74f6a304-5bbc-40b0-8f3f-a423d1bc3fb8", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:2.16.840.1.113883.19.4.6" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id", "valueString": "ISO" } ], "identifier": [ { "value": "05D2222542" } ], "type": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "XX" } ] } ], "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126-5285" } ], "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Organization/1667861767978089000.af51483e-b1e1-4523-b6ad-3c1d5742d470", "resource": { "resourceType": "Organization", "id": "1667861767978089000.af51483e-b1e1-4523-b6ad-3c1d5742d470", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "05D2222542" } ] } }, { "fullUrl": "Observation/1667861767998499000.d390c176-f9c4-4cea-892b-24b787ebf320", "resource": { "resourceType": "Observation", "id": "1667861767998499000.d390c176-f9c4-4cea-892b-24b787ebf320", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/producer-id", "valueReference": { "reference": "Organization/1667861767998101000.72c561bc-39f3-4556-a275-5eca79c9c820" } } ], "identifier": [ { "system": "urn:id:extID", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "final", "category": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory", "display": "Laboratory" } ] } ], "code": { "coding": [ { "system": "http://loinc.org", "code": "95417-2" } ], "text": "First test for condition of interest" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectiveDateTime": "2021-08-02T00:00:00-05:00", "issued": "2021-08-02T00:00:00-05:00", "performer": [ { "reference": "Organization/1667861767990271000.886996f6-051c-43d8-9948-1d36422f8aaf" } ], "valueCodeableConcept": { "coding": [ { "system": "HL70136", "code": "N" } ], "text": "No" } } }, { "fullUrl": "Organization/1667861767990271000.886996f6-051c-43d8-9948-1d36422f8aaf", "resource": { "resourceType": "Organization", "id": "1667861767990271000.886996f6-051c-43d8-9948-1d36422f8aaf", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:2.16.840.1.113883.19.4.6" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id", "valueString": "ISO" } ], "identifier": [ { "value": "05D2222542" } ], "type": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "XX" } ] } ], "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126-5285" } ], "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Organization/1667861767998101000.72c561bc-39f3-4556-a275-5eca79c9c820", "resource": { "resourceType": "Organization", "id": "1667861767998101000.72c561bc-39f3-4556-a275-5eca79c9c820", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "05D2222542" } ] } }, { "fullUrl": "Observation/1667861768018399000.6d1d1647-1c2f-4c2d-baf8-61c7d98fd6f7", "resource": { "resourceType": "Observation", "id": "1667861768018399000.6d1d1647-1c2f-4c2d-baf8-61c7d98fd6f7", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/producer-id", "valueReference": { "reference": "Organization/1667861768018078000.caf4414b-d653-46c7-8e07-8ed2450874b4" } } ], "identifier": [ { "system": "urn:id:extID", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "final", "category": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory", "display": "Laboratory" } ] } ], "code": { "coding": [ { "system": "http://loinc.org", "code": "95421-4" } ], "text": "Resides in a congregate care setting" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectiveDateTime": "2021-08-02T00:00:00-05:00", "issued": "2021-08-02T00:00:00-05:00", "performer": [ { "reference": "Organization/1667861768011635000.50bfb375-e966-457d-a925-b890d4647c55" } ], "valueCodeableConcept": { "coding": [ { "system": "HL70136", "code": "Y" } ], "text": "Yes" } } }, { "fullUrl": "Organization/1667861768011635000.50bfb375-e966-457d-a925-b890d4647c55", "resource": { "resourceType": "Organization", "id": "1667861768011635000.50bfb375-e966-457d-a925-b890d4647c55", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:2.16.840.1.113883.19.4.6" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id", "valueString": "ISO" } ], "identifier": [ { "value": "05D2222542" } ], "type": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "XX" } ] } ], "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126-5285" } ], "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Organization/1667861768018078000.caf4414b-d653-46c7-8e07-8ed2450874b4", "resource": { "resourceType": "Organization", "id": "1667861768018078000.caf4414b-d653-46c7-8e07-8ed2450874b4", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "05D2222542" } ] } }, { "fullUrl": "Observation/1667861768034907000.7d0b81c8-41d5-4738-9f77-1f8181d105a3", "resource": { "resourceType": "Observation", "id": "1667861768034907000.7d0b81c8-41d5-4738-9f77-1f8181d105a3", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/producer-id", "valueReference": { "reference": "Organization/1667861768034563000.9add9712-e16f-4cfb-ba45-5f707e5e8e44" } } ], "identifier": [ { "system": "urn:id:extID", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "final", "category": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory", "display": "Laboratory" } ] } ], "code": { "coding": [ { "system": "http://loinc.org", "code": "95419-8" } ], "text": "Has symptoms related to condition of interest" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectiveDateTime": "2021-08-02T00:00:00-05:00", "issued": "2021-08-02T00:00:00-05:00", "performer": [ { "reference": "Organization/1667861768027523000.4ddd1587-c0f5-43c2-b5a1-57d2557d8004" } ], "valueCodeableConcept": { "coding": [ { "system": "HL70136", "code": "N" } ], "text": "No" } } }, { "fullUrl": "Organization/1667861768027523000.4ddd1587-c0f5-43c2-b5a1-57d2557d8004", "resource": { "resourceType": "Organization", "id": "1667861768027523000.4ddd1587-c0f5-43c2-b5a1-57d2557d8004", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:2.16.840.1.113883.19.4.6" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id", "valueString": "ISO" } ], "identifier": [ { "value": "05D2222542" } ], "type": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "XX" } ] } ], "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126-5285" } ], "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Organization/1667861768034563000.9add9712-e16f-4cfb-ba45-5f707e5e8e44", "resource": { "resourceType": "Organization", "id": "1667861768034563000.9add9712-e16f-4cfb-ba45-5f707e5e8e44", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "05D2222542" } ] } }, { "fullUrl": "Specimen/1667861768049080000.f2ae4be6-3cf0-4615-b994-aea8a34fb21c", "resource": { "resourceType": "Specimen", "id": "1667861768049080000.f2ae4be6-3cf0-4615-b994-aea8a34fb21c", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "FGN" } ] }, "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "type": { "coding": [ { "system": "http://snomed.info/sct", "code": "445297001" } ], "text": "Swab of internal nose" }, "receivedTime": "2021-08-02T00:00:06-05:00", "collection": { "collectedDateTime": "2021-08-02T00:00:00-05:00", "bodySite": { "coding": [ { "system": "http://snomed.info/sct", "code": "53342003" } ], "text": "Internal nose structure (body structure)" } } } }, { "fullUrl": "DiagnosticReport/1667861768056908000.18c9371a-bbeb-40ff-9860-9aa68c5d2e00", "resource": { "resourceType": "DiagnosticReport", "id": "1667861768056908000.18c9371a-bbeb-40ff-9860-9aa68c5d2e00", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/result-status", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0123", "code": "F" } ] } } ], "identifier": [ { "system": "urn:id:extID", "value": "20210803131511.0147+0000" }, { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "FILL", "display": "Filler Identifier" } ] }, "system": "urn:id:Winchester_House", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "PLAC", "display": "Placer Identifier" } ] }, "system": "urn:id:Winchester_House", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "basedOn": [ { "reference": "ServiceRequest/1667861768068290000.c4b44f97-f84a-49b2-9871-f5b3434d4ff0" } ], "status": "final", "code": { "coding": [ { "system": "http://loinc.org", "code": "94558-4" } ], "text": "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectivePeriod": { "start": "2021-08-02T00:00:00-05:00", "end": "2021-08-02T00:00:00-05:00" }, "issued": "2021-08-02T00:00:00-05:00", "specimen": [ { "reference": "Specimen/1667861768049080000.f2ae4be6-3cf0-4615-b994-aea8a34fb21c" } ], "result": [ { "reference": "Observation/1667861767955966000.f3f94c27-e225-4aac-b6f5-2750f45dac4f" }, { "reference": "Observation/1667861767978508000.c6cb1063-2f87-45d5-b8f9-798ac822ecdd" }, { "reference": "Observation/1667861767998499000.d390c176-f9c4-4cea-892b-24b787ebf320" }, { "reference": "Observation/1667861768018399000.6d1d1647-1c2f-4c2d-baf8-61c7d98fd6f7" }, { "reference": "Observation/1667861768034907000.7d0b81c8-41d5-4738-9f77-1f8181d105a3" } ] } }, { "fullUrl": "Practitioner/1667861768065762000.258439c1-2c12-4e30-ab0e-2f8d6af18267", "resource": { "resourceType": "Practitioner", "id": "1667861768065762000.258439c1-2c12-4e30-ab0e-2f8d6af18267", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ {} ] } }, { "fullUrl": "Practitioner/1667861768077534000.4725986c-c3d8-4d0f-a310-270827ec2c75", "resource": { "resourceType": "Practitioner", "id": "1667861768077534000.4725986c-c3d8-4d0f-a310-270827ec2c75", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/identifier-type", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "NPI" } ] } } ], "identifier": [ { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority-namespace-id", "valueString": "CMS" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority-universal-id", "valueOid": "urn:oid:2.16.840.1.113883.3.249" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "NPI", "display": "National provider identifier" } ] }, "system": "urn:id:CMS", "value": "1679892871" } ], "name": [ { "text": "Doctor Doolittle", "family": "Doolittle", "given": [ "Doctor" ] } ], "telecom": [ { "system": "phone", "value": "+1 123 456 7890", "use": "work" } ], "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "state": "CA", "postalCode": "95126" } ] } }, { "fullUrl": "Organization/1667861768093053000.243d7442-744a-4a30-8ceb-7016738b869f", "resource": { "resourceType": "Organization", "id": "1667861768093053000.243d7442-744a-4a30-8ceb-7016738b869f", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126" } ], "contact": [ { "telecom": [ { "system": "phone", "value": "+1 123 456 7890", "use": "work" } ] } ] } }, { "fullUrl": "PractitionerRole/1667861768093461000.1af41fee-8c9a-4711-901b-3b363e4a12df", "resource": { "resourceType": "PractitionerRole", "id": "1667861768093461000.1af41fee-8c9a-4711-901b-3b363e4a12df", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "practitioner": { "reference": "Practitioner/1667861768077534000.4725986c-c3d8-4d0f-a310-270827ec2c75" }, "organization": { "reference": "Organization/1667861768093053000.243d7442-744a-4a30-8ceb-7016738b869f" } } }, { "fullUrl": "ServiceRequest/1667861768068290000.c4b44f97-f84a-49b2-9871-f5b3434d4ff0", "resource": { "resourceType": "ServiceRequest", "id": "1667861768068290000.c4b44f97-f84a-49b2-9871-f5b3434d4ff0", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/collector-identifier", "valueReference": { "reference": "Practitioner/1667861768065762000.258439c1-2c12-4e30-ab0e-2f8d6af18267" } }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/order-control", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0119", "code": "RE" } ] } }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/result-status", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0123", "code": "F" } ] } } ], "identifier": [ { "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "VN", "display": "Visit number" } ] }, "value": "20210803131511.0147+0000" }, { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "PLAC", "display": "Placer Identifier" } ] }, "system": "urn:id:Winchester_House", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "FILL", "display": "Filler Identifier" } ] }, "system": "urn:id:Winchester_House", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "unknown", "intent": "order", "code": { "coding": [ { "system": "http://loinc.org", "code": "94558-4" } ], "text": "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "occurrenceDateTime": "2021-08-02", "requester": { "reference": "PractitionerRole/1667861768093461000.1af41fee-8c9a-4711-901b-3b363e4a12df" } } }, { "fullUrl": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44", "resource": { "resourceType": "Patient", "id": "1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "http://ibm.com/fhir/cdm/StructureDefinition/local-race-cd", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v3-Race", "code": "2106-3" } ], "text": "White" } }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/ethnic-group", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0189", "code": "N" } ], "text": "Non Hispanic or Latino" } }, { "url": "http://hl7.org/fhir/StructureDefinition/patient-animal", "valueCodeableConcept": {} } ], "identifier": [ { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-facility-universal-id", "valueOid": "urn:oid:05D2222542" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority-universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "PI", "display": "Patient internal identifier" } ] }, "system": "urn:id:Winchester_House", "value": "09d12345-0987-1234-1234-111b1ee0879f" } ], "name": [ { "use": "official", "text": "Bugs C Bunny", "family": "Bunny", "given": [ "Bugs", "C" ] } ], "telecom": [ { "system": "phone", "value": "+1 123 456 7890", "use": "home" } ], "gender": "male", "birthDate": "1900-01-01", "deceasedBoolean": false, "address": [ { "line": [ "12345 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95125", "country": "USA" } ] } } ] }
        { "resourceType": "Bundle", "id": "1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347", "meta": { "lastUpdated": "2022-11-07T22:56:07.832+00:00" }, "identifier": { "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, "type": "message", "timestamp": "2021-08-03T13:15:11.015+00:00", "entry": [ { "fullUrl": "MessageHeader/c03f1b6b-cfc3-3477-89c0-d38316cd1a38", "resource": { "resourceType": "MessageHeader", "id": "c03f1b6b-cfc3-3477-89c0-d38316cd1a38", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "eventCoding": { "system": "http://terminology.hl7.org/CodeSystem/v2-0003", "code": "R01", "display": "ORU/ACK - Unsolicited transmission of an observation message" }, "destination": [ { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/facility-identifier", "valueOid": "urn:oid:2.16.840.1.114222.4.1.214104" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/application-identifier", "valueOid": "urn:oid:2.16.840.1.114222.4.3.3.10.1.1" } ], "name": "CDPH CA REDIE", "endpoint": "CDPH_CID" } ], "sender": { "reference": "Organization/1667861767851428000.4998bfc4-e2a0-4174-ab9a-ec1889429a64" }, "source": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org", "valueReference": { "reference": "Organization/1667861767863091000.87c27799-355a-422e-9338-e9b936a761c2" } }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/software-install-date", "valueDateTime": "2021-07-26" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/software-binary-id", "valueString": "0.1-SNAPSHOT" } ], "name": "CDC PRIME - Atlanta,", "software": "PRIME Data Hub", "version": "0.1-SNAPSHOT", "endpoint": "urn:oid:2.16.840.1.114222.4.1.237821" } } }, { "fullUrl": "Organization/1667861767851428000.4998bfc4-e2a0-4174-ab9a-ec1889429a64", "resource": { "resourceType": "Organization", "id": "1667861767851428000.4998bfc4-e2a0-4174-ab9a-ec1889429a64", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "urn:oid:05D2222542" } ], "name": "Winchester House", "address": [ { "country": "USA" } ] } }, { "fullUrl": "Organization/1667861767863091000.87c27799-355a-422e-9338-e9b936a761c2", "resource": { "resourceType": "Organization", "id": "1667861767863091000.87c27799-355a-422e-9338-e9b936a761c2", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "name": "Centers for Disease Control and Prevention" } }, { "fullUrl": "Provenance/1667861767909887000.56c99862-7264-4c8f-9d5e-896ac70a0d75", "resource": { "resourceType": "Provenance", "id": "1667861767909887000.56c99862-7264-4c8f-9d5e-896ac70a0d75", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "occurredDateTime": "2021-08-03T13:15:11.0147Z", "recorded": "2021-08-03T13:15:11.0147Z", "activity": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0003", "code": "R01", "display": "ORU_R01" } ] }, "agent": [ { "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/provenance-participant-type", "code": "author" } ] }, "who": { "reference": "Organization/1667861767907057000.c3030f30-9467-4e1d-95c7-30cce09dd5f1" } } ], "entity": [ { "role": "source", "what": { "reference": "Device/1667861767911610000.bd8aa647-0013-41c3-9d51-e05fa01876ea" } } ] } }, { "fullUrl": "Organization/1667861767907057000.c3030f30-9467-4e1d-95c7-30cce09dd5f1", "resource": { "resourceType": "Organization", "id": "1667861767907057000.c3030f30-9467-4e1d-95c7-30cce09dd5f1", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "name": "Winchester House", "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Device/1667861767911610000.bd8aa647-0013-41c3-9d51-e05fa01876ea", "resource": { "resourceType": "Device", "id": "1667861767911610000.bd8aa647-0013-41c3-9d51-e05fa01876ea", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "type": { "coding": [ { "system": "http://terminology.hl7.org/NamingSystem/uri", "code": "2.16.840.1.114222.4.1.237821", "display": "CDC PRIME - Atlanta," } ] } } ], "deviceName": [ { "name": "CDC PRIME - Atlanta,", "type": "user-friendly-name" } ] } }, { "fullUrl": "Observation/1667861767955966000.f3f94c27-e225-4aac-b6f5-2750f45dac4f", "resource": { "resourceType": "Observation", "id": "1667861767955966000.f3f94c27-e225-4aac-b6f5-2750f45dac4f", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/producer-id", "valueReference": { "reference": "Organization/1667861767955540000.c1a6e3bd-7a1f-4cd3-8ec2-9db7f4ce6039" } } ], "identifier": [ { "system": "urn:id:extID", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "final", "category": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory", "display": "Laboratory" } ] } ], "code": { "coding": [ { "system": "http://loinc.org", "code": "94558-4" } ], "text": "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectiveDateTime": "2021-08-02T00:00:00-05:00", "issued": "2021-08-02T00:00:00-05:00", "performer": [ { "reference": "Organization/1667861767948602000.25043715-a246-4abb-8f4b-7277ebc17027" } ], "valueCodeableConcept": { "coding": [ { "system": "http://snomed.info/sct", "code": "260415000" } ], "text": "Not detected" }, "interpretation": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0078", "code": "N", "display": "Normal" } ] } ], "method": { "coding": [ { "system": "99ELR", "code": "10811877011290_DIT" } ], "text": "10811877011290" } } }, { "fullUrl": "Organization/1667861767948602000.25043715-a246-4abb-8f4b-7277ebc17027", "resource": { "resourceType": "Organization", "id": "1667861767948602000.25043715-a246-4abb-8f4b-7277ebc17027", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:2.16.840.1.113883.19.4.6" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id", "valueString": "ISO" } ], "identifier": [ { "value": "05D2222542" } ], "type": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "XX" } ] } ], "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126" } ], "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Organization/1667861767955540000.c1a6e3bd-7a1f-4cd3-8ec2-9db7f4ce6039", "resource": { "resourceType": "Organization", "id": "1667861767955540000.c1a6e3bd-7a1f-4cd3-8ec2-9db7f4ce6039", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "05D2222542" } ], "name": "ISO" } }, { "fullUrl": "Observation/1667861767978508000.c6cb1063-2f87-45d5-b8f9-798ac822ecdd", "resource": { "resourceType": "Observation", "id": "1667861767978508000.c6cb1063-2f87-45d5-b8f9-798ac822ecdd", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/producer-id", "valueReference": { "reference": "Organization/1667861767978089000.af51483e-b1e1-4523-b6ad-3c1d5742d470" } } ], "identifier": [ { "system": "urn:id:extID", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "final", "category": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory", "display": "Laboratory" } ] } ], "code": { "coding": [ { "system": "http://loinc.org", "code": "95418-0" } ], "text": "Whether patient is employed in a healthcare setting" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectiveDateTime": "2021-08-02T00:00:00-05:00", "issued": "2021-08-02T00:00:00-05:00", "performer": [ { "reference": "Organization/1667861767970948000.74f6a304-5bbc-40b0-8f3f-a423d1bc3fb8" } ], "valueCodeableConcept": { "coding": [ { "system": "HL70136", "code": "N" } ], "text": "No" } } }, { "fullUrl": "Organization/1667861767970948000.74f6a304-5bbc-40b0-8f3f-a423d1bc3fb8", "resource": { "resourceType": "Organization", "id": "1667861767970948000.74f6a304-5bbc-40b0-8f3f-a423d1bc3fb8", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:2.16.840.1.113883.19.4.6" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id", "valueString": "ISO" } ], "identifier": [ { "value": "05D2222542" } ], "type": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "XX" } ] } ], "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126-5285" } ], "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Organization/1667861767978089000.af51483e-b1e1-4523-b6ad-3c1d5742d470", "resource": { "resourceType": "Organization", "id": "1667861767978089000.af51483e-b1e1-4523-b6ad-3c1d5742d470", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "05D2222542" } ] } }, { "fullUrl": "Observation/1667861767998499000.d390c176-f9c4-4cea-892b-24b787ebf320", "resource": { "resourceType": "Observation", "id": "1667861767998499000.d390c176-f9c4-4cea-892b-24b787ebf320", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/producer-id", "valueReference": { "reference": "Organization/1667861767998101000.72c561bc-39f3-4556-a275-5eca79c9c820" } } ], "identifier": [ { "system": "urn:id:extID", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "final", "category": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory", "display": "Laboratory" } ] } ], "code": { "coding": [ { "system": "http://loinc.org", "code": "95417-2" } ], "text": "First test for condition of interest" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectiveDateTime": "2021-08-02T00:00:00-05:00", "issued": "2021-08-02T00:00:00-05:00", "performer": [ { "reference": "Organization/1667861767990271000.886996f6-051c-43d8-9948-1d36422f8aaf" } ], "valueCodeableConcept": { "coding": [ { "system": "HL70136", "code": "N" } ], "text": "No" } } }, { "fullUrl": "Organization/1667861767990271000.886996f6-051c-43d8-9948-1d36422f8aaf", "resource": { "resourceType": "Organization", "id": "1667861767990271000.886996f6-051c-43d8-9948-1d36422f8aaf", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:2.16.840.1.113883.19.4.6" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id", "valueString": "ISO" } ], "identifier": [ { "value": "05D2222542" } ], "type": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "XX" } ] } ], "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126-5285" } ], "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Organization/1667861767998101000.72c561bc-39f3-4556-a275-5eca79c9c820", "resource": { "resourceType": "Organization", "id": "1667861767998101000.72c561bc-39f3-4556-a275-5eca79c9c820", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "05D2222542" } ] } }, { "fullUrl": "Observation/1667861768018399000.6d1d1647-1c2f-4c2d-baf8-61c7d98fd6f7", "resource": { "resourceType": "Observation", "id": "1667861768018399000.6d1d1647-1c2f-4c2d-baf8-61c7d98fd6f7", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/producer-id", "valueReference": { "reference": "Organization/1667861768018078000.caf4414b-d653-46c7-8e07-8ed2450874b4" } } ], "identifier": [ { "system": "urn:id:extID", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "final", "category": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory", "display": "Laboratory" } ] } ], "code": { "coding": [ { "system": "http://loinc.org", "code": "95421-4" } ], "text": "Resides in a congregate care setting" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectiveDateTime": "2021-08-02T00:00:00-05:00", "issued": "2021-08-02T00:00:00-05:00", "performer": [ { "reference": "Organization/1667861768011635000.50bfb375-e966-457d-a925-b890d4647c55" } ], "valueCodeableConcept": { "coding": [ { "system": "HL70136", "code": "Y" } ], "text": "Yes" } } }, { "fullUrl": "Organization/1667861768011635000.50bfb375-e966-457d-a925-b890d4647c55", "resource": { "resourceType": "Organization", "id": "1667861768011635000.50bfb375-e966-457d-a925-b890d4647c55", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:2.16.840.1.113883.19.4.6" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id", "valueString": "ISO" } ], "identifier": [ { "value": "05D2222542" } ], "type": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "XX" } ] } ], "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126-5285" } ], "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Organization/1667861768018078000.caf4414b-d653-46c7-8e07-8ed2450874b4", "resource": { "resourceType": "Organization", "id": "1667861768018078000.caf4414b-d653-46c7-8e07-8ed2450874b4", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "05D2222542" } ] } }, { "fullUrl": "Observation/1667861768034907000.7d0b81c8-41d5-4738-9f77-1f8181d105a3", "resource": { "resourceType": "Observation", "id": "1667861768034907000.7d0b81c8-41d5-4738-9f77-1f8181d105a3", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/producer-id", "valueReference": { "reference": "Organization/1667861768034563000.9add9712-e16f-4cfb-ba45-5f707e5e8e44" } } ], "identifier": [ { "system": "urn:id:extID", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "final", "category": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory", "display": "Laboratory" } ] } ], "code": { "coding": [ { "system": "http://loinc.org", "code": "95419-8" } ], "text": "Has symptoms related to condition of interest" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectiveDateTime": "2021-08-02T00:00:00-05:00", "issued": "2021-08-02T00:00:00-05:00", "performer": [ { "reference": "Organization/1667861768027523000.4ddd1587-c0f5-43c2-b5a1-57d2557d8004" } ], "valueCodeableConcept": { "coding": [ { "system": "HL70136", "code": "N" } ], "text": "No" } } }, { "fullUrl": "Organization/1667861768027523000.4ddd1587-c0f5-43c2-b5a1-57d2557d8004", "resource": { "resourceType": "Organization", "id": "1667861768027523000.4ddd1587-c0f5-43c2-b5a1-57d2557d8004", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:2.16.840.1.113883.19.4.6" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/namespace-id", "valueString": "ISO" } ], "identifier": [ { "value": "05D2222542" } ], "type": [ { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "XX" } ] } ], "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126-5285" } ], "contact": [ { "purpose": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/contactentity-type", "code": "ADMIN", "display": "Administrative" } ], "text": "Organization Medical Director" } } ] } }, { "fullUrl": "Organization/1667861768034563000.9add9712-e16f-4cfb-ba45-5f707e5e8e44", "resource": { "resourceType": "Organization", "id": "1667861768034563000.9add9712-e16f-4cfb-ba45-5f707e5e8e44", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "value": "05D2222542" } ] } }, { "fullUrl": "Specimen/1667861768049080000.f2ae4be6-3cf0-4615-b994-aea8a34fb21c", "resource": { "resourceType": "Specimen", "id": "1667861768049080000.f2ae4be6-3cf0-4615-b994-aea8a34fb21c", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "FGN" } ] }, "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "type": { "coding": [ { "system": "http://snomed.info/sct", "code": "445297001" } ], "text": "Swab of internal nose" }, "receivedTime": "2021-08-02T00:00:06-05:00", "collection": { "collectedDateTime": "2021-08-02T00:00:00-05:00", "bodySite": { "coding": [ { "system": "http://snomed.info/sct", "code": "53342003" } ], "text": "Internal nose structure (body structure)" } } } }, { "fullUrl": "DiagnosticReport/1667861768056908000.18c9371a-bbeb-40ff-9860-9aa68c5d2e00", "resource": { "resourceType": "DiagnosticReport", "id": "1667861768056908000.18c9371a-bbeb-40ff-9860-9aa68c5d2e00", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/result-status", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0123", "code": "F" } ] } } ], "identifier": [ { "system": "urn:id:extID", "value": "20210803131511.0147+0000" }, { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "FILL", "display": "Filler Identifier" } ] }, "system": "urn:id:Winchester_House", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "PLAC", "display": "Placer Identifier" } ] }, "system": "urn:id:Winchester_House", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "basedOn": [ { "reference": "ServiceRequest/1667861768068290000.c4b44f97-f84a-49b2-9871-f5b3434d4ff0" } ], "status": "final", "code": { "coding": [ { "system": "http://loinc.org", "code": "94558-4" } ], "text": "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "effectivePeriod": { "start": "2021-08-02T00:00:00-05:00", "end": "2021-08-02T00:00:00-05:00" }, "issued": "2021-08-02T00:00:00-05:00", "specimen": [ { "reference": "Specimen/1667861768049080000.f2ae4be6-3cf0-4615-b994-aea8a34fb21c" } ], "result": [ { "reference": "Observation/1667861767955966000.f3f94c27-e225-4aac-b6f5-2750f45dac4f" }, { "reference": "Observation/1667861767978508000.c6cb1063-2f87-45d5-b8f9-798ac822ecdd" }, { "reference": "Observation/1667861767998499000.d390c176-f9c4-4cea-892b-24b787ebf320" }, { "reference": "Observation/1667861768018399000.6d1d1647-1c2f-4c2d-baf8-61c7d98fd6f7" }, { "reference": "Observation/1667861768034907000.7d0b81c8-41d5-4738-9f77-1f8181d105a3" } ] } }, { "fullUrl": "Practitioner/1667861768065762000.258439c1-2c12-4e30-ab0e-2f8d6af18267", "resource": { "resourceType": "Practitioner", "id": "1667861768065762000.258439c1-2c12-4e30-ab0e-2f8d6af18267", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "identifier": [ {} ] } }, { "fullUrl": "Practitioner/1667861768077534000.4725986c-c3d8-4d0f-a310-270827ec2c75", "resource": { "resourceType": "Practitioner", "id": "1667861768077534000.4725986c-c3d8-4d0f-a310-270827ec2c75", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/identifier-type", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "NPI" } ] } } ], "identifier": [ { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority-namespace-id", "valueString": "CMS" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority-universal-id", "valueOid": "urn:oid:2.16.840.1.113883.3.249" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "NPI", "display": "National provider identifier" } ] }, "system": "urn:id:CMS", "value": "1679892871" } ], "name": [ { "text": "Doctor Doolittle", "family": "Doolittle", "given": [ "Doctor" ] } ], "telecom": [ { "system": "phone", "value": "+1 123 456 7890", "use": "work" } ], "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "state": "CA", "postalCode": "95126" } ] } }, { "fullUrl": "Organization/1667861768093053000.243d7442-744a-4a30-8ceb-7016738b869f", "resource": { "resourceType": "Organization", "id": "1667861768093053000.243d7442-744a-4a30-8ceb-7016738b869f", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "name": "Winchester House", "address": [ { "line": [ "6789 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95126" } ], "contact": [ { "telecom": [ { "system": "phone", "value": "+1 123 456 7890", "use": "work" } ] } ] } }, { "fullUrl": "PractitionerRole/1667861768093461000.1af41fee-8c9a-4711-901b-3b363e4a12df", "resource": { "resourceType": "PractitionerRole", "id": "1667861768093461000.1af41fee-8c9a-4711-901b-3b363e4a12df", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "practitioner": { "reference": "Practitioner/1667861768077534000.4725986c-c3d8-4d0f-a310-270827ec2c75" }, "organization": { "reference": "Organization/1667861768093053000.243d7442-744a-4a30-8ceb-7016738b869f" } } }, { "fullUrl": "ServiceRequest/1667861768068290000.c4b44f97-f84a-49b2-9871-f5b3434d4ff0", "resource": { "resourceType": "ServiceRequest", "id": "1667861768068290000.c4b44f97-f84a-49b2-9871-f5b3434d4ff0", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/collector-identifier", "valueReference": { "reference": "Practitioner/1667861768065762000.258439c1-2c12-4e30-ab0e-2f8d6af18267" } }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/order-control", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0119", "code": "RE" } ] } }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/result-status", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0123", "code": "F" } ] } } ], "identifier": [ { "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "VN", "display": "Visit number" } ] }, "value": "20210803131511.0147+0000" }, { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "PLAC", "display": "Placer Identifier" } ] }, "system": "urn:id:Winchester_House", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "FILL", "display": "Filler Identifier" } ] }, "system": "urn:id:Winchester_House", "value": "1234d1d1-95fe-462c-8ac6-46728dba581c" } ], "status": "unknown", "intent": "order", "code": { "coding": [ { "system": "http://loinc.org", "code": "94558-4" } ], "text": "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay" }, "subject": { "reference": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44" }, "occurrenceDateTime": "2021-08-02", "requester": { "reference": "PractitionerRole/1667861768093461000.1af41fee-8c9a-4711-901b-3b363e4a12df" } } }, { "fullUrl": "Patient/1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44", "resource": { "resourceType": "Patient", "id": "1667861767929699000.7dbebb1e-6497-4383-8c2f-de00717cbb44", "meta": { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id", "valueCodeableConcept": { "coding": [ { "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html", "code": "P" } ] } }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id", "valueId": "1234d1d1-95fe-462c-8ac6-46728dba581c" }, { "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version", "valueString": "2.5.1" } ] }, "extension": [ { "url": "http://ibm.com/fhir/cdm/StructureDefinition/local-race-cd", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v3-Race", "code": "2106-3" } ], "text": "White" } }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/ethnic-group", "valueCodeableConcept": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0189", "code": "N" } ], "text": "Non Hispanic or Latino" } }, { "url": "http://hl7.org/fhir/StructureDefinition/patient-animal", "valueCodeableConcept": {} } ], "identifier": [ { "extension": [ { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-facility-universal-id", "valueOid": "urn:oid:05D2222542" }, { "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority-universal-id", "valueOid": "urn:oid:05D2222542" } ], "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/v2-0203", "code": "PI", "display": "Patient internal identifier" } ] }, "system": "urn:id:Winchester_House", "value": "09d12345-0987-1234-1234-111b1ee0879f" } ], "name": [ { "use": "official", "text": "Bugs C Bunny", "family": "Bunny", "given": [ "Bugs", "C" ] } ], "telecom": [ { "system": "phone", "value": "+1 123 456 7890", "use": "home" } ], "gender": "male", "birthDate": "1900-01-01", "deceasedBoolean": false, "address": [ { "line": [ "12345 Main St" ], "city": "San Jose", "district": "06085", "state": "CA", "postalCode": "95125", "country": "USA" } ] } } ] }
    """.trimIndent()

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine {
        return spyk(
            WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
                .blobAccess(blobMock).queueAccess(queueMock).build()
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()

        // setup
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()
    }

    /** companion object tests **/
    // test addDuplicateLogs - duplicate file
    @Test
    fun `test addDuplicateLogs, duplicate file`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val actionLogs = ActionLogger()

        // act
        SubmissionReceiver.addDuplicateLogs(
            actionLogs,
            "Duplicate file",
            null,
            null
        )

        // assert
        assert(actionLogs.hasErrors())
        assert(actionLogs.errors.size == 1)
        assert(actionLogs.errors[0].scope == ActionLogScope.report)
    }

    @Test
    fun `test addDuplicateLogs, all items dupe`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val actionLogs = ActionLogger()

        // act
        SubmissionReceiver.addDuplicateLogs(
            actionLogs,
            "Duplicate submission",
            null,
            null
        )

        // assert
        assert(actionLogs.hasErrors())
        assert(actionLogs.errors.size == 1)
        assert(actionLogs.errors[0].scope == ActionLogScope.report)
    }

    // test addDuplicateLogs - duplicate item, skipInvalid = false
    @Test
    fun `test addDuplicateLogs, duplicate item, no skipInvalidItems`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val actionLogs = ActionLogger()

        // act
        SubmissionReceiver.addDuplicateLogs(
            actionLogs,
            "Duplicate item",
            1,
            null
        )

        // assert
        assert(actionLogs.hasErrors())
        assert(actionLogs.errors.size == 1)
        assert(actionLogs.errors[0].scope == ActionLogScope.item)
    }

    // doDuplicateDetection, one item is duplicate
    @Test
    fun `test doDuplicateDetection, 2 records, one duplicate`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val report = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)
        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val actionLogs = ActionLogger()

        every { engine.settings.findSender("Test Sender") } returns sender
        // first call to isDuplicateItem is false, second is true
        every { accessSpy.isDuplicateItem(any(), any()) }
            .returns(false)
            .andThen(true)

        // act
        SubmissionReceiver.doDuplicateDetection(
            engine,
            report,
            actionLogs
        )

        // assert
        verify(exactly = 2) {
            engine.isDuplicateItem(any())
        }
        verify(exactly = 1) {
            SubmissionReceiver.addDuplicateLogs(any(), any(), any(), any())
        }
    }

    // doDuplicateDetection, all items are duplicate
    @Test
    fun `test doDuplicateDetection, 2 records, both duplicate and already in db`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val report = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val actionLogs = ActionLogger()

        every { engine.settings.findSender("Test Sender") } returns sender
        every { accessSpy.isDuplicateItem(any(), any()) } returns true

        // act
        SubmissionReceiver.doDuplicateDetection(
            engine,
            report,
            actionLogs
        )

        // assert
        verify(exactly = 1) {
            SubmissionReceiver.addDuplicateLogs(any(), any(), any(), any())
        }
        verify(exactly = 2) {
            engine.isDuplicateItem(any())
        }
        assert(actionLogs.hasErrors())
    }

    // doDuplicateDetection, all items are duplicate
    @Test
    fun `test doDuplicateDetection, 2 records, identical rows, not in db`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val report = Report(one, listOf(listOf("1", "2"), listOf("1", "2")), source = TestSource, metadata = metadata)

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val actionLogs = ActionLogger()

        every { engine.settings.findSender("Test Sender") } returns sender
        every { accessSpy.isDuplicateItem(any(), any()) } returns false

        // act
        SubmissionReceiver.doDuplicateDetection(
            engine,
            report,
            actionLogs
        )

        // assert
        verify(exactly = 1) {
            SubmissionReceiver.addDuplicateLogs(any(), any(), any(), any())
        }
        verify(exactly = 1) {
            engine.isDuplicateItem(any())
        }
        assert(actionLogs.hasErrors())
    }

    /** COVID receiver tests **/
    @Test
    fun `test covid receiver processAsync`() {
        mockkObject(ReportWriter)
        mockkObject(BaseEngine)
        // setup
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val receiver = TopicReceiver(
            engine,
            actionHistory
        )

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val bodyFormat = Report.Format.CSV
        val bodyUrl = "http://anyblob.com"
        val bodyBytes = "".toByteArray()
        val csvSerializer = CsvSerializer(metadata)
        val hl7Serializer = Hl7Serializer(metadata, settings)
        every { BaseEngine.csvSerializerSingleton } returns csvSerializer
        every { BaseEngine.hl7SerializerSingleton } returns hl7Serializer
        every { ReportWriter.getBodyBytes(any(), any(), any(), any()) }.returns(bodyBytes)
        every { blobMock.uploadReport(any(), any(), any(), any()) }
            .returns(BlobAccess.BlobInfo(bodyFormat, bodyUrl, bodyBytes))
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit

        // act
        receiver.processAsync(
            report,
            Options.None,
            emptyMap(),
            emptyList()
        )

        // assert
        verify(exactly = 1) {
            ReportWriter.getBodyBytes(any(), any(), any(), any())
            blobMock.uploadReport(any(), any(), any(), any())
            actionHistory.trackCreatedReport(any(), any(), any())
            engine.insertProcessTask(any(), any(), any(), any())
        }
    }

    @Test
    fun `test covid receiver processAsync, incorrect format`() {
        mockkObject(ReportWriter)
        mockkObject(BaseEngine)
        // setup
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val receiver = TopicReceiver(
            engine,
            actionHistory
        )

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata,
            bodyFormat = Report.Format.HL7
        )

        val bodyFormat = Report.Format.CSV
        val bodyUrl = "http://anyblob.com"

        val bodyBytes = "".toByteArray()
        val csvSerializer = CsvSerializer(metadata)
        val hl7Serializer = Hl7Serializer(metadata, settings)
        every { BaseEngine.csvSerializerSingleton } returns csvSerializer
        every { BaseEngine.hl7SerializerSingleton } returns hl7Serializer
        every { ReportWriter.getBodyBytes(any(), any(), any(), any()) }.returns(bodyBytes)
        every { blobMock.uploadReport(any(), any(), any()) }
            .returns(BlobAccess.BlobInfo(bodyFormat, bodyUrl, bodyBytes))
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit

        // act
        var exceptionThrown = false
        try {
            receiver.processAsync(
                report,
                Options.None,
                emptyMap(),
                emptyList()
            )
        } catch (ex: IllegalStateException) {
            exceptionThrown = true
        }

        // assert
        assertTrue(exceptionThrown)
    }

    // validateAndMoveToProcessing
    @Test
    fun `test COVID receiver validateAndMoveToProcessing, async`() {
        // setup
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            TopicReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))

        every { engine.parseTopicReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { receiver.processAsync(any(), any(), any(), any()) } returns Unit

        // act
        receiver.validateAndMoveToProcessing(
            sender,
            csvString_2Records,
            emptyMap(),
            Options.None,
            emptyList(),
            true,
            true,
            ByteArray(0),
            "test.csv",
            metadata = metadata
        )

        // assert
        verify(exactly = 1) {
            engine.parseTopicReport(any(), any(), any())
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            actionHistory.trackLogs(emptyList())
            receiver.processAsync(any(), any(), any(), any())
        }
    }

    @Test
    fun `test COVID receiver validateAndMoveToProcessing, sync, with dupe check`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            TopicReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))
        val routeResult = emptyList<ActionLog>()

        every { engine.parseTopicReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.routeReport(any(), any(), any(), any(), any()) } returns routeResult
        every { SubmissionReceiver.doDuplicateDetection(any(), any(), any()) } returns Unit

        // act
        receiver.validateAndMoveToProcessing(
            sender,
            csvString_2Records,
            emptyMap(),
            Options.None,
            emptyList(),
            false,
            false,
            ByteArray(0),
            "test.csv",
            metadata = metadata
        )

        // assert
        verify(exactly = 1) {
            engine.parseTopicReport(any(), any(), any())
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            engine.routeReport(any(), any(), any(), any(), any())
            SubmissionReceiver.doDuplicateDetection(any(), any(), any())
        }

        verify(exactly = 2) {
            actionHistory.trackLogs(emptyList())
        }
    }

    @Test
    fun `test ELR receiver validateAndMoveToProcessing, async, with dupe check`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            ELRReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = false,
            customerStatus = CustomerStatus.ACTIVE
        )
        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))
        val routeResult = emptyList<ActionLog>()

        every { engine.parseTopicReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.routeReport(any(), any(), any(), any(), any()) } returns routeResult
        every { SubmissionReceiver.doDuplicateDetection(any(), any(), any()) } returns Unit
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { queueMock.sendMessage(elrConvertQueueName, any()) } returns Unit

        // act
        receiver.validateAndMoveToProcessing(
            sender,
            hl7_record,
            emptyMap(),
            Options.None,
            emptyList(),
            true,
            false,
            ByteArray(0),
            "test.csv",
            metadata = metadata
        )

        // assert
        verify(exactly = 1) {
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            SubmissionReceiver.doDuplicateDetection(any(), any(), any())
            actionHistory.trackLogs(emptyList())
            engine.insertProcessTask(any(), any(), any(), any())
            queueMock.sendMessage(elrConvertQueueName, any())
        }
    }

    @Test
    fun `test ELR receiver validateAndMoveToProcessing, inactive sender`() {
        testELRReceiverValidateAndMoveToProcessing(Report.Format.HL7, hl7_record)
    }
    @Test
    fun `test ELR receiver validateAndMoveToProcessing, HL7_BATCH format with header`() {
        testELRReceiverValidateAndMoveToProcessing(Report.Format.HL7_BATCH, hl7_record_batch_headers)
    }

    @Test
    fun `test ELR receiver validateAndMoveToProcessing, HL7_BATCH format no header, multiple records`() {
        testELRReceiverValidateAndMoveToProcessing(Report.Format.HL7_BATCH, hl7_multiple_records_no_headers)
    }

    private fun testELRReceiverValidateAndMoveToProcessing(format: Report.Format, content: String) {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            ELRReceiver(
                engine,
                actionHistory
            )
        )
        val sender = FullELRSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            allowDuplicates = true,
            customerStatus = CustomerStatus.INACTIVE
        )

        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))
        val routeResult = emptyList<ActionLog>()

        every { engine.parseTopicReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.routeReport(any(), any(), any(), any(), any()) } returns routeResult
        every { SubmissionReceiver.doDuplicateDetection(any(), any(), any()) } returns Unit
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { queueMock.sendMessage(elrConvertQueueName, any()) } returns Unit

        // act
        receiver.validateAndMoveToProcessing(
            sender,
            content,
            emptyMap(),
            Options.None,
            emptyList(),
            true,
            false,
            ByteArray(0),
            "test.csv",
            metadata = metadata
        )

        // assert
        verify(exactly = 1) {
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            SubmissionReceiver.doDuplicateDetection(any(), any(), any())
            actionHistory.trackLogs(emptyList())
            engine.insertProcessTask(any(), format.toString(), any(), any())
        }
        verify(exactly = 0) {
            queueMock.sendMessage(elrConvertQueueName, any())
        }
    }

    @Test
    fun `test ELR receiver validateAndMoveToProcessing with FHIR, inactive sender`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            ELRReceiver(
                engine,
                actionHistory
            )
        )
        val sender = FullELRSender(
            "Test Sender",
            "test",
            Sender.Format.FHIR,
            allowDuplicates = true,
            customerStatus = CustomerStatus.INACTIVE
        )

        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.FHIR, "test", ByteArray(0))
        val routeResult = emptyList<ActionLog>()

        every { engine.parseTopicReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.routeReport(any(), any(), any(), any(), any()) } returns routeResult
        every { SubmissionReceiver.doDuplicateDetection(any(), any(), any()) } returns Unit
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { queueMock.sendMessage(elrConvertQueueName, any()) } returns Unit

        // act
        receiver.validateAndMoveToProcessing(
            sender,
            File("src/test/resources/fhirengine/engine/valid_data.fhir").readText(),
            emptyMap(),
            Options.None,
            emptyList(),
            true,
            false,
            ByteArray(0),
            "test.csv",
            metadata = metadata
        )

        // assert
        verify(exactly = 1) {
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            actionHistory.trackLogs(emptyList())
            engine.insertProcessTask(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            queueMock.sendMessage(elrConvertQueueName, any())
            SubmissionReceiver.doDuplicateDetection(any(), any(), any())
        }
    }

    @Test
    fun `test ELR receiver validateAndMoveToProcessing with Bulk FHIR, inactive sender`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            ELRReceiver(
                engine,
                actionHistory
            )
        )
        val sender = FullELRSender(
            "Test Sender",
            "test",
            Sender.Format.FHIR,
            allowDuplicates = true,
            customerStatus = CustomerStatus.INACTIVE
        )

        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.FHIR, "test", ByteArray(0))
        val routeResult = emptyList<ActionLog>()

        every { engine.parseTopicReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.routeReport(any(), any(), any(), any(), any()) } returns routeResult
        every { SubmissionReceiver.doDuplicateDetection(any(), any(), any()) } returns Unit
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { queueMock.sendMessage(elrConvertQueueName, any()) } returns Unit

        // act
        receiver.validateAndMoveToProcessing(
            sender,
            bulk_fhir_bundles,
            emptyMap(),
            Options.None,
            emptyList(),
            true,
            false,
            ByteArray(0),
            "test.csv",
            metadata = metadata
        )

        // assert
        verify(exactly = 1) {
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            actionHistory.trackLogs(emptyList())
            engine.insertProcessTask(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            queueMock.sendMessage(elrConvertQueueName, any())
            SubmissionReceiver.doDuplicateDetection(any(), any(), any())
        }
    }

    @Test
    fun `test ELR receiver validateAndMoveToProcessing, invalid hl7`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            ELRReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )
        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))
        val routeResult = emptyList<ActionLog>()

        every { engine.parseTopicReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.routeReport(any(), any(), any(), any(), any()) } returns routeResult
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { queueMock.sendMessage(elrConvertQueueName, any()) } returns Unit

        // act
        var exceptionThrown = false
        try {
            receiver.validateAndMoveToProcessing(
                sender,
                "bad_data",
                emptyMap(),
                Options.None,
                emptyList(),
                true,
                true,
                ByteArray(0),
                "test.csv",
                metadata = metadata
            )
        } catch (ex: ActionError) {
            exceptionThrown = true
        }

        // assert
        assertTrue(exceptionThrown)

        verify(exactly = 0) {
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            actionHistory.trackLogs(emptyList())
            engine.insertProcessTask(any(), any(), any(), any())
            queueMock.sendMessage(elrConvertQueueName, any())
        }
    }

    @Test
    fun `test ELR receiver validateAndMoveToProcessing, invalid message type hl7`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            ELRReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )
        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))
        val routeResult = emptyList<ActionLog>()

        every { engine.parseTopicReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.routeReport(any(), any(), any(), any(), any()) } returns routeResult
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { queueMock.sendMessage(elrConvertQueueName, any()) } returns Unit

        // act / assert
        assertThat {
            receiver.validateAndMoveToProcessing(
                sender,
                hl7_record_bad_type,
                emptyMap(),
                Options.None,
                emptyList(),
                true,
                true,
                ByteArray(0),
                "test.csv",
                metadata = metadata
            )
        }.isFailure()

        verify(exactly = 0) {
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            actionHistory.trackLogs(emptyList())
            engine.insertProcessTask(any(), any(), any(), any())
            queueMock.sendMessage(elrConvertQueueName, any())
        }
    }

    @Test
    fun `test validation receiver validateAndRoute, error on parsing`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.none))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            ValidationReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )
        val actionLogs = ActionLogger()
        val itemLogger = actionLogs.getItemLogger(1)
        itemLogger.error(InvalidHL7Message("Invalid HL7 file"))
        val readResult = ReadResult(report, actionLogs)

        every { engine.parseTopicReport(any(), any(), any()) } returns readResult
        every { SubmissionReceiver.doDuplicateDetection(any(), any(), any()) } returns Unit

        // act / assert
        assertThat {
            receiver.validateAndRoute(
                sender,
                hl7_record_bad_type,
                emptyMap(),
                emptyList(),
                true
            )
        }.isFailure()

        verify(exactly = 0) {
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            actionHistory.trackLogs(emptyList())
            engine.insertProcessTask(any(), any(), any(), any())
            queueMock.sendMessage(elrConvertQueueName, any())
        }
    }

    @Test
    fun `test validation receiver validateAndRoute, warning on parsing`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.none))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            ValidationReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )
        val actionLogs = ActionLogger()
        val itemLogger = actionLogs.getItemLogger(1)
        itemLogger.warn(InvalidHL7Message("Invalid HL7 file"))
        val readResult = ReadResult(report, actionLogs)

        every { engine.parseTopicReport(any(), any(), any()) } returns readResult
        every { SubmissionReceiver.doDuplicateDetection(any(), any(), any()) } returns Unit

        // act / assert
        assertThat {
            receiver.validateAndRoute(
                sender,
                hl7_record,
                emptyMap(),
                emptyList(),
                false
            )
        }.isSuccess()

        verify(exactly = 0) {
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            engine.insertProcessTask(any(), any(), any(), any())
            queueMock.sendMessage(elrConvertQueueName, any())
        }
    }

    @Test
    fun `test validation receiver validateAndRoute, happy path`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.none))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            ValidationReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )
        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)

        every { engine.parseTopicReport(any(), any(), any()) } returns readResult
        every { SubmissionReceiver.doDuplicateDetection(any(), any(), any()) } returns Unit

        // act / assert
        assertThat {
            receiver.validateAndRoute(
                sender,
                hl7_record_bad_type,
                emptyMap(),
                emptyList(),
                false
            )
        }.isSuccess()

        verify(exactly = 0) {
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            engine.insertProcessTask(any(), any(), any(), any())
            queueMock.sendMessage(elrConvertQueueName, any())
        }
    }

    @Test
    fun `test getSubmissionReceiver`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )

        val result = SubmissionReceiver.getSubmissionReceiver(sender, engine, actionHistory)
        assertThat(result).isInstanceOf(TopicReceiver::class.java)

        val fullELRSender = FullELRSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            allowDuplicates = true
        )
        val fullELRResult = SubmissionReceiver.getSubmissionReceiver(fullELRSender, engine, actionHistory)
        assertThat(fullELRResult).isInstanceOf(ELRReceiver::class.java)
    }
}