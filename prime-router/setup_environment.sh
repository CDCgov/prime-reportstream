set -euo pipefail

# Get developer name
read -r -p "Enter a deverloper name (eg. cglodosky, test, prod): " prime_dev_name

# Check that a name was entered
if [ -z $prime_dev_name ]
then
    echo "A developer name was not entered. Exiting."
    exit 1
fi

# Set resource group
if [[ "$prime_dev_name" =~ (test|prod) ]]
then
    resource_group="prime-data-hub-$prime_dev_name"
else
    resource_group="prime-dev-$prime_dev_name"
fi

# Set variables
PREFIX="pdh"
vnet_name=$PREFIX"-vnet"
nsg_bastion=$PREFIX"-nsg.bastion"
nsg_public=$PREFIX"-nsg.public"
nsg_private=$PREFIX"-nsg.private"
bastion_ip=$PREFIX"-ip.bastion"
bastion=$PREFIX"-bastion"
storage_account=$PREFIX"storageaccount"
container_registry=$PREFIX"containerregistry"
elastic_plan=$PREFIX"-serviceplan"
function_app=$PREFIX"-functionapp"
function_image=$container_registry.azurecr.io/$PREFIX
front_door=$PREFIX"-frontdoor"
front_door_frontend=prime-data-hub-$prime_dev_name

# Check that resource group exists
if [ ! `az group exists --resource-group "$resource_group"` ]
then
    echo "$resource_group is not an existing resource group."
else
    echo "Building the environment within the $resource_group resource group..."
fi

# Create network security groups
# echo "--- Creating network security groups..."
# az network nsg create --resource-group $resource_group \
#                       --name $nsg_bastion \
#                       --output none \
#                       --only-show-errors

# az network nsg create --resource-group $resource_group \
#                       --name $nsg_public \
#                       --output none \
#                       --only-show-errors

# az network nsg create --resource-group $resource_group \
#                       --name $nsg_private \
#                       --output none \
#                       --only-show-errors

# echo -e "\tApplying security group rules..."
# az network nsg rule create --resource-group $resource_group \
#                            --nsg-name $nsg_bastion \
#                            --name AllowHttpsInbound \
#                            --priority 100 \
#                            --access Allow \
#                            --direction Inbound \
#                            --protocol TCP \
#                            --source-address-prefixes Internet \
#                            --destination-port-ranges 443 \
#                            --output none \
#                            --only-show-errors

# az network nsg rule create --resource-group $resource_group \
#                            --nsg-name $nsg_bastion \
#                            --name AllowGatewayManagerInbound \
#                            --priority 110 \
#                            --access Allow \
#                            --direction Inbound \
#                            --protocol TCP \
#                            --source-address-prefixes GatewayManager \
#                            --destination-port-ranges 443 \
#                            --output none \
#                            --only-show-errors

# az network nsg rule create --resource-group $resource_group \
#                            --nsg-name $nsg_bastion \
#                            --name AllowAzureLoadBalancerInbound \
#                            --priority 120 \
#                            --access Allow \
#                            --direction Inbound \
#                            --protocol TCP \
#                            --source-address-prefixes AzureLoadBalancer \
#                            --destination-port-ranges 443 \
#                            --output none \
#                            --only-show-errors

# az network nsg rule create --resource-group $resource_group \
#                            --nsg-name $nsg_bastion \
#                            --name AllowSshRdpOutbound \
#                            --priority 130 \
#                            --access Allow \
#                            --direction Outbound \
#                            --protocol "*" \
#                            --destination-address-prefixes VirtualNetwork \
#                            --destination-port-ranges 22 3389 \
#                            --output none \
#                            --only-show-errors

# az network nsg rule create --resource-group $resource_group \
#                            --nsg-name $nsg_bastion \
#                            --name AllowAzureCloudOutbound \
#                            --priority 140 \
#                            --access Allow \
#                            --direction Outbound \
#                            --protocol TCP \
#                            --destination-address-prefixes AzureCloud \
#                            --destination-port-ranges 443 \
#                            --output none \
#                            --only-show-errors

