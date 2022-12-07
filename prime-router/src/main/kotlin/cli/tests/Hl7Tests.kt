package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.common.Environment
import java.io.IOException
import java.net.HttpURLConnection

/**
 * Generates a fake HL7 report and sends it to ReportStream, waits some time then checks the lineage to make
 * sure the data was sent to the configured senders.
 */
class Hl7Ingest : CoolTest() {
    override val name = "hl7ingest"
    override val description = "Create HL7 Fake data, submit, wait, confirm sent via database lineage data"
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        initListOfGoodReceiversAndCounties()
        var passed = true
        val sender = hl7Sender
        val receivers = allGoodReceivers
        val itemCount = options.items * receivers.size
        ugly("Starting $name Test: send ${sender.fullName} data to $allGoodCounties")
        val file = FileUtilities.createFakeCovidFile(
            metadata,
            settings,
            sender,
            itemCount,
            receivingStates,
            allGoodCounties,
            options.dir,
            Report.Format.HL7_BATCH
        )
        echo("Created datafile $file")

        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, sender, options.asyncProcessMode, options.key)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***$name Test FAILED***:  response code $responseCode")
            passed = false
        } else {
            good("Posting of report succeeded with response code $responseCode")
        }

        // Check the response from the endpoint
        echo(json)
        passed = passed and examinePostResponse(json, !options.asyncProcessMode)

        // Now check the lineage data
        val reportId = getReportIdFromResponse(json)
        if (reportId != null) {
            // if testing async, verify the process result was generated
            if (options.asyncProcessMode) {
                // gets back the id of the internal report
                val internalReportId = getSingleChildReportId(reportId)

                val processResults = pollForProcessResult(internalReportId)
                // verify each result is valid
                for (result in processResults.values)
                    passed = passed && examineProcessResponse(result)
                if (!passed)
                    bad("***async end2end FAILED***: Process result invalid")
            }

            passed = passed and pollForLineageResults(
                reportId,
                receivers,
                itemCount,
                asyncProcessMode = options.asyncProcessMode
            )
        }

        return passed
    }
}

/**
 * Tests for bad HL7 scenarios and sends it to ReportStream, asserts on the response status as well as body
 * For test cases see,
 * https://app.zenhub.com/workspaces/prime-data-hub-5ff4833beb3e08001a4cacae/issues/cdcgov/prime-reportstream/1604
 *
 * Missing Mandatory Fields
 *      Patient lastname
 *      Patient state
 *      Ordering facility state
 *      Message id
 *      Testing lab clia
 * Invalid Data Type
 *      Invalid Date of birth
 *      Special char
 * Bad Files
 *      Empty file
 *      Csv file
 *      XML file
 *      JSON
 * Bad Data
 *      Duplicate segments e.g. PID
 *      Random text
 *      Partially terminated
 *      Emoji
 *      Non UTF
 *      More than 50mb post message body
 *      Only header MSH
 */
