# Portal Poster Reporter

## Purpose
Zenhub user story [17978 - Ability to resend multiple reports through ReportStream's report API](https://app.zenhub.com/workspaces/reportstream-om-67feaaa15ecf34000f4cc206/issues/gh/cdcgov/prime-reportstream/17978) articulates the need to have an approach to re-process a cohort of SimpleReport CSV files based on a separate CSV file of identifiers used as input.  This project addresses the part of the process that reports on the progress of the files after they have been posted to ReportStream.

## Usage
**Portal Poster Reporter** is a Spring Boot command line application.  It can be configured using any of the myriad of options outlined in Spring Boot's [Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) page.  The simplest (and suggested) method is to set the desired values in the **application.properties** file of the project.

It is assumed that the input CSV file (is the output from **Portal Poster**) takes the following form:

```
Report ID,Min of Created At: Minute,Min of Organization - Org → Organization Name,id,submission id
8e48a04b-a841-4a40-9800-38e7af10b089,"December 2, 2024, 9:54 PM",Children's Minnesota,0d385d94-a7e5-4539-bdc1-803f4ceb31bb,1014
5663ef27-71c8-46d0-9260-6aae162c6526,"December 2, 2024, 11:45 PM",Premiere medical center,eba23e56-dac6-4c6f-b6a0-6854314d3365,1015
...
```

The steps for running the application are as follows:

1. Configure the desired values in the application.properties file.
2. Compile the application.
3. Log into ReportStream to get token (update the value in **application.properties**).
4. Run the application and evaluate the results.
5. Repeat as necessary.

### Configure the Desired Values in the application.properties File
The following properties are used by **Portal Poster**:

**gov.cdc.prime.router.poster.skip.first.line** - Set this to **true** to skip the first line of the input file (usually the descriptive headers for the individual data columns).

**gov.cdc.prime.router.poster.url** - The full URL associated with the ReportStream submission history endpoint (e.g., http://localhost:7071/api/waters/report/%s/history/ for local testing).

**gov.cdc.prime.router.poster.organization** - The value to be used in the **Organization** HTTP header (e.g., simple_report).

**gov.cdc.prime.router.poster.token** - The Okta (OAuth) token used for authentication (ignored for local testing).

**gov.cdc.prime.router.poster.wait.time.in.seconds** - The number of seconds to wait between each HTTP invocation (e.g., 5).

**gov.cdc.prime.router.poster.input.file** - The canonical path to the location of the input file (e.g., **/Users/bill/projects/report-stream/user-stories/17978/portal-poster-reporter/20250609-123005-portal-poster-history.csv**).


### Compile The Application
Simpley execute `./gradlew clean build` in the project directory.

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

Copy the value for token and update `gov.cdc.prime.router.poster.token` in **application.properties**.

### Run the Application and Evaluate the Results
Execute `./gradlew bootRun` from the command line (or from within your IDE).  Output will be logged both to the console and to `portal-poster-reporter.log`.

### Repeat as Necessary
**Portal Poster Reporter** may be run as often as desired.
