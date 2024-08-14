#!/bin/bash
#
# This script uploads the api docs swagger ui to azure storage server blob container
# and the api docs swagger ui is hosted from the blob container
#
# It is meant to used on developer machines, but not in production.
#
# This script can be executed in the following context:
#
# 1. azure functions docker
# 2. on the native OS - mac or linux or windows (wsl)
#
# The effect of upload is idempotent, the content of the api docs swagger ui
# is brought up to the latest after 1 or more upload performed either from docker
# or host native
# 
# One edge case, the idempotent nature of the upload does not handle
# deletion of artifacts from swager ui container, we could handle that
# by always delete the apidocs container but that seems not a concern for now.
# 

set -e

# check az (azure cli) is installed
which az

if [ $? -eq 0 ]; then
  AzureWebJobsStorage="${AzureWebJobsStorage:-DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://localhost:10000/devstoreaccount1;QueueEndpoint=http://localhost:10001/devstoreaccount1;}"
  swagger_ui_dir_in_container="/prime-data-hub-router/swagger-ui"
  swagger_ui_dir_on_host="./build/swagger-ui"
  apidocs_container_name="apidocs"

  # check swagger ui dir location as a pretty good and cheap way to decide if it's 
  # in container or on host
  if [ -d $swagger_ui_dir_in_container ]; then
    # in docker
    az storage container create -n $apidocs_container_name --connection-string $AzureWebJobsStorage
    echo "apidocs creation: " $?
    az storage container set-permission -n $apidocs_container_name --public-access container --connection-string $AzureWebJobsStorage
    echo "apidocs public access: " $?
    az storage blob upload-batch -s $swagger_ui_dir_in_container -d $apidocs_container_name --overwrite --connection-string $AzureWebJobsStorage
    echo "swagger ui and api specs upload to $apidocs: " $?
  elif [ -d $swagger_ui_dir_on_host ]; then
    # on host (native)
    echo $AzureWebJobsStorage
    az storage container create -n $apidocs_container_name --connection-string "$AzureWebJobsStorage"
    echo "apidocs creation: " $?
    az storage container set-permission -n $apidocs_container_name --public-access container --connection-string "$AzureWebJobsStorage"
    echo "apidocs public access: " $?
    az storage blob upload-batch -s $swagger_ui_dir_on_host -d $apidocs_container_name --overwrite --connection-string "$AzureWebJobsStorage"
    echo "swagger ui and api specs upload to $apidocs: " $?
  else
    echo "swagger ui folder not found at expected location, apidocs swagger ui upload skipped..."
  fi
else
  echo "azure CLI required but seems not installed, please install azure CLI..."
fi

