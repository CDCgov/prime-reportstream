package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.eachHref
import it.skrape.selects.html5.a
import java.io.File
import java.io.FileOutputStream
import java.net.URL

private const val cdcLOINCTestCodeMappingPage = "https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html"
private const val livdSARSCov2File = "LIVD-SARS-CoV-2"

/**
 * LivdTableDownload is the command line interface for the livd-table-download command. It parses the command line
 * for option given as below.
 */
class LivdTableDownload() : CliktCommand(
    name = "livd-table-download",
    help = """
    livd-table downloads the latest LOINC test data, so it can be ingested automatically. 
    
    It looks for the LIVD-SAR-CoV-2-yyyy-MM-dd.xlsx file from $cdcLOINCTestCodeMappingPage.
	If the file is found, it downloads the file into the directory specified by the --output-dir <path> option.
	If the option is not specified, it will download the file to ./build directory.
        
    Example:
      The following command will download the latest LIVD-SARS-CoV-2-yyyy-MM-dd.xlsx from the above URL.  It will 
      store the file under the ./junk directory.
      
         ./prime livd-table --output-dir ./junk 
    """
) {
    private val defaultOutputDir = "./build"
    private val outputDir by option(
        "--output-dir",
        metavar = "<path>",
        help = "interpret `--output` relative to this directory (default: \"$defaultOutputDir\")"
    ).default(defaultOutputDir)

    override fun run() {
        downloadFile(outputDir)
    }

    /**
     *  The downloadFile downloads the latest LOINC test data, so it can be ingested automatically.
     *  It looks for the LIVD-SAR-CoV-2-yyyy-MM-dd.xlsx file from https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html.
     *  If the file is found, it downloads the file into the directory specified by the --output-dir <path> option.
     *  If the option is not specified, it will download the file to ./build directory.
     */
    private fun downloadFile(outputDir: String): Boolean {
        //
        // Get the link to the LIVD-SARS-CoV-2-yyyy-MM-dd.xlsx file
        //
        var livdFile = search(cdcLOINCTestCodeMappingPage, livdSARSCov2File)
        if (livdFile.isEmpty()) {
            print("Error: unable to find LOINC code data to download!\n")
            return false
        } else {
        }
        var livdFileUrl = "https://cdc.gov/" + livdFile.get(0)

        //
        // Create the local file in the specified directory
        //
        val localFilename = livdFileUrl.split('/').filter { it.contains(livdSARSCov2File) }.get(0)
        val outputFile = File(outputDir, localFilename)

        //
        // Read the file from the website and store it in local directory
        //
        URL(livdFileUrl).openStream().use { input ->
            if (outputFile.exists()) {
                val c = prompt("$outputFile file is already existed: You want to overwrite it (y/n)?", "n")
                if (c?.lowercase() == "n") {
                    return false
                } else {
                    print("The $outputFile file is overwriting.\n")
                }
            } else {
                print("The $outputFile file is downloaded.\n")
            }

            try {
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            } catch (e: Exception) {
                print("Error: $e")
                return false
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
    private fun search(urlToSearch: String, partialHref: String): List<String> {
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