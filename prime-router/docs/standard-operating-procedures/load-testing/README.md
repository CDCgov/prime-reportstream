# Load Testing

## Load Testing scripts

All the load testing scripts need to have `{REPORTS_FUNCTION_KEY}` replaced with a function key for the reports 
endpoint.

- `simple-report-config.jmx`: this script sends reports with 1, 50 and 200 FHIR items; they are paced approximately so
that there are 4x 1 item reports as 50 and 4x 50 item reports as 200

## Setting up JMeter

[JMeter](https://jmeter.apache.org/) can be installed via download or through most package managers.  In order to work
with azure load testing, [JMeter plugins](https://jmeter-plugins.org/wiki/PluginsManager/) will also need to be 
installed.

### Required Plugins
- Custom Thread Groups