# Failed Report Resender

## Purpose
Zenhub user story [18484 - Automate Resending of Failed HHS Protect Reports](https://app.zenhub.com/workspaces/reportstream-om-67feaaa15ecf34000f4cc206/issues/gh/cdcgov/prime-reportstream/18484) articulates the need to have an approach to re-send a cohort of reports that failed last-mile delivery.  This project resends (via the ReportStream API) each report of that cohort.

## Usage
**Failed Report Resender** is a Spring Boot command line application.  It can be configured using any of the myriad of options outlined in Spring Boot's [Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) page.  The simplest (and suggested) method is to set the desired values in the **application.properties** file of the project.

It is assumed that the input CSV file (obtained from running a query in Metabase) takes the following form:

```
Report ID
8e48a04b-a841-4a40-9800-38e7af10b089
5663ef27-71c8-46d0-9260-6aae162c6526
...
```

The steps for running the application are as follows:

1. Configure the desired values in the application.properties file.
2. Log into ReportStream to get token (update the value in **application.properties**).
3. Build the application.
4. Run the application and evaluate the results.
5. Repeat as necessary.

### Configure the Desired Values in the application.properties File
The following properties are used by **Failed Report Resender**:

**gov.cdc.prime.router.resender.skip.first.line** - Set this to **true** to skip the first line of the input file (usually the descriptive headers for the individual data columns).

**gov.cdc.prime.router.resender.baseUrl** - The URL associated with the ReportStream resend admin endpoint (e.g., http://localhost:7071/api/adm/resend for local testing).

**gov.cdc.prime.router.resender.token** - The Okta (OAuth) token used for authentication (ignored for local testing).

**gov.cdc.prime.router.resender.wait.time.in.seconds** - The maximum number of seconds to wait between each HTTP invocation (e.g., 5).

**gov.cdc.prime.router.resender.receive** - The full organization and receiver (e.g., **hhsprotect.mars-otc-elr**)

**gov.cdc.prime.router.resender.input.file** - The canonical path to the location of the input file (e.g., **/Users/bill/Downloads/query_result_2025-09-08T18_50_08.576037Z.csv**).


### Log into ReportStream to Get Token
Use the **prime** script to retireve the Okta (Oauth) token.

```./prime --env prod```

Look in **~/.prime/DH_PROD/accessToken** to get the value for the OAuth token.

```
% cat .prime/DH_PROD/accessToken.json                                              
{
  "token" : "<REDACTED>",
  "clientId" : "<REDACTED>",
  "expiresAt" : "2025-08-20T13:44:33.961924"
}
```

Copy the value for token and update `gov.cdc.prime.router.resender.token` in **application.properties**.

### Build The Application
Simpley execute `./gradlew clean build` in the project directory.

### Run the Application and Evaluate the Results
Execute `./gradlew bootRun` from the command line (or from within your IDE).  Output will be logged both to the console and to `failed-report-resender.log`.  A CSV file containing the results of the run will also be created.

### Repeat as Necessary
**Failed Report Resender** should be run with batches of ~500 report ids at a time so that it may complete comfortably withing the 1 hour token lifetime.
