package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import kotlin.test.Test

class Hl7UtilitiesTests {

    @Test
    fun `test cut multiple`() {
        val input = """
        FHS|^~\&|||0.0.0.0.1|0.0.0.0.1|202201042030-0800
        BHS|^~\&|||0.0.0.0.1|0.0.0.0.1|202201042030-0800
        MSH|^~\&||Any facility USA^27D6667459^CLIA|0.0.0.0.1|0.0.0.0.1|20220104203055.2282-0800||ORU^R01^ORU_R01|132035|P|2.5.1|||NE|NE|USA|UNICODE UTF-8
        SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20220104
        PID|1||bejcw9e^^^Any lab USA&27D6667459&CLIA^4ottsq769^Any facility USA&27D6667459&CLIA||Rice^Gene^Kristopher^III^^^4gbufq77q||19600717|U||1002-5^American Indian or Alaska Native^HL70005^^^^2.5.1|9871 Jerde Viaduct^^^IG^^hjfy9||(256)718-4532^PRN^PH^^1^256^7184532~^NET^Internet^misti.littel@email.com|||||||307-66-6930||U^Unknown^HL70189^^^^2.9|||||||20211231|UNK|||||||||561^Native Village of Venetie Tribal Government (Arcti^HL70171
        ORC|RE|036432^Any lab USA^86D6877918^CLIA|846671^Any lab USA^86D6877918^CLIA|073619^7gmz4^19D8531761^CLIA||||||||1700490836^Lebsack^Berry^Claire^^^^^0.0.0.0.1^^^^065a9od||(264)134-5163^WPN^PH^^1^264^1345163|202112311311-0800||||||Any facility USA^L|1805 Sherice Views^^^IG^^^^HL7_NULL|(265)589-5832^WPN^PH^^1^265^5895832|455 Aida Lodge^^^IG
        OBR|1|036432^Any lab USA^86D6877918^CLIA|846671^Any lab USA^86D6877918^CLIA|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^nkns19si^^^^2.68|||202112262314-0800|202112262314-0800||||||||1700490836^Lebsack^Berry^Claire^^^^^0.0.0.0.1^^^^065a9od|(264)134-5163^WPN^PH^^1^264^1345163|||||202112290515-0800|||C^Corrected, not final^HL70123||||||u2j4cxpqm
        OBX|1|CWE^Coded With Exceptions^HL70125|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^5mtzic0k^^^^2.68|766594|260415000^Not detected^SCT|guzb8|Normal|null^No range defined, or normal ranges don't apply^HL70078^^^^2.7|||C^Results entered -- not verified^HL70085|||202112262314-0800|86D6877918^Any lab USA^CLIA||^BD Veritor System for Rapid Detection of SARS-CoV-2*^^^^^2.68|BD Veritor Plus System_Becton Dickinson^^MNI|202112271143-0800||||Any lab USA^^^^^0.0.0.0.1^XX^^^86D6877918|149 Haywood Crossroad^^^IG
        SPM|1|415711&Any facility USA&27D6667459&CLIA^846671&Any facility USA&27D6667459&CLIA||445297001^Swab of internal nose^SCT^^^^2.67|||QC5^Quality Control For Micro^HL70048|71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01||nnqm5e|L|||61c1owc|||202112262314-0800|202112310112-0800
        MSH|^~\&||Any facility USA^29D0071355^CLIA|0.0.0.0.1|0.0.0.0.1|20220104203055.2282-0800||ORU^R01^ORU_R01|867013|P|2.5.1|||NE|NE|USA|UNICODE UTF-8
        SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20220104
        PID|1||z76uv9mb^^^Any lab USA&29D0071355&CLIA^25b5e^Any facility USA&29D0071355&CLIA||VonRueden^Joslyn^Ernie^^^^2fmxl||19480422|A||2106-3^White^HL70005^^^^2.5.1|944 Amos Mount^^^IG^^peenodx89||(242)436-3444^PRN^PH^^1^242^4363444~^NET^Internet^jayson.feest@email.com|||||||020-62-2478||H^Hispanic or Latino^HL70189^^^^2.9|||||||20211229|Y|||||||||75^Delaware Tribe of Indians, Oklahoma^HL70171
        ORC|RE|735653^Any lab USA^85D9885142^CLIA|172016^Any lab USA^85D9885142^CLIA|412908^rrv6b21^47D4661348^CLIA||||||||1726466463^Lang^Juan^Lorrine^^^^^0.0.0.0.1^^^^2iydk1||(238)338-9786^WPN^PH^^1^238^3389786|202112290617-0800||||||Any facility USA^L|3583 Landon Groves^^^IG^^^^HL7_NULL|(273)062-8365^WPN^PH^^1^273^0628365|926 Janita Points^^^IG
        OBR|1|735653^Any lab USA^85D9885142^CLIA|172016^Any lab USA^85D9885142^CLIA|95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^gve4r3733^^^^2.68|||202201011137-0800|202201011137-0800||||||||1726466463^Lang^Juan^Lorrine^^^^^0.0.0.0.1^^^^2iydk1|(238)338-9786^WPN^PH^^1^238^3389786|||||202201030112-0800|||C^Final results^HL70123||||||x1lm63
        OBX|1|CWE^Coded With Exceptions^HL70125|95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^s5us9un^^^^2.68|928964|260373001^Detected^SCT|8zkz997|Normal|S^Susceptible. Indicates for microbiology susceptibilities only.^HL70078^^^^2.7|||C^Results status change to final without retransmitting results already sent as ‘preliminary.’  E.g., radiology changes status from preliminary to final^HL70085|||202201011137-0800|85D9885142^Any lab USA^CLIA||^LumiraDx SARS-CoV-2 Ag Test*^^^^^2.68|LumiraDx Platform_LumiraDx^^MNI|202201011031-0800||||Any lab USA^^^^^0.0.0.0.1^XX^^^85D9885142|8808 Waelchi Circles^^^IG
        SPM|1|435180&Any facility USA&29D0071355&CLIA^172016&Any facility USA&29D0071355&CLIA||871810001^Mid-turbinate nasal swab^SCT^^^^2.67|||EPLA^Environmental, Plate^HL70048|71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01||2a5f3x|Q|||m21ehnz8d|||202201011137-0800|202112280640-0800
        MSH|^~\&||Any facility USA^40D0447178^CLIA|0.0.0.0.1|0.0.0.0.1|20220104203055.2282-0800||ORU^R01^ORU_R01|749583|P|2.5.1|||NE|NE|USA|UNICODE UTF-8
        SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20220104
        PID|1||yfwbo2^^^Any lab USA&40D0447178&CLIA^qokuptxq^Any facility USA&40D0447178&CLIA||Considine^Odis^Laureen^^^^fyhu6v1em||20141031|F||2076-8^Native Hawaiian or Other Pacific Islander^HL70005^^^^2.5.1|79080 Clifford Mill^^^IG^^lndi9zmj9||(292)094-0461^PRN^PH^^1^292^0940461~^NET^Internet^jed.schinner@email.com|||||||281-06-8836||N^Non Hispanic or Latino^HL70189^^^^2.9|||||||20211226|UNK|||||||||212^Pueblo of Isleta, New Mexico^HL70171
        ORC|RE|017693^Any lab USA^46D8025914^CLIA|039301^Any lab USA^46D8025914^CLIA|922137^72xtc^75D2640873^CLIA||||||||1907356269^Ebert^Zena^Cordie^^^^^0.0.0.0.1^^^^xmonq||(205)726-1790^WPN^PH^^1^205^7261790|202201032229-0800||||||Any facility USA^L|733 Von Parkways^^^IG^^^^HL7_NULL|(230)683-3936^WPN^PH^^1^230^6833936|0610 Tai Vista^^^IG
        OBR|1|017693^Any lab USA^46D8025914^CLIA|039301^Any lab USA^46D8025914^CLIA|95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^isjnlo^^^^2.68|||202201030200-0800|202201030200-0800||||||||1907356269^Ebert^Zena^Cordie^^^^^0.0.0.0.1^^^^xmonq|(205)726-1790^WPN^PH^^1^205^7261790|||||202112280451-0800|||F^No results available; specimen received, procedure incomplete^HL70123||||||24vjy
        OBX|1|CWE^Coded With Exceptions^HL70125|95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^l5jc4tctf^^^^2.68|708173|419984006^Inconclusive^SCT|nunfv|Abnormal|W^Worse--use when direction not relevant^HL70078^^^^2.7|||F^Record coming over is a correction and thus replaces a final result^HL70085|||202201030200-0800|46D8025914^Any lab USA^CLIA||^LumiraDx SARS-CoV-2 Ag Test*^^^^^2.68|LumiraDx Platform_LumiraDx^^MNI|202112311405-0800||||Any lab USA^^^^^0.0.0.0.1^XX^^^46D8025914|88069 Borer Trace^^^IG
        SPM|1|627792&Any facility USA&40D0447178&CLIA^039301&Any facility USA&40D0447178&CLIA||445297001^Swab of internal nose^SCT^^^^2.67|||NYP^Plate, New York City^HL70048|71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01||9jxvzxu44|L|||fm5fx1p|||202201030200-0800|202112261308-0800
        BTS|3
        FTS|1
        """.trimIndent().replace('\n', '\r')
        assertThat(Hl7Utilities.cut(input, listOf(1))).isEqualTo(
            """
            FHS|^~\&|||0.0.0.0.1|0.0.0.0.1|202201042030-0800
            BHS|^~\&|||0.0.0.0.1|0.0.0.0.1|202201042030-0800
            MSH|^~\&||Any facility USA^29D0071355^CLIA|0.0.0.0.1|0.0.0.0.1|20220104203055.2282-0800||ORU^R01^ORU_R01|867013|P|2.5.1|||NE|NE|USA|UNICODE UTF-8
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20220104
            PID|1||z76uv9mb^^^Any lab USA&29D0071355&CLIA^25b5e^Any facility USA&29D0071355&CLIA||VonRueden^Joslyn^Ernie^^^^2fmxl||19480422|A||2106-3^White^HL70005^^^^2.5.1|944 Amos Mount^^^IG^^peenodx89||(242)436-3444^PRN^PH^^1^242^4363444~^NET^Internet^jayson.feest@email.com|||||||020-62-2478||H^Hispanic or Latino^HL70189^^^^2.9|||||||20211229|Y|||||||||75^Delaware Tribe of Indians, Oklahoma^HL70171
            ORC|RE|735653^Any lab USA^85D9885142^CLIA|172016^Any lab USA^85D9885142^CLIA|412908^rrv6b21^47D4661348^CLIA||||||||1726466463^Lang^Juan^Lorrine^^^^^0.0.0.0.1^^^^2iydk1||(238)338-9786^WPN^PH^^1^238^3389786|202112290617-0800||||||Any facility USA^L|3583 Landon Groves^^^IG^^^^HL7_NULL|(273)062-8365^WPN^PH^^1^273^0628365|926 Janita Points^^^IG
            OBR|1|735653^Any lab USA^85D9885142^CLIA|172016^Any lab USA^85D9885142^CLIA|95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^gve4r3733^^^^2.68|||202201011137-0800|202201011137-0800||||||||1726466463^Lang^Juan^Lorrine^^^^^0.0.0.0.1^^^^2iydk1|(238)338-9786^WPN^PH^^1^238^3389786|||||202201030112-0800|||C^Final results^HL70123||||||x1lm63
            OBX|1|CWE^Coded With Exceptions^HL70125|95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^s5us9un^^^^2.68|928964|260373001^Detected^SCT|8zkz997|Normal|S^Susceptible. Indicates for microbiology susceptibilities only.^HL70078^^^^2.7|||C^Results status change to final without retransmitting results already sent as ‘preliminary.’  E.g., radiology changes status from preliminary to final^HL70085|||202201011137-0800|85D9885142^Any lab USA^CLIA||^LumiraDx SARS-CoV-2 Ag Test*^^^^^2.68|LumiraDx Platform_LumiraDx^^MNI|202201011031-0800||||Any lab USA^^^^^0.0.0.0.1^XX^^^85D9885142|8808 Waelchi Circles^^^IG
            SPM|1|435180&Any facility USA&29D0071355&CLIA^172016&Any facility USA&29D0071355&CLIA||871810001^Mid-turbinate nasal swab^SCT^^^^2.67|||EPLA^Environmental, Plate^HL70048|71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01||2a5f3x|Q|||m21ehnz8d|||202201011137-0800|202112280640-0800
            BTS|1
            FTS|1
            """.trimIndent().replace('\n', '\r')
        )
        assertThat { Hl7Utilities.cut(input, listOf(3)) }.isFailure()
        assertThat(Hl7Utilities.cut("", listOf())).isEmpty()
    }

