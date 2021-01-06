set -euo pipefail

# Get developer name
read -r -p "Enter a deverloper name (eg. cglodosky, test, prod): " prime_dev_name

# Get PostgreSQL admin password
read -s -p "Enter an admin password for the PostgreSQL database: " pg_admin_pwd
echo

# Check that a name was entered
if [ -z $prime_dev_name ]
then
    echo "A developer name was not entered. Exiting."
    exit 1
elif [ -z $pg_admin_pwd ]
then
    echo "A PostgreSQL admin password was not entered. Exiting."
    exit 1
fi

# Set resource group and resource prefix
if [[ "$prime_dev_name" =~ (test|prod) ]]
then
    resource_group="prime-data-hub-$prime_dev_name"
    PREFIX="pdh"$prime_dev_name
else
    resource_group="prime-dev-$prime_dev_name"
    PREFIX="pdh"
fi

# Set variables
vnet_name=$PREFIX"-vnet"
nsg_public=$PREFIX"-nsg.public"
nsg_private=$PREFIX"-nsg.private"
storage_account=$PREFIX"storageaccount"
container_registry=$PREFIX"containerregistry"
elastic_plan=$PREFIX"-serviceplan"
function_app=$PREFIX"-functionapp"
function_image=$container_registry.azurecr.io/$PREFIX
pg_server_name=$PREFIX"-pgsql"
sftp_server_name=$PREFIX"-sftpserver"
front_door=prime-data-hub-$prime_dev_name

# Check that resource group exists
if [ ! `az group exists --resource-group "$resource_group"` ]
then
    echo "$resource_group is not an existing resource group."
else
    echo "Building the environment within the $resource_group resource group..."
fi

# Create network security groups
echo "--- Creating network security groups..."
az network nsg create --resource-group $resource_group \
                      --name $nsg_public \
                      --output none \
                      --only-show-errors

az network nsg create --resource-group $resource_group \
                      --name $nsg_private \
                      --output none \
                      --only-show-errors

# Create virtual network
echo "--- Creating virtual network..."
az network vnet create --resource-group $resource_group \
                       --name $vnet_name \
                       --network-security-group $nsg_public \
                       --address-prefixes 10.0.0.0/16 \
                       --subnet-name public \
                       --subnet-prefix 10.0.1.0/24 \
                       --network-security-group $nsg_public \
                       --output none \
                       --only-show-errors

echo -e "\t- Creating subnets..."
az network vnet subnet update --resource-group $resource_group \
                              --vnet-name $vnet_name \
                              --name public \
                              --service-endpoints Microsoft.ContainerRegistry Microsoft.Storage Microsoft.SQL \
                              --output none \
                              --only-show-errors

az network vnet subnet create --resource-group $resource_group \
                              --vnet-name $vnet_name \
                              --name container \
                              --address-prefix 10.0.2.0/24 \
                              --network-security-group $nsg_public \
                              --delegations Microsoft.ContainerInstance/containerGroups \
                              --service-endpoints Microsoft.Storage \
                              --output none \
                              --only-show-errors

az network vnet subnet create --resource-group $resource_group \
                              --vnet-name $vnet_name \
                              --name private \
                              --address-prefix 10.0.3.0/24 \
                              --network-security-group $nsg_private \
                              --service-endpoints Microsoft.Storage Microsoft.SQL \
                              --output none \
                              --only-show-errors

# Create storage account
echo "--- Creating storage account..."
az storage account create --resource-group $resource_group \
                          --name $storage_account \
                          --default-action Deny \
                          --sku Standard_LRS \
                          --output none \
                          --only-show-errors

echo -e "\t- Applying network rules..."
az storage account network-rule add --resource-group $resource_group \
                                    --account-name $storage_account \
                                    --action Allow \
                                    --vnet-name $vnet_name \
                                    --subnet public \
                                    --output none \
                                    --only-show-errors

az storage account network-rule add --resource-group $resource_group \
                                    --account-name $storage_account \
                                    --action Allow \
                                    --vnet-name $vnet_name \
                                    --subnet container \
                                    --output none \
                                    --only-show-errors

az storage account network-rule add --resource-group $resource_group \
                                    --account-name $storage_account \
                                    --action Allow \
                                    --vnet-name $vnet_name \
                                    --subnet private \
                                    --output none \
                                    --only-show-errors

# Create container registry
echo "--- Creating container registry..."
az acr create --resource-group $resource_group \
              --name $container_registry \
              --admin-enabled true \
              --sku Premium \
              --output none \
              --only-show-errors

echo -e "\t- Applying network rules..."
az acr network-rule add --resource-group $resource_group \
                        --name $container_registry \
                        --vnet-name $vnet_name \
                        --subnet public \
                        --output none \
                        --only-show-errors

echo "--- Creating elastic plan for functions app..."
if [[ $prime_dev_name == prod ]]
then
  az functionapp plan create --resource-group $resource_group \
                             --name $elastic_plan \
                             --is-linux \
                             --min-instances 1 \
                             --max-burst 10 \
                             --sku EP1 \
                             --output none \
                             --only-show-errors
else
  az functionapp plan create --resource-group $resource_group \
                             --name $elastic_plan \
                             --is-linux \
                             --number-of-workers 1 \
                             --sku B1 \
                             --output none \
                             --only-show-errors
