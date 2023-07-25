#!/bin/bash
#
# This script runs the Azure function of the project with a debug port open to allow debugger to attach.
# It is meant to used on developer machines, but not in production.
#
# It handles three different configurations in this order of preference
#  1. The dev container case where '/prime-data-hub-router/azure-functions/prime-data-hub-router' contains the function
#  2. The local case where 'build/azure-functions/prime-data-hub-router'1' contains the function
#  3. The local case where 'azure-functions/prime-data-hub-router' contains the function
#

base_name=azure-functions/prime-data-hub-router

# find the function folder
if [ -d /prime-data-hub-router/$base_name ]; then
  function_folder=/prime-data-hub-router/$base_name
elif [ -d build/$base_name ]; then
  function_folder=target/$base_name
elif [ -d $base_name ]; then
  function_folder=$base_name
fi

cd $function_folder

# Load the configuration to the database.  Note the CLI will wait for the API to be available.
function load_config() {
  # Use the prime CLI to load configuration in the background
  # The fatjar is two levels up in the libs folder
  top_dir=$function_folder/../..
  fatjar=$top_dir/libs/prime-router-0.2-SNAPSHOT-all.jar
  echo "Loading lookup tables..."
  java -jar $fatjar lookuptables loadall -d $function_folder/metadata/tables/local -r 60 --check-last-modified
  # Note the settings require the full metadata catalog to be in place, so run last
  echo "Loading organization settings..."
  java -jar $fatjar multiple-settings set -s -i $function_folder/settings/organizations.yml -r 60 --check-last-modified
  echo "Done loading local configurations."
}

# Load the configuration in the background if not running in GitHub Actions.
if [ ! -z "$GITHUB_ACTIONS" ]
then
   load_config | awk -v date="$(date +[%FT%TZ])" '{print date " [LOAD CONFIG] " $0}' &
else
   echo "Running in GitHub Actions.  Skipping load of configuration."
fi

# Run the functions
func host start --cors http://localhost:10000,http://127.0.0.1:10000,http://localhost:8090,http://localhost:3000 --language-worker -- "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"