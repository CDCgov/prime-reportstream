package gov.cdc.prime.router.serializers.datatests

import org.apache.commons.io.filefilter.SuffixFileFilter
import java.io.File
import kotlin.test.fail

interface ConversionTest {
    /**
     * Gets a list of test files from the given [path].
     * @return a list of absolute pathnames to the test files
     */
    fun getTestFiles(path: String, suffix: String): List<String> {
        val files = ArrayList<String>()
        val fullDirPath = this.javaClass.getResource(path)?.path
        if (!fullDirPath.isNullOrBlank()) {
            val dir = File(fullDirPath)
            if (dir.exists()) {
                val filenames = dir.list(SuffixFileFilter(suffix))
                filenames?.forEach { files.add("$fullDirPath/$it") }
                if (files.isEmpty()) fail("There are no HL7 files present in $fullDirPath")
            } else {
                fail("Directory $path does not exist in the classpath.")
            }
            files.forEach { println(it) }
        } else {
            fail("Unable to obtain the path to the test files.")
        }
        return files
    }
}