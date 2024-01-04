# Connect Local Environment to appinsights

## Prerequisites
 - CDC account 
 - Azure Portal CDC Super User Access

## Steps
1. Download the latest [Application Insights JAR](https://github.com/microsoft/ApplicationInsights-Java/releases)
   1. When writing this document, the current version is `3.4.19`
2. Append the following argument to `JAVA_OPTS` in your `local.settings.json`
   1. ` -javaagent:\"/path/to/jar/applicationinsights-agent-3.4.19.jar\"`
   2. Update your path to point to your real JAR
   3. The escaped quotes are present because these JAVA_OPTS are stored in a json file. If you were running this off the command line then there would be no backslashes.
      1. `-javaagent:"/path/to/jar/applicationinsights-agent-3.4.19.jar"`
3. Create an environment variable with the connection string you can find in the Azure Portal
   1. Go to "pdhtest-appinsights" resource in the Azure Portal with your SU account
   2. See the “Connection String” value under the "overview" tab (you may have to open up the JSON view to see the whole string since it's rather large)
   3. `APPLICATIONINSIGHTS_CONNECTION_STRING=<Connection String>`
   4. You can set this environment variable however you’d like, but I personally updated the run configuration for the “quickRun” Gradle task in Intellij
4. Run the `quickRun` Gradle task ensuring that the new environment variable is being picked up.
   1. Make sure everything starts up normally 
   2. Make a few calls to an authenticated endpoint
   3. I suggested this one: http://localhost:7071/api/waters/report/1/history
   4. Wait about 10 minutes and ensure data is flowing into the appinsights portal. You should be able to see basic statistics around the requests.
    
## Extra Resources

- [Official Microsoft Documentation](https://learn.microsoft.com/en-us/azure/azure-monitor/app/opentelemetry-enable?tabs=java)
