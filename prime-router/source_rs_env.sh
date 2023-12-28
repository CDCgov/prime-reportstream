#!/bin/bash

# Source ENV secrets from Azure Key Vault for the target environment
#
# NOTE:
#
# It is required to az login to access the key vault
#

echo_msg () {
    echo "$(basename $0): $*"
}

# main
set -e -u -o pipefail

echo_msg " - Sourcing RS secrets from: Azure Key Vault"
echo_msg

# fetch parameters into ENV vars
#RS_ENV_DB_USER = "prime"
#RS_ENV_DB_PASSWORD = "changeIT!"
#RS_ENV_DB_URL = "jdbc:postgresql://localhost:5432/prime_data_hub"
#RS_PRIME_API_BASE_URL = "http://localhost:7071/api"
#RS_ENV_OAUTH_BASE_URL = "reportstream.oktapreview.com"
#RS_ENV_OAUTH_CLIENT_ID = "0oa8uvan2i07YXJLk1d7"
#RS_ENV_OAUTH_REDIRECT = "http://localhost:7071/api/download"
#RS_ENV_SFTP_HOST = "localhost"
#RS_ENV_SFTP_PORT = "22"
#RS_ENV_SFTP_USER = "foo"
#RS_ENV_SFTP_PASSWORD = "pass"
#RS_ENV_BLOB_STORAGE_CONN_STR = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=<find online>;BlobEndpoint=http://localhost:10000/devstoreaccount1;QueueEndpoint=http://localhost:10001/devstoreaccount1;"
#RS_ENV_PARTNER_BLOB_STORAGE_CONN_STR = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=<find online>;BlobEndpoint=http://localhost:10000/devstoreaccount1;QueueEndpoint=http://localhost:10001/devstoreaccount1;"

