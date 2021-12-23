package gov.cdc.prime.router

import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.eachHref
import it.skrape.selects.html5.a
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.Scanner

private const val cdcLOINCTestCodeMappingPage = "https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html"
private const val livdSARSCov2File = "LIVD-SARS-CoV-2"

/**
 *  The LivdTable class downloads the latest LOINC test data, so it can be ingested automatically.
 *  It looks for the LIVD-SAR-CoV-2-yyyy-MM-dd.xlsx file from https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html.
 *  If the file is found, it downloads the file into the directory specified by the --output-dir <path> option.
 *  If the option is not specified, it will download the file to ./build directory.
 */
data class LivdTable(val outputDir: String) {
    fun downloadFile(): Boolean {
        //
        // Get the link to the LIVD-SARS-CoV-2-yyyy-MM-dd.xlsx file
        //
        var livdFile = search(cdcLOINCTestCodeMappingPage, livdSARSCov2File)
        if (livdFile.isEmpty()) {
            print("Error: There is no LOINC code data to download!\n")
            return false
        } else {}
        var livdFileUri = livdFile.get(0)

        //
        // Create the local file in the specified directory
        //
        val localFilename = livdFileUri.split('/').filter { it.contains(livdSARSCov2File) }.get(0)
        val outputFile = File(outputDir, localFilename)

        //
        // Read the file from the website and store it in local directory
        //
        URL("https://cdc.gov/$livdFileUri").openStream().use { input ->
            if (outputFile.exists()) {
                print("$outputFile file is already existed: You want to overwrite it (y/N)? ")
                val c = Scanner(System.`in`).next().lowercase()
                if (c == "y") {
                    print("The $outputFile file is overwriting.\n")
                    try {
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    } catch (e: Exception) {
                        print("Error: $e")
                        return false
                    }
                }
            } else {
                try {
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                    print("The $outputFile file is downloaded.\n")
                } catch (e: Exception) {
                    print("Error: $e")
                    return false
                }
            }
        }
        return true
    }

    /**
     * Search - searching for the URI link to the LIVID-SARS-CoV-2-yyyy-MM-dd file
     * @param - urlToSearch is the URL to the web page that we will search.
     * @param - partialHref is the substring that we are searching for.
     * @return - List of the URI that contain the substring
     */
    public fun search(urlToSearch: String, partialHref: String): List<String> {
        val allLinks =
            skrape(HttpFetcher) {
                request {
                    url = urlToSearch
                }
                response {
                    htmlDocument {
                        a {
                            findAll {
                                eachHref
                            }
                        }
                    }
                }
            }
        return allLinks.filter { it.contains(partialHref) }
    }
}