fi

echo "--- Creating functions app..."
az functionapp create --resource-group $resource_group \
                      --name $function_app \
                      --functions-version 3 \
                      --storage-account $storage_account \
                      --plan $elastic_plan \
                      --runtime java \
                      --runtime-version 11 \
                      --deployment-container-image-name $function_image:latest \
                      --output none \
                      --only-show-errors

echo -e "\t- Applying network rules..."
az functionapp vnet-integration add --resource-group $resource_group \
                                    --name $function_app \
                                    --vnet $vnet_name \
                                    --subnet public \
                                    --output none \
                                    --only-show-errors

echo -e "\t- Enabling filesystem logging..."
az webapp log config --resource-group $resource_group \
                     --name $function_app \
                     --web-server-logging filesystem \
                     --output none \
                     --only-show-errors

echo "--- Creating PostgreSQL server..."
if [[ $prime_dev_name == prod ]]
then
    az postgres server create --resource-group $resource_group \
                              --name $pg_server_name \
                              --auto-grow Enabled \
                              --minimal-tls-version TLS1_2 \
                              --public-network-access Disabled \
                              --admin-user prime \
                              --admin-password $pg_admin_pwd \
                              --sku-name GP_Gen5_4 \
                              --storage-size=5120 \
                              --version 11 \
                              --output none \
                              --only-show-errors
else
    az postgres server create --resource-group $resource_group \
                              --name $pg_server_name \
                              --auto-grow Disabled \
                              --minimal-tls-version TLS1_2 \
                              --admin-user prime \
                              --admin-password $pg_admin_pwd \
                              --sku-name GP_Gen5_4 \
                              --storage-size=5120 \
                              --version 11 \
                              --output none \
                              --only-show-errors
fi

echo -e "\t- Applying network rules..."
az postgres server vnet-rule create --resource-group $resource_group \
                                    --name AllowPublicSubnet \
                                    --server-name $pg_server_name \
                                    --vnet-name $vnet_name \
                                    --subnet public \
                                    --output none \
                                    --only-show-errors

az postgres server vnet-rule create --resource-group $resource_group \
                                    --name AllowPrivateSubnet \
                                    --server-name $pg_server_name \
                                    --vnet-name $vnet_name \
                                    --subnet private \
                                    --output none \
                                    --only-show-errors

echo -e "\t- Creating database..."
az postgres db create --resource-group $resource_group \
                      --server-name $pg_server_name \
                      --name prime_data_hub \
                      --output none \
                      --only-show-errors

echo -e "\t- Setting environment variables..."
az functionapp config appsettings set --resource-group $resource_group \
                                      --name $function_app \
                                      --settings "POSTGRES_USER=prime@$pg_server_name" \
                                                 "POSTGRES_PASSWORD=$pg_admin_pwd" \
                                                 "POSTGRES_URL=jdbc:postgresql://${pg_server_name}.postgres.database.azure.com:5432/prime_data_hub?sslmode=require" \
                                      --output none \
                                      --only-show-errors

echo "--- Creating file share for SFTP transfer..."
storage_key=$(az storage account keys list --account-name $storage_account \
                                           --output tsv \
                                           --query [0].value)

az storage share-rm create --resource-group $resource_group \
                           --name $sftp_server_name \
                           --storage-account $storage_account \
                           --output none \
                           --only-show-errors

echo "--- Creating SFTP server container..."
az container create --resource-group $resource_group \
                    --name $sftp_server_name \
                    --image atmoz/sftp:latest \
                    --vnet $vnet_name \
                    --subnet container \
                    --ports 22 \
                    --environment-variables SFTP_USERS=foo:pass:::upload \
                    --azure-file-volume-share-name $sftp_server_name \
                    --azure-file-volume-account-name $storage_account \
                    --azure-file-volume-account-key $storage_key \
                    --azure-file-volume-mount-path /home/foo/upload \
                    --output none \
                    --only-show-errors

echo "--- Creating front door..."
az network front-door create --resource-group $resource_group \
                             --name $front_door \
                             --backend-address $function_app.azurewebsites.net \
                             --accepted-protocols Https \
                             --output none \
                             --only-show-errors

echo -e "\t- Applying access restrictions..."
# IMPORTANT - The above access restriction will prevent open access
# to the function app.  However, an access restriction must be manually
# created to allow traffic via FrontDoor.Backend.  This cannot currently
# be done via CLI as it is in preview.

az functionapp config access-restriction set --resource-group $resource_group \
                                             --name $function_app \
                                             --use-same-restrictions-for-scm-site true \
                                             --output none \
                                             --only-show-errors

az functionapp config access-restriction add --resource-group $resource_group \
                                             --name $function_app \
                                             --rule-name AllowVNetTraffic \
                                             --priority 100 \
                                             --action Allow \
                                             --vnet-name $vnet_name \
                                             --subnet public \
                                             --output none \
                                             --only-show-errors

# IMPORTANT - NEED TO SET SFTP CONTAINER ENVIRONMENT VARIABLES!!
# PRIME_ENVIRONMENT=
# AZ_PHD__ELR_PROD__USER=**** 
# AZ_PHD__ELR_PROD__PASS=**** 
# AZ_PHD__ELR_TEST__USER=****
# AZ_PHD__ELR_TEST__PASS=****
