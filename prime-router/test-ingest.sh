#!/usr/bin/env bash

# Despite the name, at the moment this doesn't actually test anything - it just creates some
# curl commands you can run.
#
# This assumes you setup a Hub Router environment using setup_resource_group.sh

# Using the same variable names as in Rick's script, in case we want to merge them.
app_name=prime-data-hub
resource_group=prime-dev-${PRIME_DEV_NAME}
storage_account=${PRIME_DEV_NAME}primedev
full_app_name="${PRIME_DEV_NAME}"-"$app_name"

echo; echo Generating example curl to send a file to the hub.
echo Using Resource Group: $resource_group, Azure Function App Name  $full_app_name

# Get the default host name for the cloud function; parse from the json
hostname=$(az functionapp show -g $resource_group -n $full_app_name | python <( echo '
import sys, json
print json.loads(sys.stdin.read())["defaultHostName"]
' ))

# Get the secret access key for the cloud function
default_function_key=$(az functionapp function keys list -g $resource_group -n $full_app_name --function-name report | python <( echo '
import sys, json
print json.loads(sys.stdin.read())["default"]
' ))

printf "Use this access key parameter in any URL that needs to send a file to the hub\n"
printf "       code=$default_function_key\n"

# Generate a giant ugly curl call
boilerplate_glop="curl -X POST -H \"Content-Type: text/csv\""
localfile_glop="--data-binary \"@./src/test/unit_test_files/lab1-test_results-17-42-31.csv\""
cloud_url="\"https://$hostname/api/report?code=$default_function_key&schema=pdi-covid-19.schema&filename=lab1-test_results-17-42-31.csv\""
local_url="\"http:/localhost:7071/api/report?schema=pdi-covid-19.schema&filename=lab1-test_results-17-42-31.csv\""

# Now put it all together:
printf "\nRun this to submit a test report to your cloud:\n"
printf "     $boilerplate_glop $localfile_glop $cloud_url\n"

printf "\nRun this to submit a test report locally:\n"
printf "     $boilerplate_glop $localfile_glop $local_url\n"







