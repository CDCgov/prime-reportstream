package gov.cdc.prime.router

import java.io.File

data class CsvComparer(val fileOnePath: String, val fileTwoPath: String, val recordId: String = "Patient_Id") {
    fun compareFiles(): Boolean {
        val fileOne = File(fileOnePath)
        val fileTwo = File(fileTwoPath)
        if (!fileOne.exists()) error("File $fileOne does not exist")
        if (!fileTwo.exists()) error("File $fileTwo does not exist")
        // A bit of a hack:  diff the two files.
        val fileOneLines = fileOne.readLines()
        val fileTwoLines = fileTwo.readLines()

        val testLines = convertFileToMap(fileOneLines, recordId)
        val expectedLines = convertFileToMap(fileTwoLines, recordId)
        val headerRow = fileOneLines[0].split(",")

        // let's first compare the keys
        val keyMessages = compareKeysOfMaps(expectedLines, testLines)

        // let's compare our lines now
        val linesInError = compareLinesOfMaps(expectedLines, testLines, headerRow)

        if (linesInError.count() > 0) {
            println(keyMessages.first)
            println(keyMessages.second)
            linesInError.forEach { println(it) }
            return false
        } else {
            println("Files provided match! Well done!")
        }

        return true
    }

    fun compareKeysOfMaps(expected: Map<String, Any?>, actual: Map<String, Any?>): Pair<String, String> {
        val actualKeys = actual.keys.toSet()
        val expectedKeys = expected.keys.toSet()
        return Pair(
            "There are keys in actual that are not in expected: ${actualKeys.minus(expectedKeys).joinToString { "," }}",
            "There are keys in expected that are not present in actual: ${expectedKeys.minus(actualKeys).joinToString { "," }}"
        )
    }

    fun compareLinesOfMaps(
        expected: Map<String, Any?>,
        actual: Map<String, Any?>,
        headerRow: List<String>? = null
    ): List<String> {
        val linesInError = mutableListOf<String>()

        for (expectedKey in expected.keys) {
            if (!actual.keys.contains(expectedKey)) error("Key $expectedKey missing in actual dataset")

            val actualLines: List<String>? = actual[expectedKey] as? List<String>
            val expectedLines: List<String>? = expected[expectedKey] as? List<String>

            if (actualLines == null) error("Cast failed for actual values")
            if (expectedLines == null) error("Cast failed for expected values")

            for ((i, v) in expectedLines.withIndex()) {
                if (v != actualLines[i]) {
                    val header = headerRow?.get(i) ?: "$i"
                    val message = "Patient_ID $expectedKey differed at $header. Expected '$v' but found '${actualLines[i]}'."
                    linesInError.add(message)
                    println(message)
                }
            }
        }

        return linesInError.toList()
    }

    fun convertFileToMap(
        lines: List<String>,
        recordId: String = "Patient_ID",
        skipHeader: Boolean = true,
        delimiter: String = ","
    ): Map<String, Any?> {
        val expectedLines = mutableMapOf<String, Any?>()
        val headerLine = lines[0]
        var recordIdIndex = 0
        var skippedHeader = false

        for (expectedResultsLine in lines) {
            // if a header is passed in and we need to skip it, then check our control values
            if (skipHeader && !skippedHeader) {
                // while we're in the header, find our record id index, which will be our map key
                val headerValues = headerLine.split(",")
                recordIdIndex = headerValues.indexOf(recordId)
                // reset our control variable
                skippedHeader = true
                continue
            }

            if (recordIdIndex == -1) error("Key provided for recordId was not found in header of file. Expected $recordId")

            val splitLine = expectedResultsLine.split(delimiter)
            if (!expectedLines.containsKey(splitLine[recordIdIndex])) {
                expectedLines[splitLine[recordIdIndex]] = splitLine
            } // TODO: should we throw an error if we are adding the same key twice?
        }

        // remove mutability and return
        return expectedLines.toMap()
    }
}