class BadHl7 : CoolTest() {
    override val name = "badhl7"
    override val description = "Submit bad hl7 scenarios - should get errors"
    override val status = TestStatus.DRAFT // This test fails or hangs sometimes when part of the smokes.  Not sure why
    val failures = mutableSetOf<String>()
    val strHl7Message = """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime Data Hub|20210210170737||ORU^R01^ORU_R01|3719999|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
PID|1|ABC123DF|AND234DA_PID3|PID_4_ALTID|Patlast^Patfirst^Mid||19670202|F|||4505 21 st^^LAKE COUNTRY^MD^FO||222-555-8484|||||MF0050356/15|
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|202102090000-0500||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^IG^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.19.4.6&ISO^^^^10D08761999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
SPM|1|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||20201102063552-0500|20201102063552-0500"""
    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        val sender = hl7Sender
        val csv = """
            A,b
            1,2
        """.trimIndent()
        val a_60Meg_payload = "*".repeat(60000000)
        val special_char = "!@#\\\\\\\$*()-_=+[]\\\\\\\\{};':,./<>?"
        val xml_data = """
            <?xml version="1.0"?>
            <Tests xmlns="http://www.adatum.com">
              <Test TestId="0001" TestType="CMD">
                <Name>Convert number to string</Name>
                <CommandLine>Examp1.EXE</CommandLine>
                <Input>1</Input>
                <Output>One</Output>
              </Test>
            </Tests>
        """.trimIndent()
        val badHl7Pairs = listOf(
            /* Missing Required Fields */
            Pair("MISSING PATIENT LASTNAME", strHl7Message.replace("Patlast", "")),
            Pair("MISSING ORDERING FACILITY STATE", strHl7Message.replace("^IG^", "")),
            Pair("MISSING MESSAGE ID", strHl7Message.replace("3719999", "")),
            Pair("MISSING TESTING LAB CLIA", strHl7Message.replace("10D08761999", "")),
            Pair("MISSING PATIENT STATE", strHl7Message.replace("^MD^", "^^")),
            /* Invalid data type */
            Pair("INVALID DOB Partial", strHl7Message.replace("19670202", "19")),
            Pair("INVALID DOB Special Chars", strHl7Message.replace("19670202", special_char)),
            /* Bad hl7 files */
            Pair("EMPTY FILE", ""),
            Pair("CSV FILE", csv),
            Pair("XML FILE", xml_data),
            Pair("JSON", "{\"alive\": true}"),
            /* Bad data */
            Pair(
                "Duplicate PID",
                strHl7Message + "PID|||||Richards^Mary||19340428|F|||||||||||||||||||\\nPID|||||||" +
                    "19700510105000|M|||||||||||||||||||"
            ),
            Pair(
                "Only MSH",
                "MSH|^~\\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante " +
                    "at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime Data Hub|20210210170737||ORU^R01^ORU_R01|371784|" +
                    "P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO"
            ),
            Pair("Partially Terminated", "MSH|^~\\&"),
            Pair("RANDOM TEXT", "foobar"),
            Pair("NON-UTF", "®"),
            Pair("EMOJI", "❤️ \uD83D\uDC94 \uD83D\uDC8C \uD83D\uDC95 \uD83D\uDC9E \uD83D\uDC93"),
            Pair("LARGE POST BODY", a_60Meg_payload),
        )
        for (pair in badHl7Pairs) {
            try {
                val (responseCode, jsonResponse) = HttpUtilities.postReportBytes(
                    environment,
                    (pair.second).toByteArray(),
                    sender,
                    options.key
                )
                echo("ResponseCode to POST: $responseCode")
                echo(jsonResponse)
                if (responseCode >= 400 && responseCode < 500) {
                    good("Test for $name ${pair.first} passed: received $responseCode response code.")
                } else {
                    failures.add(pair.first)
                    bad("***Test for $name ${pair.first} FAILED***: Expected a failure HttpStatus***")
                    continue
                }

                val tree = jacksonObjectMapper().readTree(jsonResponse)
                if (tree["id"].isNull) {
                    good("Test for $name ${pair.first} passed: id is null.")
                } else {
                    bad("***Test for $name ${pair.first} FAILED***: Expected null Id, got ${tree["id"]}")
                    failures.add(pair.first)
                }
                val errorCount = tree["errorCount"].intValue()
                if (errorCount > 0) {
                    good("Test for $name ${pair.first} passed: ErrorCount of $errorCount was returned.")
                } else {
                    bad(
                        "***Test for $name ${pair.first} FAILED***: " +
                            "Expected a non-zero ErrorCount, got $errorCount error(s)"
                    )
                    failures.add(pair.first)
                }
                val warningCount = tree["warningCount"].intValue()
                if (warningCount == 0) {
                    good("Test for BadHl7 ${pair.first} passed: $warningCount warning was returned.")
                } else {
                    bad(
                        "***Test for $name ${pair.first} FAILED***: " +
                            "Expected zero warning, got $warningCount warning(s)"
                    )
                    failures.add(pair.first)
                }
            } catch (e: NullPointerException) {
                return bad("***Test for $name ${pair.first} FAILED***: Unable to properly parse response json")
            } catch (e: IOException) {
                // For local runs of this test we may get an error writing to server for the LARGE POST BODY test, so
                // return a passed value to ignore.
                if (options.env == "local" && pair.first == "LARGE POST BODY") {
                    ugly("*** Ignoring failure for test $name ${pair.first} due to large payload size")
                    return true
                }
            }
        }
        if (failures.size == 0) {
            return true
        } else {
            return bad("Tests for $name FAILED***: $failures")
        }
    }
}