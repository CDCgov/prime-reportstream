#!/bin/bash
#
# This script provisions apidocs swagger ui to the azure storage server
# in the Azure functions context, this task is to be run before start_func.sh.
# It is meant to used on developer machines, but not in production.
#
# It handles three different configurations in this order of preference
#  1. The dev container case where '/prime-data-hub-router/azure-functions/prime-data-hub-router' contains the function
#  2. The local case where 'build/azure-functions/prime-data-hub-router'1' contains the function
#  2. The local case where 'azure-functions/prime-data-hub-router' contains the function
#

set -e
swagger_ui_dir="/prime-data-hub-router/swagger-ui"
apidocs_container_name="apidocs"
# find the swagger ui resources base folder
if [ -d $swagger_ui_dir ]; then
  function_folder=/prime-data-hub-router/$base_name
else
  echo "API docs swagger ui resources not found at: $swagger_ui_dir"
  exit 1
fi

az storage container create -n $apidocs_container_name --connection-string $AzureWebJobsStorage
echo "apidocs creation: " $?
az storage container set-permission -n $apidocs_container_name --public-access container --connection-string $AzureWebJobsStorage
echo "apidocs public access: " $?
az storage blob upload-batch -s $swagger_ui_dir -d $apidocs_container_name --overwrite --connection-string $AzureWebJobsStorage
echo "swagger ui and api specs upload to $apidocs: " $?