    @Test
    fun `test cut single`() {
        val input = """
        MSH|^~\&||Any facility USA^29D0071355^CLIA|0.0.0.0.1|0.0.0.0.1|20220104203055.2282-0800||ORU^R01^ORU_R01|867013|P|2.5.1|||NE|NE|USA|UNICODE UTF-8
        SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20220104
        PID|1||z76uv9mb^^^Any lab USA&29D0071355&CLIA^25b5e^Any facility USA&29D0071355&CLIA||VonRueden^Joslyn^Ernie^^^^2fmxl||19480422|A||2106-3^White^HL70005^^^^2.5.1|944 Amos Mount^^^IG^^peenodx89||(242)436-3444^PRN^PH^^1^242^4363444~^NET^Internet^jayson.feest@email.com|||||||020-62-2478||H^Hispanic or Latino^HL70189^^^^2.9|||||||20211229|Y|||||||||75^Delaware Tribe of Indians, Oklahoma^HL70171
        ORC|RE|735653^Any lab USA^85D9885142^CLIA|172016^Any lab USA^85D9885142^CLIA|412908^rrv6b21^47D4661348^CLIA||||||||1726466463^Lang^Juan^Lorrine^^^^^0.0.0.0.1^^^^2iydk1||(238)338-9786^WPN^PH^^1^238^3389786|202112290617-0800||||||Any facility USA^L|3583 Landon Groves^^^IG^^^^HL7_NULL|(273)062-8365^WPN^PH^^1^273^0628365|926 Janita Points^^^IG
        OBR|1|735653^Any lab USA^85D9885142^CLIA|172016^Any lab USA^85D9885142^CLIA|95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^gve4r3733^^^^2.68|||202201011137-0800|202201011137-0800||||||||1726466463^Lang^Juan^Lorrine^^^^^0.0.0.0.1^^^^2iydk1|(238)338-9786^WPN^PH^^1^238^3389786|||||202201030112-0800|||C^Final results^HL70123||||||x1lm63
        OBX|1|CWE^Coded With Exceptions^HL70125|95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^s5us9un^^^^2.68|928964|260373001^Detected^SCT|8zkz997|Normal|S^Susceptible. Indicates for microbiology susceptibilities only.^HL70078^^^^2.7|||C^Results status change to final without retransmitting results already sent as ‘preliminary.’  E.g., radiology changes status from preliminary to final^HL70085|||202201011137-0800|85D9885142^Any lab USA^CLIA||^LumiraDx SARS-CoV-2 Ag Test*^^^^^2.68|LumiraDx Platform_LumiraDx^^MNI|202201011031-0800||||Any lab USA^^^^^0.0.0.0.1^XX^^^85D9885142|8808 Waelchi Circles^^^IG
        SPM|1|435180&Any facility USA&29D0071355&CLIA^172016&Any facility USA&29D0071355&CLIA||871810001^Mid-turbinate nasal swab^SCT^^^^2.67|||EPLA^Environmental, Plate^HL70048|71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01||2a5f3x|Q|||m21ehnz8d|||202201011137-0800|202112280640-0800
        """.trimIndent().replace('\n', '\r')
        assertThat(Hl7Utilities.cut(input, listOf(0))).isEqualTo(
            """
            MSH|^~\&||Any facility USA^29D0071355^CLIA|0.0.0.0.1|0.0.0.0.1|20220104203055.2282-0800||ORU^R01^ORU_R01|867013|P|2.5.1|||NE|NE|USA|UNICODE UTF-8
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20220104
            PID|1||z76uv9mb^^^Any lab USA&29D0071355&CLIA^25b5e^Any facility USA&29D0071355&CLIA||VonRueden^Joslyn^Ernie^^^^2fmxl||19480422|A||2106-3^White^HL70005^^^^2.5.1|944 Amos Mount^^^IG^^peenodx89||(242)436-3444^PRN^PH^^1^242^4363444~^NET^Internet^jayson.feest@email.com|||||||020-62-2478||H^Hispanic or Latino^HL70189^^^^2.9|||||||20211229|Y|||||||||75^Delaware Tribe of Indians, Oklahoma^HL70171
            ORC|RE|735653^Any lab USA^85D9885142^CLIA|172016^Any lab USA^85D9885142^CLIA|412908^rrv6b21^47D4661348^CLIA||||||||1726466463^Lang^Juan^Lorrine^^^^^0.0.0.0.1^^^^2iydk1||(238)338-9786^WPN^PH^^1^238^3389786|202112290617-0800||||||Any facility USA^L|3583 Landon Groves^^^IG^^^^HL7_NULL|(273)062-8365^WPN^PH^^1^273^0628365|926 Janita Points^^^IG
            OBR|1|735653^Any lab USA^85D9885142^CLIA|172016^Any lab USA^85D9885142^CLIA|95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^gve4r3733^^^^2.68|||202201011137-0800|202201011137-0800||||||||1726466463^Lang^Juan^Lorrine^^^^^0.0.0.0.1^^^^2iydk1|(238)338-9786^WPN^PH^^1^238^3389786|||||202201030112-0800|||C^Final results^HL70123||||||x1lm63
            OBX|1|CWE^Coded With Exceptions^HL70125|95209-3^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^s5us9un^^^^2.68|928964|260373001^Detected^SCT|8zkz997|Normal|S^Susceptible. Indicates for microbiology susceptibilities only.^HL70078^^^^2.7|||C^Results status change to final without retransmitting results already sent as ‘preliminary.’  E.g., radiology changes status from preliminary to final^HL70085|||202201011137-0800|85D9885142^Any lab USA^CLIA||^LumiraDx SARS-CoV-2 Ag Test*^^^^^2.68|LumiraDx Platform_LumiraDx^^MNI|202201011031-0800||||Any lab USA^^^^^0.0.0.0.1^XX^^^85D9885142|8808 Waelchi Circles^^^IG
            SPM|1|435180&Any facility USA&29D0071355&CLIA^172016&Any facility USA&29D0071355&CLIA||871810001^Mid-turbinate nasal swab^SCT^^^^2.67|||EPLA^Environmental, Plate^HL70048|71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01||2a5f3x|Q|||m21ehnz8d|||202201011137-0800|202112280640-0800
            """.trimIndent().replace('\n', '\r')
        )

        assertThat { Hl7Utilities.cut(input, listOf(1)) }.isFailure()
        assertThat(Hl7Utilities.cut("", listOf())).isEmpty()
    }
}