# # Create virtual network
# echo "--- Creating virtual network..."
# az network vnet create --resource-group $resource_group \
#                        --name $vnet_name \
#                        --address-prefixes 10.0.0.0/16 \
#                        --subnet-name AzureBastionSubnet \
#                        --subnet-prefix 10.0.0.0/27 \
#                        --network-security-group $nsg_bastion \
#                        --output none \
#                        --only-show-errors

# echo -e "\tCreating subnets..."
# az network vnet subnet create --resource-group $resource_group \
#                               --vnet-name $vnet_name \
#                               --name public \
#                               --address-prefix 10.0.1.0/27 \
#                               --network-security-group $nsg_public \
#                               --service-endpoints Microsoft.ContainerRegistry \
#                               --output none \
#                               --only-show-errors

# az network vnet subnet create --resource-group $resource_group \
#                               --vnet-name $vnet_name \
#                               --name private \
#                               --address-prefix 10.0.2.0/24 \
#                               --network-security-group $nsg_private \
#                               --service-endpoints Microsoft.Storage \
#                               --output none \
#                               --only-show-errors

# # Create public IP address for bastion host
# echo "--- Creating public IP for bastion host..."
# az network public-ip create --resource-group $resource_group \
#                             --name $bastion_ip \
#                             --sku Standard \
#                             --output none \
#                             --only-show-errors

# # Create bastion host
# echo "--- Creating bastion host..."
# az network bastion create --resource-group $resource_group \
#                           --vnet-name $vnet_name \
#                           --name $bastion \
#                           --public-ip-address $bastion_ip \
#                           --output none \
#                           --only-show-errors

# # Create storage account
# echo "--- Creating storage account..."
# az storage account create --resource-group $resource_group \
#                           --name $storage_account \
#                           --default-action Deny \
#                           --sku Standard_LRS \
#                           --output none \
#                           --only-show-errors

# echo -e "\tApplying network rules..."
# az storage account network-rule add --resource-group $resource_group \
#                                     --account-name $storage_account \
#                                     --action Allow \
#                                     --vnet-name $vnet_name \
#                                     --subnet private \
#                                     --output none \
#                                     --only-show-errors

# # Create container registry
echo "--- Creating container registry..."
az acr create --resource-group $resource_group \
              --name $container_registry \
              --sku Basic \
              --admin-enabled true \
              --default-action Deny \
              --public-network-enabled false \
              --sku Premium \
              --output none \
              --only-show-errors

echo -e "\tApplying network rules..."
az acr network-rule add --resource-group $resource_group \
                        --name $container_registry \
                        --vnet-name $vnet_name \
                        --subnet public \
                        --output none \
                        --only-show-errors

# echo "--- Creating elastic plan for functions app..."
# if [[ $prime_dev_name == prod ]]
# then
#   az functionapp plan create --resource-group $resource_group \
#                              --name $elastic_plan \
#                              --is-linux \
#                              --min-instances 1 \
#                              --max-burst 10 \
#                              --sku EP1 \
#                              --output none \
#                              --only-show-errors
# else
#   az functionapp plan create --resource-group $resource_group \
#                              --name $elastic_plan \
#                              --is-linux \
#                              --number-of-workers 1 \
#                              --sku B1 \
#                              --output none \
#                              --only-show-errors
# fi

# echo "--- Creating function app..."
# az functionapp create --resource-group $resource_group \
#                       --name $function_app \
#                       --functions-version 3 \
#                       --storage-account $storage_account \
#                       --plan $elastic_plan \
#                       --runtime java \
#                       --runtime-version 11 \
#                       --deployment-container-image-name $function_image:latest \
#                       --output none \
#                       --only-show-errors

# echo -e "\tApplying network rules..."
# az functionapp vnet-integration add --resource-group $resource_group \
#                                     --name $function_app \
#                                     --vnet $vnet_name \
#                                     --subnet private \
#                                     --output none \
#                                     --only-show-errors

# echo "--- Creating front door..."
# az network front-door create --resource-group $resource_group \
#                              --name $front_door \
#                              --backend-address $function_app.azurewebsites.net \
#                              --accepted-protocols Https 
