# Portal Poster

## Purpose
Zenhub user story [17978 - Ability to resend multiple reports through ReportStream's report API](https://app.zenhub.com/workspaces/reportstream-om-67feaaa15ecf34000f4cc206/issues/gh/cdcgov/prime-reportstream/17978) articulates the need to have an approach to re-process a cohort of SimpleReport CSV files based on a separate CSV file of identifiers used as input.  This project addresses the part of the process that posts the files to ReportStream.  (While this particular project is targeted specifically at processing CSV files for the COVID Pipeline, the approach could easily be modified to process HL7 or FHIR files as well.)

## Usage
**Portal Poster** is a Spring Boot command line application.  It can be configured using any of the myriad of options outlined in Spring Boot's [Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) page.  The simplest (and suggested) method is to set the desired values in the **application.properties** file of the project.

It is assumed that the input CSV file takes the following form:

```
Report ID,Min of Created At: Minute,Min of Organization - Org → Organization Name
8e48a04b-a841-4a40-9800-38e7af10b089,"December 2, 2024, 9:54 PM",Children's Minnesota
5663ef27-71c8-46d0-9260-6aae162c6526,"December 2, 2024, 11:45 PM",Premiere medical center
...
```

The files that will be sent to ReportStream must be manually retrieved through Azure Storage Explorer.  (Connect using the appropriate credentials, navigate to the correct folder, download all files.)

The steps for running the application are as follows:

1. Configure the desired values in the application.properties file.
2. Compile the application.
3. Run the application and evaluate the results.
4. Use Portal Poster Reporter to track progress.

### Configure the Desired Values in the application.properties File
The following properties are used by **Portal Poster**:

**gov.cdc.prime.router.poster.skip.first.line** - Set this to **true** to skip the first line of the input file (usually the descriptive headers for the individual data columns).

**gov.cdc.prime.router.poster.url** - The full URL associated with the ReportStream endpoint (e.g., http://localhost:7071/api/reports for local testing).

**gov.cdc.prime.router.poster.client** - The value to be used in the **client** HTTP header (e.g., simple_report.csvuploader).

**gov.cdc.prime.router.poster.x.functions.key** - The Azure key used for authentication (ignored for local testing).

**gov.cdc.prime.router.poster.wait.time.in.seconds** - The number of seconds to wait between each HTTP invocation (e.g., 30).

**gov.cdc.prime.router.poster.payload.directory** - The canonical file path to the directory on the local machine where the files to be re-processed are stored (e.g., /Users/bill/projects/report-stream/user-stories/17170/archive).

**gov.cdc.prime.router.poster.input.file** - The canonical path to the location of the input file (e.g., /Users/bill/projects/report-stream/user-stories/17978/query\_result\_2025-04-04t18\_57\_23.876244z.csv).


### Compile The Application
Simpley execute `./gradlew clean build` in the project directory.

### Run the Application and Evaluate the Results
Execute `./gradlew bootRun` from the command line (or from within your IDE).  Output will be logged both to the console and to `portal-poster.log`.

### Use Portal Poster Reporter to Track Progress
**Portal Poster Reporter** uses the the `YYYYMMDD-HHMMSS-portal-poster-history.csv` file created by **Portal Poster** to call the ReportStream submission history endpoint so that the progress of each submission may be tracked.
