# Connect Local Environment to appinsights

## Prerequisites

- CDC account
- Azure Portal CDC Super User Access

## Steps

1. Download the latest [Application Insights JAR](https://github.com/microsoft/ApplicationInsights-Java/releases)
    1. This jar enables additional features like Azure events and customDimensions to be sent to Application Insights
    2. When writing this document, the current version is `3.4.19`
2. Append the following argument to `JAVA_OPTS` in your `local.settings.json`
    1. `-javaagent:\"/path/to/jar/applicationinsights-agent-3.4.19.jar\"`
    2. Update your path to point to your real JAR
       1. For OSX use `realpath ./applicationinsights-agent-3.4.19.jar` to output the absolute path.
    3. The escaped quotes are present because these JAVA_OPTS are stored in a json file. If you were running this off
       the command line then there would be no backslashes.
        1. `-javaagent:"/path/to/jar/applicationinsights-agent-3.4.19.jar"`
3. Create an environment variable with the connection string you can find in the Azure Portal
    1. Go to "pdhtest-appinsights" resource in the Azure Portal with your SU account
    2. See the “Connection String” value under the "overview" tab (you may have to open up the JSON view to see the
       whole string since it's rather large)
    3. `APPLICATIONINSIGHTS_CONNECTION_STRING=<Connection String>`
    4. You can set this environment variable however you’d like, but I personally updated the run configuration for the
       “quickRun” Gradle task in Intellij
    5. Keep in mind this is what will start sending logs to Application Insights, NOT the .jar above. The .jar just
       enables additional features. So, make sure this connection string is unset if you do not want to send logs to
       Azure!
4. Run the `quickRun` Gradle task ensuring that the new environment variable is being picked up.
    1. Make sure everything starts up normally
       1. If java is unable to open the JAR see [below](#if-java-is-unable-to-open-the-jar).
    2. Make a few calls to an authenticated endpoint
    3. I suggested this one: http://localhost:7071/api/waters/report/1/history
    4. Wait about 10 minutes and ensure data is flowing into the appinsights portal. You should be able to see basic
       statistics around the requests.

## Troubleshooting

#### If only part of the logging appears to be working

- If some logs from your computer are flowing to pdhtest-appinsights, but are not the ReportStream defined custom events listed above, this means your connection string is configured properly but there is an issue with your jar.
- If none of the above defined custom events from your computer are ending up in pdhtest-appinsights, but you do see additional logging locally, then your connection string is incorrectly setup.
  - Example of what the logs will look like when the jar is working: 
    - `{"mdc":{"span_id":"bc4881697f596148","trace_flags":"01","trace_id":"7fc97db1ef3ec27336ee70472e7989b7"}`

#### If Java is unable to open the JAR

- Double-check your path in settings.local.json the path should be in escaped quotes "/path/to/file"
- Double-click the jar in finder, then go to System Settings -> Security and click "Open Anyway".
  - Alternative is to use the cli tool to remove the quarantine attr, something like this xattr -dr com.apple.quarantine <path to file>/my-binary-amd64
- Re-download the .jar file (you can attempt extracting it with the unzip cli command)

## Extra Resources

- [Official Microsoft Documentation](https://learn.microsoft.com/en-us/azure/azure-monitor/app/opentelemetry-enable?tabs=java)