# shellcheck disable=SC2155
export RS_ENV_DB_USER=$(az keyvault secret show --name "RS-ENV-DB-USER" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_DB_USER" "${RS_ENV_DB_USER}"
# shellcheck disable=SC2155
export RS_ENV_DB_PASSWORD=$(az keyvault secret show --name "RS-ENV-DB-PASSWORD" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_DB_PASSWORD" "$RS_ENV_DB_PASSWORD"
# shellcheck disable=SC2155
export RS_ENV_DB_URL=$(az keyvault secret show --name "RS-ENV-DB-URL" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_DB_URL" "$RS_ENV_DB_URL"
# shellcheck disable=SC2155
export RS_PRIME_API_BASE_URL=$(az keyvault secret show --name "RS-PRIME-API-BASE-URL" --vault-name "RG1KV001" --query "value")
echo "RS_PRIME_API_BASE_URL" "$RS_PRIME_API_BASE_URL"
# shellcheck disable=SC2155
export RS_ENV_OAUTH_BASE_URL=$(az keyvault secret show --name "RS-ENV-OAUTH-BASE-URL" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_OAUTH_BASE_URL" "$RS_ENV_OAUTH_BASE_URL"
# shellcheck disable=SC2155
export RS_ENV_OAUTH_CLIENT_ID=$(az keyvault secret show --name "RS-ENV-OAUTH-CLIENT-ID" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_OAUTH_CLIENT_ID" "$RS_ENV_OAUTH_CLIENT_ID"
# shellcheck disable=SC2155
export RS_ENV_OAUTH_REDIRECT=$(az keyvault secret show --name "RS-ENV-OAUTH-REDIRECT" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_OAUTH_REDIRECT" "$RS_ENV_OAUTH_REDIRECT"
# shellcheck disable=SC2155
export RS_ENV_SFTP_HOST=$(az keyvault secret show --name "RS-ENV-SFTP-HOST" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_SFTP_HOST" "$RS_ENV_SFTP_HOST"
# shellcheck disable=SC2155
export RS_ENV_SFTP_PORT=$(az keyvault secret show --name "RS-ENV-SFTP-PORT" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_SFTP_PORT" "$RS_ENV_SFTP_PORT"
# shellcheck disable=SC2155
export RS_ENV_SFTP_USER=$(az keyvault secret show --name "RS-ENV-SFTP-USER" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_SFTP_USER" "$RS_ENV_SFTP_USER"
# shellcheck disable=SC2155
export RS_ENV_SFTP_PASSWORD=$(az keyvault secret show --name "RS-ENV-SFTP-PASSWORD" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_SFTP_PASSWORD" "$RS_ENV_SFTP_PASSWORD"
# shellcheck disable=SC2155
export RS_ENV_BLOB_STORAGE_CONN_STR=$(az keyvault secret show --name "RS-ENV-BLOB-STORAGE-CONN-STR" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_BLOB_STORAGE_CONN_STR" "$RS_ENV_BLOB_STORAGE_CONN_STR"
# shellcheck disable=SC2155
export RS_ENV_PARTNER_BLOB_STORAGE_CONN_STR=$(az keyvault secret show --name "RS-ENV-PARTNER-BLOB-STORAGE-CONN-STR" --vault-name "RG1KV001" --query "value")
echo "RS_ENV_PARTNER_BLOB_STORAGE_CONN_STR" "$RS_ENV_PARTNER_BLOB_STORAGE_CONN_STR"
export AzureWebJobsStorage=${RS_ENV_BLOB_STORAGE_CONN_STR}
echo "AzureWebJobsStorage" "${AzureWebJobsStorage}"
export PartnerStorage=${RS_ENV_PARTNER_BLOB_STORAGE_CONN_STR}
echo "PartnerStorage" "${PartnerStorage}"
export AzureBlobDownloadRetryCount=5
echo "AzureBlobDownloadRetryCount" ${AzureBlobDownloadRetryCount}
export POSTGRES_URL=${RS_ENV_DB_URL}
echo "POSTGRES_URL" "${POSTGRES_URL}"
export POSTGRES_DB="prime_data_hub"
echo "POSTGRES_DB" ${POSTGRES_DB}
export POSTGRES_PASSWORD=${RS_ENV_DB_PASSWORD}
echo "POSTGRES_PASSWORD" "${POSTGRES_PASSWORD}"
export POSTGRES_USER=${RS_ENV_DB_USER}
echo "POSTGRES_USER" "${POSTGRES_USER}"
export PRIME_ENVIRONMENT="local"
echo "PRIME_ENVIRONMENT" ${PRIME_ENVIRONMENT}
export RS_OKTA_baseUrl=${RS_ENV_OAUTH_BASE_URL}
echo "RS_OKTA_baseUrl" "${RS_OKTA_baseUrl}"
export RS_OKTA_clientId=${RS_ENV_OAUTH_CLIENT_ID}
echo "RS_OKTA_clientId" "${RS_OKTA_clientId}"
export RS_OKTA_redirect=${RS_ENV_OAUTH_REDIRECT}
echo "RS_OKTA_redirect" "${RS_OKTA_redirect}"
export VAULT_API_ADDR="http://vault:8200"
echo "VAULT_API_ADDR" ${VAULT_API_ADDR}
export FHIR_ENGINE_TEST_PIPELINE=enabled
echo "FHIR_ENGINE_TEST_PIPELINE" ${FHIR_ENGINE_TEST_PIPELINE}
export DB_PASSWORD=${RS_ENV_DB_PASSWORD}
echo "DB_PASSWORD" "${DB_PASSWORD}"
export DB_URL=${RS_ENV_DB_URL}
echo "DB_URL" "${DB_URL}"
export DB_USER=${RS_ENV_DB_USER}
echo "DB_USER" "$DB_USER"
export FUNCTIONS_CORE_TOOLS_TELEMETRY_OPTOUT=1
echo "FUNCTIONS_CORE_TOOLS_TELEMETRY_OPTOUT" ${FUNCTIONS_CORE_TOOLS_TELEMETRY_OPTOUT}
export PRIME_RS_API_ENDPOINT_HOST="localhost"
echo "PRIME_RS_API_ENDPOINT_HOST" ${PRIME_RS_API_ENDPOINT_HOST}
