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

# Use this variable to point to a different host on which your 'local' API runs
# This can be useful if you are running the end-to-end test in a container
# as opposed to on your actual localhost (e.g. the builder container)
# Default Value (i.e. if unspecified): localhost
PRIME_RS_API_ENDPOINT_HOST=${PRIME_RS_API_ENDPOINT_HOST:-localhost}

echo; echo Generating example curl to send a file to the hub.
echo Using Resource Group: $resource_group, Azure Function App Name  $full_app_name

# Get the default host name for the cloud function; parse from the json
hostname=$(az functionapp show -g $resource_group -n $full_app_name | python <( echo '
import sys, json
print json.loads(sys.stdin.read())["defaultHostName"]
' ))

# Get the secret access key for the cloud function
default_function_key=$(az functionapp function keys list -g $resource_group -n $full_app_name --function-name reports | python <( echo '
import sys, json
print json.loads(sys.stdin.read())["default"]
' ))

printf "Use this access key parameter in any URL that needs to send a file to the hub\n"
printf "       code=$default_function_key\n"

# Generate a giant ugly curl call
boilerplate_glop="curl -X POST -H \"client:simple_report\" -H \"Content-Type: text/csv\" "
localfile_glop="--data-binary \"@./src/test/csv_test_files/input/simplereport.csv\""
cloud_access_key=" -H \"x-functions-key:$default_function_key\""
cloud_url="\"https://$hostname/api/reports\""
local_url="\"http://${PRIME_RS_API_ENDPOINT_HOST?}:7071/api/reports\""

# Now put it all together:
printf "\nRun this to submit a test report to your cloud:\n"
printf "     $boilerplate_glop $cloud_access_key $localfile_glop $cloud_url\n"

printf "\nRun this to submit a test report locally:\n"
printf "     $boilerplate_glop $localfile_glop $local_url\n"

printf "\nTo run the prime cli locally:\n"
printf "     ./prime data --input-schema primedatainput/pdi-covid-19 --input ./src/test/csv_test_files/input/simplereport.csv --route --output-dir . \n"
