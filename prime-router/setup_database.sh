#!/bin/bash
#
# Setup a dev database in a developer resource group
#

confirm() {
    # call with a prompt string or use a default
    read -r -p "${1} [y/n/e] " response
    case "$response" in
        [yY][eE][sS]|[yY])
            true
            ;;
				[nN][oO]|[nN])
						false
						;;
        *)
            exit 1
            ;;
    esac
}

# Check the PRIME_DEV_NAME
if [ -z "${PRIME_DEV_NAME}" ]; then
  echo "PRIME_DEV_NAME must be set. Example: export PRIME_DEV_NAME rhawes. Suggest sticking this in your shell profile"
  exit 1
fi

# Check the PRIME_POSTGRES_PASSWORD
if [ -z "${PRIME_POSTGRES_PASSWORD}" ]; then
  echo "PRIME_POSTGRES_PASSWORD must be set. Example: export PRIME_POSTGRES_PASSWORD superSecure2Password"
  exit 1
fi

if [[ $PRIME_DEV_NAME == prime-data-hub-* ]]
then
	server_name=${PRIME_DEV_NAME}
	resource_group=${PRIME_DEV_NAME}
else
	server_name="${PRIME_DEV_NAME}-prime-data-hub"
	resource_group="prime-dev-${PRIME_DEV_NAME}"
fi

if confirm "Create a database server ${server_name}. Will take 5 minutes"; then
	az postgres server create \
	--resource-group $resource_group \
	--name $server_name  \
	--auto-grow Disabled \
	--minimal-tls-version TLS1_2 \
	--location eastus \
	--admin-user prime \
	--admin-password "${PRIME_POSTGRES_PASSWORD}" \
	--sku-name B_Gen5_1 \
	--storage-size=5120 \
	--version 11
fi

if confirm "Create firewall rules to allow your Function App to access the new DB (will take 10 minutes)"; then
  addresses=$(az webapp show --resource-group $resource_group --name $server_name --query possibleOutboundIpAddresses --output tsv) 
	set -f 
  array=(${addresses//,/ })
  for ((i = 0; i < ${#array[@]}; i++));
	do
  	az postgres server firewall-rule create --resource-group $resource_group --server $server_name --name allow_func_ip$i --start-ip-address ${array[$i]} --end-ip-address ${array[$i]}
  done
fi

if confirm "Create a prime_data_hub database"; then
  az postgres db create -g "$resource_group" -s "$server_name" -n prime_data_hub
fi

if confirm "Update the environment variables of your prime_data_hub function app"; then
	az functionapp config appsettings set \
	-g $resource_group \
	-n $server_name \
	--settings "POSTGRES_PASSWORD=${PRIME_POSTGRES_PASSWORD}"
	az functionapp config appsettings set \
	-g $resource_group \
	-n $server_name \
	--settings "POSTGRES_USER=prime@${server_name}"
	az functionapp config appsettings set \
	-g $resource_group \
	-n $server_name \
	--settings "POSTGRES_URL=jdbc:postgresql://${server_name}.postgres.database.azure.com:5432/prime_data_hub?sslmode=require"
fi 


